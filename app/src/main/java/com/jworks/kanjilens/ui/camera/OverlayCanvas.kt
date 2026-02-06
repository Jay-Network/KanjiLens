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

        // FIX: Calculate visible crop region in IMAGE coordinates
        // The scaled image may overflow the canvas - we need to know which part is visible
        val scaledImageWidth = effectiveWidth * scale
        val scaledImageHeight = effectiveHeight * scale

        // Crop offset: how much of the scaled image is cut off on each side
        val cropOffsetX = (scaledImageWidth - size.width) / 2f
        val cropOffsetY = (scaledImageHeight - size.height) / 2f

        // Only render kanji elements that have readings
        for (detected in detectedTexts) {
            if (!detected.containsKanji) continue

            for (element in detected.elements) {
                if (!element.containsKanji) continue
                val bounds = element.bounds ?: continue
                if (bounds.isEmpty) continue

                try {
                    if (element.kanjiSegments.isNotEmpty()) {
                        // Per-segment rendering: individual boxes + furigana per kanji word
                        drawKanjiSegments(
                            bounds, element.text.length, element.kanjiSegments,
                            scale, cropOffsetX, cropOffsetY, kanjiColor, settings.strokeWidth,
                            textMeasurer, furiganaStyle, labelBg
                        )
                    } else if (element.reading != null) {
                        // Fallback: element-level rendering
                        drawBoundingBox(bounds, scale, cropOffsetX, cropOffsetY, kanjiColor, settings.strokeWidth)
                        drawFuriganaLabel(
                            bounds, element.reading, scale, cropOffsetX, cropOffsetY,
                            kanjiColor, textMeasurer, furiganaStyle, labelBg
                        )
                    }
                } catch (e: IllegalArgumentException) {
                    // Skip this element if drawing fails (edge case with invalid dimensions)
                    android.util.Log.w("OverlayCanvas", "Skipped drawing element due to: ${e.message}")
                }
            }
        }
    }
}

private fun DrawScope.drawBoundingBox(
    bounds: Rect,
    scale: Float,
    cropOffsetX: Float,
    cropOffsetY: Float,
    color: Color,
    strokeWidth: Float
) {
    val left = bounds.left * scale - cropOffsetX
    val top = bounds.top * scale - cropOffsetY
    val width = bounds.width() * scale
    val height = bounds.height() * scale

    // Skip if element is completely off-screen or has invalid dimensions
    if (width <= 0 || height <= 0) return
    if (left + width < 0 || top + height < 0) return  // Completely off left/top
    if (left > size.width || top > size.height) return  // Completely off right/bottom

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
    cropOffsetX: Float,
    cropOffsetY: Float,
    color: Color,
    strokeWidth: Float,
    textMeasurer: TextMeasurer,
    furiganaStyle: TextStyle,
    labelBg: Color
) {
    val elemLeft = elementBounds.left * scale - cropOffsetX
    val elemTop = elementBounds.top * scale - cropOffsetY
    val elemWidth = elementBounds.width() * scale
    val elemHeight = elementBounds.height() * scale

    // Skip if element is completely off-screen or has invalid dimensions
    if (elemWidth <= 0 || elemHeight <= 0) return
    if (elemLeft + elemWidth < 0 || elemTop + elemHeight < 0) return  // Off left/top
    if (elemLeft > size.width || elemTop > size.height) return  // Off right/bottom

    if (textLength <= 0) return
    val charWidth = elemWidth / textLength.toFloat()

    for (segment in segments) {
        val segLeft = elemLeft + segment.startIndex * charWidth
        val segWidth = (segment.endIndex - segment.startIndex) * charWidth

        // Skip segment if it's off-screen or has invalid dimensions
        if (segWidth <= 0) continue
        if (segLeft + segWidth < 0 || segLeft > size.width) continue

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

        // Skip label if it would be invalid or off-screen
        if (bgWidth <= 0 || bgHeight <= 0) continue
        if (bgLeft.isNaN() || bgTop.isNaN()) continue
        if (bgLeft + bgWidth < -100 || bgTop + bgHeight < -100) continue  // Far off-screen

        // Validate all dimensions are reasonable before drawing
        if (bgWidth > 10000 || bgHeight > 10000) continue  // Unreasonably large

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
    cropOffsetX: Float,
    cropOffsetY: Float,
    color: Color,
    textMeasurer: TextMeasurer,
    furiganaStyle: TextStyle,
    labelBg: Color
) {
    val elemLeft = bounds.left * scale - cropOffsetX
    val elemTop = bounds.top * scale - cropOffsetY
    val elemWidth = bounds.width() * scale
    val elemHeight = bounds.height() * scale

    // Skip if element is completely off-screen or has invalid dimensions
    if (elemWidth <= 0 || elemHeight <= 0) return
    if (elemLeft + elemWidth < 0 || elemTop + elemHeight < 0) return  // Off left/top
    if (elemLeft > size.width || elemTop > size.height) return  // Off right/bottom

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

    // Skip if label would be off-screen or have invalid dimensions
    if (bgWidth <= 0 || bgHeight <= 0) return
    if (bgLeft.isNaN() || bgTop.isNaN()) return
    if (bgLeft + bgWidth < -100 || bgTop + bgHeight < -100) return  // Far off-screen
    if (bgWidth > 10000 || bgHeight > 10000) return  // Unreasonably large

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
