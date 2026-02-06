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
import com.jworks.kanjilens.domain.models.TextElement

private val LABEL_TEXT_COLOR = Color.White
private val FURIGANA_TEXT_COLOR = Color.White

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
        TextStyle(color = LABEL_TEXT_COLOR, fontSize = settings.labelFontSize.sp)
    }
    val furiganaStyle = remember(settings.labelFontSize) {
        TextStyle(color = FURIGANA_TEXT_COLOR, fontSize = (settings.labelFontSize * 0.75f).sp)
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

            // Draw line bounding box
            drawBoundingBox(bounds, scaleX, scaleY, color, settings.strokeWidth)

            // Draw furigana for each kanji element that has a reading
            val elementsWithReading = detected.elements.filter {
                it.containsKanji && it.reading != null && it.bounds != null
            }
            if (elementsWithReading.isNotEmpty()) {
                elementsWithReading.forEach { element ->
                    drawFuriganaLabel(
                        element, scaleX, scaleY, kanjiColor,
                        textMeasurer, furiganaStyle, labelBg
                    )
                }
            } else {
                // No furigana available - fall back to showing detected text as label
                drawTextLabel(
                    bounds, scaleX, scaleY, color, detected.text,
                    textMeasurer, labelStyle, labelBg
                )
            }
        }
    }
}

private fun DrawScope.drawBoundingBox(
    bounds: Rect,
    scaleX: Float,
    scaleY: Float,
    color: Color,
    strokeWidth: Float
) {
    val left = bounds.left * scaleX
    val top = bounds.top * scaleY
    val width = bounds.width() * scaleX
    val height = bounds.height() * scaleY

    drawRect(
        color = color,
        topLeft = Offset(left, top),
        size = Size(width, height),
        style = Stroke(width = strokeWidth)
    )
}

private fun DrawScope.drawFuriganaLabel(
    element: TextElement,
    scaleX: Float,
    scaleY: Float,
    color: Color,
    textMeasurer: TextMeasurer,
    furiganaStyle: TextStyle,
    labelBg: Color
) {
    val bounds = element.bounds ?: return
    val reading = element.reading ?: return

    val elemLeft = bounds.left * scaleX
    val elemTop = bounds.top * scaleY
    val elemWidth = bounds.width() * scaleX

    val measured = textMeasurer.measure(reading, furiganaStyle)
    val furiganaWidth = measured.size.width.toFloat()
    val furiganaHeight = measured.size.height.toFloat()

    // Center furigana above the element's bounding box
    val labelPadH = 6f
    val labelPadV = 3f
    val bgWidth = furiganaWidth + labelPadH * 2
    val bgHeight = furiganaHeight + labelPadV * 2
    val bgLeft = elemLeft + (elemWidth - bgWidth) / 2f
    val bgTop = (elemTop - bgHeight - 2f).coerceAtLeast(0f)

    // Background pill
    drawRoundRect(
        color = labelBg,
        topLeft = Offset(bgLeft, bgTop),
        size = Size(bgWidth, bgHeight),
        cornerRadius = CornerRadius(4f, 4f)
    )

    // Color accent bar on top
    drawRoundRect(
        color = color,
        topLeft = Offset(bgLeft, bgTop),
        size = Size(bgWidth, 2f),
        cornerRadius = CornerRadius(2f, 2f)
    )

    // Furigana text
    drawText(
        textMeasurer = textMeasurer,
        text = reading,
        topLeft = Offset(bgLeft + labelPadH, bgTop + labelPadV),
        style = furiganaStyle
    )
}

private fun DrawScope.drawTextLabel(
    bounds: Rect,
    scaleX: Float,
    scaleY: Float,
    color: Color,
    text: String,
    textMeasurer: TextMeasurer,
    labelStyle: TextStyle,
    labelBg: Color
) {
    val left = bounds.left * scaleX
    val top = bounds.top * scaleY

    val measuredText = textMeasurer.measure(text, labelStyle)
    val labelWidth = measuredText.size.width.toFloat() + 12f
    val labelHeight = measuredText.size.height.toFloat() + 8f
    val labelTop = (top - labelHeight - 4f).coerceAtLeast(0f)

    drawRoundRect(
        color = labelBg,
        topLeft = Offset(left, labelTop),
        size = Size(labelWidth, labelHeight),
        cornerRadius = CornerRadius(6f, 6f)
    )

    drawRoundRect(
        color = color,
        topLeft = Offset(left, labelTop),
        size = Size(4f, labelHeight),
        cornerRadius = CornerRadius(2f, 2f)
    )

    drawText(
        textMeasurer = textMeasurer,
        text = text,
        topLeft = Offset(left + 8f, labelTop + 4f),
        style = labelStyle
    )
}
