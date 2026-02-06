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
import com.jworks.kanjilens.domain.models.KanjiSegment

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
        val effectiveWidth = (if (isRotated) imageHeight else imageWidth).toFloat()
        val effectiveHeight = (if (isRotated) imageWidth else imageHeight).toFloat()

        // Match PreviewView.ScaleType.FILL_CENTER: uniform scale + centered crop
        val scale = maxOf(size.width / effectiveWidth, size.height / effectiveHeight)
        val offsetX = (effectiveWidth * scale - size.width) / 2f
        val offsetY = (effectiveHeight * scale - size.height) / 2f

        // Only render kanji elements that have readings
        for (detected in detectedTexts) {
            if (!detected.containsKanji) continue

            for (element in detected.elements) {
                if (!element.containsKanji) continue
                val bounds = element.bounds ?: continue
                if (bounds.isEmpty) continue

                if (element.kanjiSegments.isNotEmpty()) {
                    // Per-segment rendering: individual boxes + furigana per kanji word
                    drawKanjiSegments(
                        bounds, element.text.length, element.kanjiSegments,
                        scale, offsetX, offsetY, kanjiColor, settings.strokeWidth,
                        textMeasurer, furiganaStyle, labelBg
                    )
                } else if (element.reading != null) {
                    // Fallback: element-level rendering
                    drawBoundingBox(bounds, scale, offsetX, offsetY, kanjiColor, settings.strokeWidth)
                    drawFuriganaLabel(
                        bounds, element.reading, scale, offsetX, offsetY,
                        kanjiColor, textMeasurer, furiganaStyle, labelBg
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawBoundingBox(
    bounds: Rect,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    color: Color,
    strokeWidth: Float
) {
    val left = bounds.left * scale - offsetX
    val top = bounds.top * scale - offsetY
    val width = bounds.width() * scale
    val height = bounds.height() * scale

    drawRect(
        color = color,
        topLeft = Offset(left, top),
        size = Size(width, height),
        style = Stroke(width = strokeWidth)
    )
}

private fun DrawScope.drawKanjiSegments(
    elementBounds: Rect,
    textLength: Int,
    segments: List<KanjiSegment>,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    color: Color,
    strokeWidth: Float,
    textMeasurer: TextMeasurer,
    furiganaStyle: TextStyle,
    labelBg: Color
) {
    val elemLeft = elementBounds.left * scale - offsetX
    val elemTop = elementBounds.top * scale - offsetY
    val elemWidth = elementBounds.width() * scale
    val elemHeight = elementBounds.height() * scale

    if (textLength <= 0) return
    val charWidth = elemWidth / textLength.toFloat()

    for (segment in segments) {
        val segLeft = elemLeft + segment.startIndex * charWidth
        val segWidth = (segment.endIndex - segment.startIndex) * charWidth

        // Bounding box for this kanji segment
        drawRect(
            color = color,
            topLeft = Offset(segLeft, elemTop),
            size = Size(segWidth, elemHeight),
            style = Stroke(width = strokeWidth)
        )

        // Furigana pill above this segment
        val measured = textMeasurer.measure(segment.reading, furiganaStyle)
        val furiganaWidth = measured.size.width.toFloat()
        val furiganaHeight = measured.size.height.toFloat()

        val padH = 6f
        val padV = 3f
        val bgWidth = furiganaWidth + padH * 2
        val bgHeight = furiganaHeight + padV * 2
        val bgLeft = segLeft + (segWidth - bgWidth) / 2f
        val bgTop = (elemTop - bgHeight - 2f).coerceAtLeast(0f)

        drawRoundRect(
            color = labelBg,
            topLeft = Offset(bgLeft, bgTop),
            size = Size(bgWidth, bgHeight),
            cornerRadius = CornerRadius(4f, 4f)
        )

        drawRoundRect(
            color = color,
            topLeft = Offset(bgLeft, bgTop),
            size = Size(bgWidth, 2f),
            cornerRadius = CornerRadius(2f, 2f)
        )

        drawText(
            textMeasurer = textMeasurer,
            text = segment.reading,
            topLeft = Offset(bgLeft + padH, bgTop + padV),
            style = furiganaStyle
        )
    }
}

private fun DrawScope.drawFuriganaLabel(
    bounds: Rect,
    reading: String,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    color: Color,
    textMeasurer: TextMeasurer,
    furiganaStyle: TextStyle,
    labelBg: Color
) {
    val elemLeft = bounds.left * scale - offsetX
    val elemTop = bounds.top * scale - offsetY
    val elemWidth = bounds.width() * scale

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
