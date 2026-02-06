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

private val KANJI_COLOR = Color(0xFF4CAF50)   // green - needs furigana
private val KANA_COLOR = Color(0xFF2196F3)     // blue - already readable

@Composable
fun TextOverlay(
    detectedTexts: List<DetectedText>,
    imageWidth: Int,
    imageHeight: Int,
    rotationDegrees: Int,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val isRotated = rotationDegrees == 90 || rotationDegrees == 270
        val effectiveWidth = if (isRotated) imageHeight else imageWidth
        val effectiveHeight = if (isRotated) imageWidth else imageHeight

        val scaleX = size.width / effectiveWidth
        val scaleY = size.height / effectiveHeight

        detectedTexts.forEach { detected ->
            val bounds = detected.bounds ?: return@forEach
            val color = if (detected.containsKanji) KANJI_COLOR else KANA_COLOR
            drawBoundingBox(bounds, scaleX, scaleY, color)
        }
    }
}

private fun DrawScope.drawBoundingBox(
    bounds: Rect,
    scaleX: Float,
    scaleY: Float,
    color: Color
) {
    val left = bounds.left * scaleX
    val top = bounds.top * scaleY
    val width = bounds.width() * scaleX
    val height = bounds.height() * scaleY

    // Bounding box outline
    drawRect(
        color = color,
        topLeft = Offset(left, top),
        size = Size(width, height),
        style = Stroke(width = 3f)
    )

    // Semi-transparent label background above the box
    val labelHeight = 36f
    val labelTop = (top - labelHeight).coerceAtLeast(0f)
    drawRect(
        color = color.copy(alpha = 0.3f),
        topLeft = Offset(left, labelTop),
        size = Size(width.coerceAtLeast(80f), labelHeight)
    )
}
