package com.jworks.kanjilens.domain.usecases

import android.graphics.Rect
import android.util.Size
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognizer
import com.jworks.kanjilens.domain.models.DetectedText
import com.jworks.kanjilens.domain.models.OCRResult
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class ProcessCameraFrameUseCase @Inject constructor(
    private val textRecognizer: TextRecognizer
) {
    suspend fun execute(inputImage: InputImage, imageSize: Size): OCRResult {
        val visionText = textRecognizer.process(inputImage).await()

        val detectedTexts = visionText.textBlocks.flatMap { block ->
            block.lines.map { line ->
                DetectedText(
                    text = line.text,
                    bounds = line.boundingBox ?: Rect(),
                    confidence = line.confidence,
                    language = line.recognizedLanguage ?: "ja"
                )
            }
        }

        return OCRResult(
            texts = detectedTexts,
            timestamp = System.currentTimeMillis(),
            imageSize = imageSize
        )
    }
}
