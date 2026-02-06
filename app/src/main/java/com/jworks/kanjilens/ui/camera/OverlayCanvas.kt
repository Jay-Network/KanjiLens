package com.jworks.kanjilens.ui.camera

import android.graphics.Rect
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import com.jworks.kanjilens.domain.models.DetectedText

@Composable
fun TextOverlay(
    detectedTexts: List<DetectedText>,
    imageWidth: Int,
    imageHeight: Int,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val scaleX = size.width / imageWidth
        val scaleY = size.height / imageHeight

        detectedTexts.forEach { detected ->
            val bounds = detected.bounds ?: return@forEach

            val left = bounds.left * scaleX
            val top = bounds.top * scaleY
            val width = bounds.width() * scaleX
            val height = bounds.height() * scaleY

            // Bounding box
            drawRect(
                color = Color(0xFF4CAF50),
                topLeft = Offset(left, top),
                size = Size(width, height),
                style = Stroke(width = 3f)
            )

            // Label background
            drawRect(
                color = Color.Black.copy(alpha = 0.6f),
                topLeft = Offset(left, top - 40f),
                size = Size(width.coerceAtLeast(80f), 36f)
            )
        }
    }
}

fun mapToScreenCoordinates(
    imageBounds: Rect,
    imageWidth: Int,
    imageHeight: Int,
    screenWidth: Int,
    screenHeight: Int,
    rotation: Int
): Rect {
    val rotatedBounds = when (rotation) {
        90 -> Rect(
            imageBounds.top,
            imageWidth - imageBounds.right,
            imageBounds.bottom,
            imageWidth - imageBounds.left
        )
        270 -> Rect(
            imageHeight - imageBounds.bottom,
            imageBounds.left,
            imageHeight - imageBounds.top,
            imageBounds.right
        )
        else -> imageBounds
    }

    val scaleX = screenWidth.toFloat() / imageWidth
    val scaleY = screenHeight.toFloat() / imageHeight

    return Rect(
        (rotatedBounds.left * scaleX).toInt(),
        (rotatedBounds.top * scaleY).toInt(),
        (rotatedBounds.right * scaleX).toInt(),
        (rotatedBounds.bottom * scaleY).toInt()
    )
}
