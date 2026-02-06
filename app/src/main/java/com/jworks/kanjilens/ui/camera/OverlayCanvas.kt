package com.jworks.kanjilens.ui.camera

import android.graphics.Rect
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.jworks.kanjilens.domain.models.AppSettings
import com.jworks.kanjilens.domain.models.DetectedText

private val LABEL_TEXT_COLOR = Color.White

@Composable
fun TextOverlay(
    detectedTexts: List<DetectedText>,
    imageWidth: Int,
    imageHeight: Int,
    rotationDegrees: Int,
    settings: AppSettings,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val kanjiColor = remember(settings.kanjiColor) { Color(settings.kanjiColor) }
    val kanaColor = remember(settings.kanaColor) { Color(settings.kanaColor) }
    val labelBg = remember(settings.labelBackgroundAlpha) {
        Color.Black.copy(alpha = settings.labelBackgroundAlpha)
    }
    val labelStyle = remember(settings.labelFontSize) {
        TextStyle(
            color = LABEL_TEXT_COLOR,
            fontSize = settings.labelFontSize.sp
        )
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        if (imageWidth <= 0 || imageHeight <= 0) return@Canvas

        val isRotated = rotationDegrees == 90 || rotationDegrees == 270
        val effectiveWidth = if (isRotated) imageHeight else imageWidth
        val effectiveHeight = if (isRotated) imageWidth else imageHeight

        val scaleX = size.width / effectiveWidth
        val scaleY = size.height / effectiveHeight

        detectedTexts.forEach { detected ->
            val bounds = detected.bounds ?: return@forEach
            if (bounds.isEmpty) return@forEach
            val color = if (detected.containsKanji) kanjiColor else kanaColor
            drawDetectedText(
                bounds, scaleX, scaleY, color, detected.text,
                textMeasurer, labelStyle, labelBg, settings.strokeWidth
            )
        }
    }
}

private fun DrawScope.drawDetectedText(
    bounds: Rect,
    scaleX: Float,
    scaleY: Float,
    color: Color,
    text: String,
    textMeasurer: TextMeasurer,
    labelStyle: TextStyle,
    labelBg: Color,
    strokeWidth: Float
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
        style = Stroke(width = strokeWidth)
    )

    // Measure text for label sizing
    val measuredText = textMeasurer.measure(text, labelStyle)
    val labelWidth = measuredText.size.width.toFloat() + 12f
    val labelHeight = measuredText.size.height.toFloat() + 8f
    val labelTop = (top - labelHeight - 4f).coerceAtLeast(0f)

    // Label background (rounded)
    drawRoundRect(
        color = labelBg,
        topLeft = Offset(left, labelTop),
        size = Size(labelWidth, labelHeight),
        cornerRadius = CornerRadius(6f, 6f)
    )

    // Color indicator bar on left of label
    drawRoundRect(
        color = color,
        topLeft = Offset(left, labelTop),
        size = Size(4f, labelHeight),
        cornerRadius = CornerRadius(2f, 2f)
    )

    // Text label
    drawText(
        textMeasurer = textMeasurer,
        text = text,
        topLeft = Offset(left + 8f, labelTop + 4f),
        style = labelStyle
    )
}
