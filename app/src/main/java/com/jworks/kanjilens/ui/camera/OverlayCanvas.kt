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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.jworks.kanjilens.domain.models.AppSettings
import com.jworks.kanjilens.domain.models.DetectedText
import com.jworks.kanjilens.domain.models.KanjiSegment

@Composable
fun TextOverlay(
    detectedTexts: List<DetectedText>,
    imageWidth: Int,
    imageHeight: Int,
    rotationDegrees: Int,
    settings: AppSettings,
    isVerticalMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val kanjiColor = remember(settings.kanjiColor) { Color(settings.kanjiColor) }
    val labelBg = remember(settings.labelBackgroundAlpha) {
        Color.Black.copy(alpha = settings.labelBackgroundAlpha)
    }
    val furiganaStyle = remember(settings.labelFontSize, settings.furiganaIsBold, settings.furiganaUseWhiteText) {
        TextStyle(
            color = if (settings.furiganaUseWhiteText) Color.White else Color.Black,
            fontSize = (settings.labelFontSize * 0.75f).sp,
            fontWeight = if (settings.furiganaIsBold) FontWeight.Bold else FontWeight.Normal
        )
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

        // In partial modes, skip elements partially hidden behind the panel edge.
        // FILL_CENTER mapping makes elemLeft deeply negative (cropOffsetX ~405px on Z Flip 7),
        // so left-edge clipping is handled by the OCR filter in CameraViewModel instead.
        // Here we only clip at the bottom boundary where the panel/pad starts.
        val isPartial = settings.partialModeBoundaryRatio < 0.99f
        val clipLeftEdge = 0f  // OCR filter handles left-edge spatial filtering
        val clipBottomEdge = if (isPartial) {
            if (isVerticalMode) {
                size.height * PartialModeConstants.VERT_PAD_TOP_RATIO
            } else {
                size.height * PartialModeConstants.HORIZ_CAMERA_HEIGHT_RATIO
            }
        } else 0f

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
                        scale, cropOffsetX, cropOffsetY, kanjiColor, settings.strokeWidth,
                        textMeasurer, furiganaStyle, labelBg, settings.showBoxes,
                        isVerticalMode, clipLeftEdge, clipBottomEdge
                    )
                } else if (element.reading != null) {
                    // Fallback: element-level rendering
                    val elemLeft = bounds.left * scale - cropOffsetX
                    val elemTop = bounds.top * scale - cropOffsetY
                    if (clipLeftEdge > 0f && elemLeft < clipLeftEdge) continue
                    if (clipBottomEdge > 0f && elemTop > clipBottomEdge) continue

                    if (settings.showBoxes) {
                        drawBoundingBox(bounds, scale, cropOffsetX, cropOffsetY, kanjiColor, settings.strokeWidth)
                    }
                    drawFuriganaLabel(
                        bounds, element.reading, scale, cropOffsetX, cropOffsetY,
                        kanjiColor, textMeasurer, furiganaStyle, labelBg,
                        isVerticalMode
                    )
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

    // Quick bounds check and coercion
    if (width <= 0 || height <= 0) return
    if (left + width < -50 || left > size.width + 50) return  // Off-screen horizontally
    if (top + height < -50 || top > size.height + 50) return  // Off-screen vertically

    val safeWidth = width.coerceAtLeast(0.1f)
    val safeHeight = height.coerceAtLeast(0.1f)

    drawRect(
        color = color,
        topLeft = Offset(left, top),
        size = Size(safeWidth, safeHeight),
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
    labelBg: Color,
    showBoxes: Boolean,
    isVerticalMode: Boolean = false,
    clipLeftEdge: Float = 0f,
    clipBottomEdge: Float = 0f
) {
    val elemLeft = elementBounds.left * scale - cropOffsetX
    val elemTop = elementBounds.top * scale - cropOffsetY
    val elemWidth = elementBounds.width() * scale
    val elemHeight = elementBounds.height() * scale

    // Quick bounds check
    if (elemWidth <= 0 || elemHeight <= 0 || textLength <= 0) return
    if (elemLeft + elemWidth < -50 || elemLeft > size.width + 50) return  // Off-screen horizontally
    if (elemTop + elemHeight < -50 || elemTop > size.height + 50) return  // Off-screen vertically

    // Skip elements fully hidden behind panel left edge
    if (clipLeftEdge > 0f && elemLeft < clipLeftEdge) return
    // Skip elements that START below the panel bottom edge
    if (clipBottomEdge > 0f && elemTop > clipBottomEdge) return

    if (isVerticalMode) {
        // Vertical mode: characters stacked top-to-bottom, furigana to the RIGHT
        val charHeight = elemHeight / textLength.toFloat()

        for (segment in segments) {
            val segTop = elemTop + segment.startIndex * charHeight
            val segHeight = (segment.endIndex - segment.startIndex) * charHeight

            // Quick validation
            if (segHeight <= 0 || segTop + segHeight < -50 || segTop > size.height + 50) continue

            // Per-segment clip: skip segments that extend into the pad/panel area
            if (clipBottomEdge > 0f && segTop + segHeight > clipBottomEdge) continue

            val safeSegHeight = segHeight.coerceAtLeast(0.1f)
            val safeElemWidth = elemWidth.coerceAtLeast(0.1f)

            // Bounding box for this kanji segment (if enabled)
            if (showBoxes) {
                drawRect(
                    color = color,
                    topLeft = Offset(elemLeft, segTop),
                    size = Size(safeElemWidth, safeSegHeight),
                    style = Stroke(width = strokeWidth)
                )
            }

            // Vertical furigana pill to the RIGHT of this segment
            drawVerticalFurigana(
                reading = segment.reading,
                anchorLeft = elemLeft + elemWidth + 2f,
                anchorTop = segTop,
                anchorHeight = segHeight,
                textMeasurer = textMeasurer,
                furiganaStyle = furiganaStyle,
                labelBg = labelBg
            )
        }
    } else {
        // Horizontal mode: characters laid out left-to-right, furigana ABOVE
        // All segments share the same Y — skip entire element if bottom is clipped by panel
        if (clipBottomEdge > 0f && elemTop + elemHeight > clipBottomEdge) return

        val charWidth = elemWidth / textLength.toFloat()

        for (segment in segments) {
            val segLeft = elemLeft + segment.startIndex * charWidth
            val segWidth = (segment.endIndex - segment.startIndex) * charWidth

            // Quick validation
            if (segWidth <= 0 || segLeft + segWidth < -50 || segLeft > size.width + 50) continue

            val safeSegWidth = segWidth.coerceAtLeast(0.1f)
            val safeElemHeight = elemHeight.coerceAtLeast(0.1f)

            // Bounding box for this kanji segment (if enabled)
            if (showBoxes) {
                drawRect(
                    color = color,
                    topLeft = Offset(segLeft, elemTop),
                    size = Size(safeSegWidth, safeElemHeight),
                    style = Stroke(width = strokeWidth)
                )
            }

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

            // Quick validation and coercion
            if (bgWidth <= 0 || bgHeight <= 0 || bgLeft.isNaN() || bgTop.isNaN()) continue
            if (bgLeft + bgWidth < -50 || bgTop > size.height + 50) continue  // Off-screen

            val safeBgWidth = bgWidth.coerceAtLeast(0.1f)
            val safeBgHeight = bgHeight.coerceAtLeast(0.1f)

            // Skip text if it would create invalid constraints
            val textLeft = bgLeft + padH
            val textTop = bgTop + padV
            if (textLeft < 0 || textTop < 0) continue
            if (textLeft >= size.width || textTop >= size.height) continue
            // Ensure there's enough space for text to render without negative constraints
            val availableWidth = (size.width - textLeft).coerceAtLeast(0f)
            val availableHeight = (size.height - textTop).coerceAtLeast(0f)
            if (availableWidth < 10f || availableHeight < 10f) continue

            drawRoundRect(
                color = labelBg,
                topLeft = Offset(bgLeft, bgTop),
                size = Size(safeBgWidth, safeBgHeight),
                cornerRadius = CornerRadius(4f, 4f)
            )

            drawText(
                textMeasurer = textMeasurer,
                text = segment.reading,
                topLeft = Offset(textLeft, textTop),
                style = furiganaStyle
            )
        }
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
    labelBg: Color,
    isVerticalMode: Boolean = false
) {
    val elemLeft = bounds.left * scale - cropOffsetX
    val elemTop = bounds.top * scale - cropOffsetY
    val elemWidth = bounds.width() * scale
    val elemHeight = bounds.height() * scale

    // Quick bounds check
    if (elemWidth <= 0 || elemHeight <= 0) return
    if (elemLeft + elemWidth < -50 || elemLeft > size.width + 50) return  // Off-screen horizontally
    if (elemTop + elemHeight < -50 || elemTop > size.height + 50) return  // Off-screen vertically

    if (isVerticalMode) {
        // Vertical mode: furigana drawn vertically to the RIGHT
        drawVerticalFurigana(
            reading = reading,
            anchorLeft = elemLeft + elemWidth + 2f,
            anchorTop = elemTop,
            anchorHeight = elemHeight,
            textMeasurer = textMeasurer,
            furiganaStyle = furiganaStyle,
            labelBg = labelBg
        )
        return
    }

    // Horizontal mode: furigana centered ABOVE element
    val measured = textMeasurer.measure(reading, furiganaStyle)
    val furiganaWidth = measured.size.width.toFloat()
    val furiganaHeight = measured.size.height.toFloat()

    val padH = 6f
    val padV = 3f
    val bgWidth = furiganaWidth + padH * 2
    val bgHeight = furiganaHeight + padV * 2
    val bgLeft = elemLeft + (elemWidth - bgWidth) / 2f
    val bgTop = (elemTop - bgHeight - 2f).coerceAtLeast(0f)

    // Quick validation and coercion
    if (bgWidth <= 0 || bgHeight <= 0 || bgLeft.isNaN() || bgTop.isNaN()) return
    if (bgLeft + bgWidth < -50 || bgTop > size.height + 50) return  // Off-screen

    val safeBgWidth = bgWidth.coerceAtLeast(0.1f)
    val safeBgHeight = bgHeight.coerceAtLeast(0.1f)

    // Skip text if it would create invalid constraints
    val textLeft = bgLeft + padH
    val textTop = bgTop + padV
    if (textLeft < 0 || textTop < 0) return
    if (textLeft >= size.width || textTop >= size.height) return
    // Ensure there's enough space for text to render without negative constraints
    val availableWidth = (size.width - textLeft).coerceAtLeast(0f)
    val availableHeight = (size.height - textTop).coerceAtLeast(0f)
    if (availableWidth < 10f || availableHeight < 10f) return

    // Background pill
    drawRoundRect(
        color = labelBg,
        topLeft = Offset(bgLeft, bgTop),
        size = Size(safeBgWidth, safeBgHeight),
        cornerRadius = CornerRadius(4f, 4f)
    )

    // Furigana text
    drawText(
        textMeasurer = textMeasurer,
        text = reading,
        topLeft = Offset(textLeft, textTop),
        style = furiganaStyle
    )
}

/**
 * Draw furigana text vertically: each character stacked top-to-bottom in a tall narrow pill,
 * positioned to the right of the kanji and vertically centered on the anchor region.
 */
private fun DrawScope.drawVerticalFurigana(
    reading: String,
    anchorLeft: Float,
    anchorTop: Float,
    anchorHeight: Float,
    textMeasurer: TextMeasurer,
    furiganaStyle: TextStyle,
    labelBg: Color
) {
    if (reading.isEmpty()) return

    // Measure a single character to get consistent dimensions
    val sampleMeasured = textMeasurer.measure("あ", furiganaStyle)
    val charW = sampleMeasured.size.width.toFloat()
    val charH = sampleMeasured.size.height.toFloat()

    val padH = 4f
    val padV = 3f
    val charCount = reading.length
    val bgWidth = charW + padH * 2
    val bgHeight = charH * charCount + padV * 2

    val bgLeft = anchorLeft
    val bgTop = (anchorTop + (anchorHeight - bgHeight) / 2f).coerceIn(0f, (size.height - bgHeight).coerceAtLeast(0f))

    // Quick validation — skip if off-screen
    if (bgWidth <= 0 || bgHeight <= 0 || bgLeft.isNaN() || bgTop.isNaN()) return
    if (bgLeft + bgWidth < -50 || bgLeft > size.width + 50) return  // Off-screen horizontally
    if (bgTop + bgHeight < -50 || bgTop > size.height + 50) return  // Off-screen vertically

    val textLeft = bgLeft + padH
    val textTopStart = bgTop + padV
    if (textLeft < 0 || textTopStart < 0) return
    if (textLeft >= size.width || textTopStart >= size.height) return
    val availableWidth = (size.width - textLeft).coerceAtLeast(0f)
    val availableHeight = (size.height - textTopStart).coerceAtLeast(0f)
    if (availableWidth < 10f || availableHeight < 10f) return

    // Background pill
    drawRoundRect(
        color = labelBg,
        topLeft = Offset(bgLeft, bgTop),
        size = Size(bgWidth.coerceAtLeast(0.1f), bgHeight.coerceAtLeast(0.1f)),
        cornerRadius = CornerRadius(4f, 4f)
    )

    // Draw each character stacked vertically
    for (i in reading.indices) {
        val charTop = textTopStart + i * charH
        if (charTop + charH > size.height) break

        // Measure individual char for proper centering (varying widths)
        val charMeasured = textMeasurer.measure(reading[i].toString(), furiganaStyle)
        val actualCharW = charMeasured.size.width.toFloat()
        val charLeft = textLeft + (charW - actualCharW) / 2f  // Center within column

        drawText(
            textMeasurer = textMeasurer,
            text = reading[i].toString(),
            topLeft = Offset(charLeft, charTop),
            style = furiganaStyle
        )
    }
}
