package com.jworks.kanjilens.domain.usecases

import android.util.Log
import com.jworks.kanjilens.domain.models.DetectedText
import com.jworks.kanjilens.domain.models.JapaneseTextUtil
import com.jworks.kanjilens.domain.repository.FuriganaRepository
import javax.inject.Inject

class EnrichWithFuriganaUseCase @Inject constructor(
    private val furiganaRepository: FuriganaRepository
) {
    companion object {
        private const val TAG = "FuriganaEnrich"
        private const val MAX_WORD_LEN = 6
    }

    suspend fun execute(detectedTexts: List<DetectedText>): List<DetectedText> {
        val kanjiLines = detectedTexts.filter { it.containsKanji }
        if (kanjiLines.isEmpty()) return detectedTexts

        // Step 1: Extract candidate substrings from full line texts (line-level context)
        val candidates = mutableSetOf<String>()
        for (line in kanjiLines) {
            candidates.addAll(extractCandidates(line.text))
        }
        if (candidates.isEmpty()) return detectedTexts

        // Step 2: Batch lookup all candidates against JMDict
        val readings = try {
            furiganaRepository.batchGetFurigana(candidates.toList())
                .getOrDefault(emptyMap())
        } catch (e: Exception) {
            Log.w(TAG, "Batch lookup failed for ${candidates.size} candidates", e)
            emptyMap()
        }

        if (readings.isEmpty()) return detectedTexts

        val wordMap = readings.mapValues { it.value.reading }
        Log.d(TAG, "Loaded ${wordMap.size} readings from ${candidates.size} candidates")

        // Step 3: For each element that contains kanji, resolve its reading
        // using greedy longest-match with the pre-loaded wordMap
        return detectedTexts.map { detected ->
            if (!detected.containsKanji) return@map detected

            detected.copy(
                elements = detected.elements.map { element ->
                    if (element.containsKanji) {
                        val reading = resolveElementReading(element.text, wordMap)
                        element.copy(reading = reading)
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

    /**
     * Greedy longest-match on the element text to build a combined reading.
     * Only the kanji portions get replaced with readings; kana pass through.
     * Returns null if no kanji in the element could be resolved.
     */
    private fun resolveElementReading(text: String, wordMap: Map<String, String>): String? {
        val result = StringBuilder()
        var i = 0
        var matchCount = 0

        while (i < text.length) {
            if (JapaneseTextUtil.containsKanji(text[i].toString())) {
                var matched = false
                for (len in MAX_WORD_LEN.coerceAtMost(text.length - i) downTo 1) {
                    val sub = text.substring(i, i + len)
                    val reading = wordMap[sub]
                    if (reading != null) {
                        result.append(reading)
                        i += len
                        matched = true
                        matchCount++
                        break
                    }
                }
                if (!matched) {
                    result.append(text[i])
                    i++
                }
            } else {
                result.append(text[i])
                i++
            }
        }

        return if (matchCount > 0) result.toString() else null
    }
}
