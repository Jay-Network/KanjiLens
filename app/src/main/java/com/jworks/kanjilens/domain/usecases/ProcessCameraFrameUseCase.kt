package com.jworks.kanjilens.domain.usecases

import android.util.Log
import android.util.Size
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognizer
import com.jworks.kanjilens.domain.models.DetectedText
import com.jworks.kanjilens.domain.models.JapaneseTextUtil
import com.jworks.kanjilens.domain.models.OCRResult
import com.jworks.kanjilens.domain.models.TextElement
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class ProcessCameraFrameUseCase @Inject constructor(
    private val textRecognizer: TextRecognizer
) {
    companion object {
        private const val TAG = "OCR"
        private const val MIN_JAPANESE_RATIO = 0.3f
    }

    suspend fun execute(inputImage: InputImage, imageSize: Size): OCRResult {
        val startTime = System.currentTimeMillis()

        val visionText = textRecognizer.process(inputImage).await()

        val detectedTexts = visionText.textBlocks.flatMap { block ->
            block.lines.mapNotNull { line ->
                // Filter: only keep lines with Japanese content
                if (!JapaneseTextUtil.containsJapanese(line.text)) return@mapNotNull null

                // Skip lines that are mostly non-Japanese (e.g. URLs, numbers)
                if (JapaneseTextUtil.japaneseRatio(line.text) < MIN_JAPANESE_RATIO) {
                    return@mapNotNull null
                }

                // Extract word-level elements from the line
                val elements = line.elements.map { element ->
                    TextElement(
                        text = element.text,
                        bounds = element.boundingBox,
                        containsKanji = JapaneseTextUtil.containsKanji(element.text)
                    )
                }

                DetectedText(
                    text = line.text,
                    bounds = line.boundingBox,
                    confidence = line.confidence,
                    language = line.recognizedLanguage ?: "ja",
                    containsKanji = JapaneseTextUtil.containsKanji(line.text),
                    elements = elements
                )
            }
        }

        val processingTime = System.currentTimeMillis() - startTime
        Log.d(TAG, "OCR: ${detectedTexts.size} lines, ${processingTime}ms, " +
                "kanji lines: ${detectedTexts.count { it.containsKanji }}")

        return OCRResult(
            texts = detectedTexts,
            timestamp = System.currentTimeMillis(),
            imageSize = imageSize,
            processingTimeMs = processingTime
        )
    }
}
