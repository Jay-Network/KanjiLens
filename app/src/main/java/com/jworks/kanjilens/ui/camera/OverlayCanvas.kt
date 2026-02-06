package com.jworks.kanjilens.ui.camera

import android.graphics.Rect
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.jworks.kanjilens.domain.models.DetectedText

@Composable
fun TextOverlay(
    detectedTexts: List<DetectedText>,
    imageWidth: Int,
    imageHeight: Int,
    rotationDegrees: Int,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        // For portrait mode (rotation 90 or 270), the image dimensions are swapped
        // relative to the screen. ML Kit returns coordinates in the rotated frame,
        // so we need to account for this when mapping to screen coordinates.
        val isRotated = rotationDegrees == 90 || rotationDegrees == 270
        val effectiveWidth = if (isRotated) imageHeight else imageWidth
        val effectiveHeight = if (isRotated) imageWidth else imageHeight

        val scaleX = size.width / effectiveWidth
        val scaleY = size.height / effectiveHeight

        detectedTexts.forEach { detected ->
            val bounds = detected.bounds ?: return@forEach
            drawBoundingBox(bounds, scaleX, scaleY)
        }
    }
}

private fun DrawScope.drawBoundingBox(
    bounds: Rect,
    scaleX: Float,
    scaleY: Float
) {
    val left = bounds.left * scaleX
    val top = bounds.top * scaleY
    val width = bounds.width() * scaleX
    val height = bounds.height() * scaleY

    // Bounding box outline
    drawRect(
        color = Color(0xFF4CAF50),
        topLeft = Offset(left, top),
        size = Size(width, height),
        style = Stroke(width = 3f)
    )

    // Semi-transparent label background above the box
    val labelHeight = 36f
    val labelTop = (top - labelHeight).coerceAtLeast(0f)
    drawRect(
        color = Color.Black.copy(alpha = 0.6f),
        topLeft = Offset(left, labelTop),
        size = Size(width.coerceAtLeast(80f), labelHeight)
    )
}
