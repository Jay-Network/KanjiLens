package com.jworks.kanjilens.domain.usecases

import android.util.Log
import com.jworks.kanjilens.data.nlp.KuromojiTokenizer
import com.jworks.kanjilens.domain.models.DetectedText
import com.jworks.kanjilens.domain.models.JapaneseTextUtil
import com.jworks.kanjilens.domain.models.JapaneseToken
import com.jworks.kanjilens.domain.models.KanjiSegment
import com.jworks.kanjilens.domain.models.TextElement
import com.jworks.kanjilens.domain.repository.FuriganaRepository
import javax.inject.Inject

class EnrichWithFuriganaUseCase @Inject constructor(
    private val furiganaRepository: FuriganaRepository,
    private val kuromojiTokenizer: KuromojiTokenizer
) {
    companion object {
        private const val TAG = "FuriganaEnrich"
        private const val MAX_WORD_LEN = 6

        // Counter suffix readings when preceded by a digit (number + kanji compound)
        private val COUNTER_READINGS = mapOf(
            "月" to "がつ", "日" to "にち", "年" to "ねん",
            "時" to "じ", "分" to "ふん", "秒" to "びょう",
            "回" to "かい", "階" to "かい", "歳" to "さい",
            "才" to "さい", "円" to "えん", "個" to "こ",
            "本" to "ほん", "冊" to "さつ", "枚" to "まい",
            "台" to "だい", "匹" to "ひき", "頭" to "とう",
            "羽" to "わ", "杯" to "はい", "件" to "けん",
            "号" to "ごう", "巻" to "かん", "週" to "しゅう",
        )

        private fun isDigitChar(ch: Char): Boolean =
            ch in '0'..'9' || ch in '０'..'９'
    }

    suspend fun execute(detectedTexts: List<DetectedText>): List<DetectedText> {
        val kanjiLines = detectedTexts.filter { it.containsKanji }
        if (kanjiLines.isEmpty()) return detectedTexts

        // Use Kuromoji if available (context-aware), fall back to JMDict (greedy match)
        return if (kuromojiTokenizer.isReady()) {
            enrichWithKuromoji(detectedTexts)
        } else {
            enrichWithJMDict(detectedTexts, kanjiLines)
        }
    }

    /**
     * Kuromoji-based enrichment: tokenize the full line for context-aware readings.
     * Maps tokens back to element positions for per-segment rendering.
     */
    private fun enrichWithKuromoji(detectedTexts: List<DetectedText>): List<DetectedText> {
        return detectedTexts.map { detected ->
            if (!detected.containsKanji) return@map detected

            // Tokenize the full line text for context-aware readings
            val lineTokens = kuromojiTokenizer.tokenize(detected.text)
            val kanjiTokens = lineTokens.filter { it.containsKanji }

            if (kanjiTokens.isEmpty()) return@map detected

            val elementSpans = buildElementSpans(detected.text, detected.elements)

            detected.copy(
                elements = detected.elements.mapIndexed { index, element ->
                    if (!element.containsKanji) return@mapIndexed element

                    // Map context-aware line tokens to this element using deterministic OCR span mapping.
                    val (elemStart, elemEnd) = elementSpans.getOrNull(index)
                        ?: return@mapIndexed element
                    val rawSegments = resolveElementFromTokens(element.text, elemStart, elemEnd, lineTokens)
                    val segments = fixCounterReadings(element.text, rawSegments).map { stripOkurigana(it) }
                    val reading = buildPositionalReading(element.text, segments)
                    element.copy(reading = reading, kanjiSegments = segments)
                }
            )
        }
    }

    /**
     * Build element spans in line order, handling repeated OCR text deterministically.
     */
    private fun buildElementSpans(
        lineText: String,
        elements: List<TextElement>
    ): List<Pair<Int, Int>> {
        val spans = mutableListOf<Pair<Int, Int>>()
        var cursor = 0

        for (element in elements) {
            val text = element.text
            if (text.isEmpty()) {
                spans.add(cursor to cursor)
                continue
            }

            // Primary: find from current cursor to preserve left-to-right occurrence mapping.
            val idxFromCursor = lineText.indexOf(text, cursor)
            val idx = if (idxFromCursor >= 0) {
                idxFromCursor
            } else {
                // Fallback: global search when OCR element text has small spacing/punctuation mismatch.
                lineText.indexOf(text).takeIf { it >= 0 } ?: cursor
            }
            val end = (idx + text.length).coerceAtMost(lineText.length)
            spans.add(idx to end)
            cursor = end
        }

        return spans
    }

    /**
     * Map Kuromoji tokens that are fully inside this element span.
     */
    private fun resolveElementFromTokens(
        elementText: String,
        elemStart: Int,
        elemEnd: Int,
        lineTokens: List<JapaneseToken>
    ): List<KanjiSegment> {
        val segments = mutableListOf<KanjiSegment>()
        for (token in lineTokens) {
            if (!token.containsKanji) continue
            if (token.reading.isBlank() || token.reading == "*") continue
            val tokenEnd = token.startIndex + token.surface.length

            // Keep tokens that lie completely in this element span.
            if (token.startIndex >= elemStart && tokenEnd <= elemEnd) {
                val localStart = token.startIndex - elemStart
                val localEnd = localStart + token.surface.length
                segments.add(
                    KanjiSegment(
                        text = token.surface,
                        reading = token.reading,
                        startIndex = localStart,
                        endIndex = localEnd
                    )
                )
            }
        }
        return segments
    }

    private fun buildPositionalReading(text: String, segments: List<KanjiSegment>): String? {
        if (segments.isEmpty()) return null
        val result = StringBuilder()
        var i = 0
        while (i < text.length) {
            val segment = segments.find { it.startIndex == i }
            if (segment != null) {
                result.append(segment.reading)
                i = segment.endIndex
            } else {
                result.append('\u3000')
                i++
            }
        }
        return result.toString().trimEnd('\u3000').ifEmpty { null }
    }

    /**
     * Fix counter suffix readings: when a single kanji is preceded by a digit,
     * apply the counter reading (e.g., 月→がつ not つき, 日→にち not ひ).
     */
    private fun fixCounterReadings(text: String, segments: List<KanjiSegment>): List<KanjiSegment> {
        return segments.map { segment ->
            if (segment.text.length == 1 && segment.startIndex > 0) {
                val prevChar = text[segment.startIndex - 1]
                if (isDigitChar(prevChar)) {
                    val counterReading = COUNTER_READINGS[segment.text]
                    if (counterReading != null && counterReading != segment.reading) {
                        Log.d(TAG, "Counter fix: ${segment.text} ${segment.reading}→$counterReading (after $prevChar)")
                        segment.copy(reading = counterReading)
                    } else segment
                } else segment
            } else segment
        }
    }

    /**
     * Strip okurigana from a KanjiSegment so furigana only covers kanji characters.
     * e.g. 送り(おくり) → 送(おく), 新しい(あたらしい) → 新(あたら)
     */
    private fun stripOkurigana(segment: KanjiSegment): KanjiSegment {
        val text = segment.text
        val reading = segment.reading
        if (text.isEmpty() || reading.isEmpty()) return segment

        // Strip trailing okurigana (送り→送, 新しい→新, 食べた→食)
        var trailCount = 0
        for (i in text.length - 1 downTo 0) {
            if (isKanjiChar(text[i])) break
            trailCount++
        }

        var strippedText = text
        var strippedReading = reading
        var endAdjust = 0

        if (trailCount > 0) {
            val suffix = text.substring(text.length - trailCount)
            if (strippedReading.endsWith(suffix)) {
                strippedText = text.substring(0, text.length - trailCount)
                strippedReading = strippedReading.substring(0, strippedReading.length - suffix.length)
                endAdjust = trailCount
            }
        }

        // Strip leading okurigana (お見舞い→見舞い after trailing strip)
        var leadCount = 0
        for (ch in strippedText) {
            if (isKanjiChar(ch)) break
            leadCount++
        }

        var startAdjust = 0
        if (leadCount > 0) {
            val prefix = strippedText.substring(0, leadCount)
            if (strippedReading.startsWith(prefix)) {
                strippedText = strippedText.substring(leadCount)
                strippedReading = strippedReading.substring(prefix.length)
                startAdjust = leadCount
            }
        }

        if (strippedText.isEmpty() || strippedReading.isEmpty()) return segment
        if (startAdjust == 0 && endAdjust == 0) return segment

        return segment.copy(
            text = strippedText,
            reading = strippedReading,
            startIndex = segment.startIndex + startAdjust,
            endIndex = segment.endIndex - endAdjust
        )
    }

    private fun isKanjiChar(ch: Char): Boolean {
        val code = ch.code
        return code in 0x4E00..0x9FFF || code in 0x3400..0x4DBF
    }

    // ========== JMDict fallback (existing logic) ==========

    private suspend fun enrichWithJMDict(
        detectedTexts: List<DetectedText>,
        kanjiLines: List<DetectedText>
    ): List<DetectedText> {
        val candidates = mutableSetOf<String>()
        for (line in kanjiLines) {
            candidates.addAll(extractCandidates(line.text))
        }
        if (candidates.isEmpty()) return detectedTexts

        val readings = try {
            furiganaRepository.batchGetFurigana(candidates.toList())
                .getOrDefault(emptyMap())
        } catch (e: Exception) {
            Log.w(TAG, "Batch lookup failed for ${candidates.size} candidates", e)
            emptyMap()
        }

        if (readings.isEmpty()) return detectedTexts

        val wordMap = readings.mapValues { it.value.reading }
        Log.d(TAG, "JMDict fallback: ${wordMap.size} readings from ${candidates.size} candidates")

        return detectedTexts.map { detected ->
            if (!detected.containsKanji) return@map detected
            detected.copy(
                elements = detected.elements.map { element ->
                    if (element.containsKanji) {
                        val (reading, rawSegments) = resolveElement(element.text, wordMap)
                        val segments = fixCounterReadings(element.text, rawSegments).map { stripOkurigana(it) }
                        val fixedReading = buildPositionalReading(element.text, segments)
                        element.copy(reading = fixedReading, kanjiSegments = segments)
                    } else {
                        element
                    }
                }
            )
        }
    }

    private fun extractCandidates(text: String): Set<String> {
        val candidates = mutableSetOf<String>()
        for (i in text.indices) {
            val ch = text[i].code
            // Fast char-code check for CJK Unified Ideographs + Extension A
            if (ch !in 0x4E00..0x9FFF && ch !in 0x3400..0x4DBF) continue
            // Start at len=2: single kanji rarely have standalone JMDict entries
            for (len in 2..MAX_WORD_LEN.coerceAtMost(text.length - i)) {
                candidates.add(text.substring(i, i + len))
                if (candidates.size >= 150) return candidates
            }
        }
        return candidates
    }

    private fun resolveElement(
        text: String,
        wordMap: Map<String, String>
    ): Pair<String?, List<KanjiSegment>> {
        val segments = mutableListOf<KanjiSegment>()
        val result = StringBuilder()
        var i = 0

        while (i < text.length) {
            if (JapaneseTextUtil.containsKanji(text[i].toString())) {
                var matched = false
                for (len in MAX_WORD_LEN.coerceAtMost(text.length - i) downTo 1) {
                    val sub = text.substring(i, i + len)
                    val reading = wordMap[sub]
                    if (reading != null) {
                        segments.add(KanjiSegment(sub, reading, i, i + len))
                        result.append(reading)
                        i += len
                        matched = true
                        break
                    }
                }
                if (!matched) {
                    result.append('\u3000')
                    i++
                }
            } else {
                result.append('\u3000')
                i++
            }
        }

        val reading = if (segments.isNotEmpty()) result.toString().trimEnd('\u3000') else null
        return Pair(reading, segments)
    }
}
