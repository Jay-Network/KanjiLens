package com.jworks.kanjilens.domain.usecases

import android.util.Log
import com.jworks.kanjilens.domain.models.DetectedText
import com.jworks.kanjilens.domain.repository.FuriganaRepository
import javax.inject.Inject

class EnrichWithFuriganaUseCase @Inject constructor(
    private val furiganaRepository: FuriganaRepository
) {
    companion object {
        private const val TAG = "FuriganaEnrich"
    }

    suspend fun execute(detectedTexts: List<DetectedText>): List<DetectedText> {
        // Collect all kanji-containing element texts for batch lookup
        val kanjiWords = detectedTexts
            .flatMap { it.elements }
            .filter { it.containsKanji }
            .map { it.text }
            .distinct()

        if (kanjiWords.isEmpty()) return detectedTexts

        // Batch lookup: local DB first, then backend fallback (handled by repository)
        val readings = try {
            furiganaRepository.batchGetFurigana(kanjiWords).getOrDefault(emptyMap())
        } catch (e: Exception) {
            Log.w(TAG, "Batch furigana lookup failed for ${kanjiWords.size} words", e)
            emptyMap()
        }

        if (readings.isNotEmpty()) {
            Log.d(TAG, "Resolved ${readings.size}/${kanjiWords.size} readings")
        }

        // Enrich elements with readings
        return detectedTexts.map { detected ->
            detected.copy(
                elements = detected.elements.map { element ->
                    if (element.containsKanji) {
                        val result = readings[element.text]
                        element.copy(reading = result?.reading)
                    } else {
                        element
                    }
                }
            )
        }
    }
}
