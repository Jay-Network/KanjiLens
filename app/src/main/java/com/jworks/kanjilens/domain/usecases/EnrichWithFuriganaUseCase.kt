package com.jworks.kanjilens.domain.usecases

import android.util.Log
import com.jworks.kanjilens.data.nlp.KuromojiTokenizer
import com.jworks.kanjilens.domain.models.DetectedText
import com.jworks.kanjilens.domain.models.JapaneseTextUtil
import com.jworks.kanjilens.domain.models.KanjiSegment
import com.jworks.kanjilens.domain.repository.FuriganaRepository
import javax.inject.Inject

class EnrichWithFuriganaUseCase @Inject constructor(
    private val furiganaRepository: FuriganaRepository,
    private val kuromojiTokenizer: KuromojiTokenizer
) {
    companion object {
        private const val TAG = "FuriganaEnrich"
        private const val MAX_WORD_LEN = 6
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

            detected.copy(
                elements = detected.elements.map { element ->
                    if (!element.containsKanji) return@map element

                    // Find kanji tokens that overlap with this element's text
                    val segments = resolveElementFromTokens(element.text, detected.text, lineTokens)
                    val reading = buildPositionalReading(element.text, segments)
                    element.copy(reading = reading, kanjiSegments = segments)
                }
            )
        }
    }

    /**
     * Find the element's text within the line, then map Kuromoji tokens to element positions.
     */
    private fun resolveElementFromTokens(
        elementText: String,
        lineText: String,
        lineTokens: List<com.jworks.kanjilens.domain.models.JapaneseToken>
    ): List<KanjiSegment> {
        // Find where this element appears in the line
        val elemStart = lineText.indexOf(elementText)
        if (elemStart < 0) return emptyList()
        val elemEnd = elemStart + elementText.length

        val segments = mutableListOf<KanjiSegment>()
        for (token in lineTokens) {
            if (!token.containsKanji) continue
            val tokenEnd = token.startIndex + token.surface.length

            // Check if this token overlaps with the element
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
                        val (reading, segments) = resolveElement(element.text, wordMap)
                        element.copy(reading = reading, kanjiSegments = segments)
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
            if (!JapaneseTextUtil.containsKanji(text[i].toString())) continue
            for (len in 1..MAX_WORD_LEN.coerceAtMost(text.length - i)) {
                candidates.add(text.substring(i, i + len))
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
