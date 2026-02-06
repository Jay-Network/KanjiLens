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
    val labelBg = remember(settings.labelBackgroundAlpha) {
        Color.Black.copy(alpha = settings.labelBackgroundAlpha)
    }
    val furiganaStyle = remember(settings.labelFontSize) {
        TextStyle(color = LABEL_TEXT_COLOR, fontSize = (settings.labelFontSize * 0.75f).sp)
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        if (imageWidth <= 0 || imageHeight <= 0) return@Canvas

        val isRotated = rotationDegrees == 90 || rotationDegrees == 270
        val effectiveWidth = if (isRotated) imageHeight else imageWidth
        val effectiveHeight = if (isRotated) imageWidth else imageHeight

        val scaleX = size.width / effectiveWidth
        val scaleY = size.height / effectiveHeight

        // Only render kanji elements that have readings
        for (detected in detectedTexts) {
            if (!detected.containsKanji) continue

            for (element in detected.elements) {
                if (!element.containsKanji) continue
                val bounds = element.bounds ?: continue
                if (bounds.isEmpty) continue

                // Bounding box around kanji element
                drawBoundingBox(bounds, scaleX, scaleY, kanjiColor, settings.strokeWidth)

                // Furigana pill above kanji element (only if reading resolved)
                if (element.reading != null) {
                    drawFuriganaLabel(
                        bounds, element.reading, scaleX, scaleY,
                        kanjiColor, textMeasurer, furiganaStyle, labelBg
                    )
                }
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
    bounds: Rect,
    reading: String,
    scaleX: Float,
    scaleY: Float,
    color: Color,
    textMeasurer: TextMeasurer,
    furiganaStyle: TextStyle,
    labelBg: Color
) {
    val elemLeft = bounds.left * scaleX
    val elemTop = bounds.top * scaleY
    val elemWidth = bounds.width() * scaleX

    val measured = textMeasurer.measure(reading, furiganaStyle)
    val furiganaWidth = measured.size.width.toFloat()
    val furiganaHeight = measured.size.height.toFloat()

    // Center furigana pill above the kanji element
    val padH = 6f
    val padV = 3f
    val bgWidth = furiganaWidth + padH * 2
    val bgHeight = furiganaHeight + padV * 2
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
        topLeft = Offset(bgLeft + padH, bgTop + padV),
        style = furiganaStyle
    )
}
