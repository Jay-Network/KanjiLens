package com.jworks.kanjilens.ui.camera

import android.graphics.Rect
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.jworks.kanjilens.domain.models.AppSettings
import com.jworks.kanjilens.domain.models.DetectedText
import com.jworks.kanjilens.domain.models.KanjiSegment
import com.jworks.kanjilens.domain.models.TextElement

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
    val outlineStroke = remember { Stroke(width = 3f, join = StrokeJoin.Round) }
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

        // Pre-measure sample char for vertical mode (avoids re-measuring "あ" per segment)
        val sampleCharSize = textMeasurer.measure("あ", furiganaStyle).size
        val cachedCharW = sampleCharSize.width.toFloat()
        val cachedCharH = sampleCharSize.height.toFloat()

        // Per-frame measurement cache (avoids re-measuring same reading string within one frame)
        val measureCache = HashMap<String, androidx.compose.ui.text.TextLayoutResult>()

        // Pre-pass: collect screen-space rects for neighbor-gap computation
        val elemScreenRects = mutableListOf<RectF>()
        for (detected in detectedTexts) {
            if (!detected.containsKanji) continue
            for (element in detected.elements) {
                if (!element.containsKanji) continue
                val bounds = element.bounds ?: continue
                if (bounds.isEmpty) continue
                elemScreenRects.add(RectF(
                    bounds.left * scale - cropOffsetX,
                    bounds.top * scale - cropOffsetY,
                    bounds.right * scale - cropOffsetX,
                    bounds.bottom * scale - cropOffsetY
                ))
            }
        }

        // Canvas-level clip for partial modes: prevents ANY rendering below boundary
        // This is the definitive fix — furigana drawn above text that's below the boundary
        // gets clipped even if per-element checks miss edge cases
        if (isPartial && clipBottomEdge > 0f) {
            val clipLeft = if (isVerticalMode) {
                size.width * (1f - PartialModeConstants.VERT_CAMERA_WIDTH_RATIO)
            } else 0f
            drawContext.canvas.nativeCanvas.save()
            drawContext.canvas.nativeCanvas.clipRect(clipLeft, 0f, size.width, clipBottomEdge)
        }

        // Default fill/stroke when adaptive color is off
        val defaultFill = if (settings.furiganaUseWhiteText) Color.White else Color.Black
        val defaultStroke = if (defaultFill == Color.White) Color.Black else Color.White

        // Only render kanji elements that have readings
        for (detected in detectedTexts) {
            if (!detected.containsKanji) continue

            for (element in detected.elements) {
                if (!element.containsKanji) continue
                val bounds = element.bounds ?: continue
                if (bounds.isEmpty) continue

                // Per-element adaptive color from background luminance
                val (elemFillColor, elemStrokeColor) = computeElementColors(
                    element, settings.furiganaAdaptiveColor, defaultFill, defaultStroke
                )

                if (element.kanjiSegments.isNotEmpty()) {
                    // Compute available inter-line gap for furigana sizing
                    val scrLeft = bounds.left * scale - cropOffsetX
                    val scrTop = bounds.top * scale - cropOffsetY
                    val scrRight = bounds.right * scale - cropOffsetX
                    val scrBottom = bounds.bottom * scale - cropOffsetY
                    val gapAbove = computeGapAbove(scrTop, scrLeft, scrRight, elemScreenRects)
                    val gapRight = computeGapRight(scrRight, scrTop, scrBottom, elemScreenRects)

                    // Per-segment rendering: individual boxes + furigana per kanji word
                    drawKanjiSegments(
                        bounds, element.text.length, element.kanjiSegments,
                        scale, cropOffsetX, cropOffsetY, kanjiColor, settings.strokeWidth,
                        textMeasurer, furiganaStyle, outlineStroke, settings.showBoxes,
                        isVerticalMode, clipLeftEdge, clipBottomEdge,
                        cachedCharW, cachedCharH, measureCache,
                        maxFuriganaHeight = gapAbove,
                        maxFuriganaWidth = gapRight,
                        fillColor = elemFillColor,
                        strokeColor = elemStrokeColor
                    )
                } else if (element.reading != null) {
                    // Fallback: element-level rendering
                    val elemLeft = bounds.left * scale - cropOffsetX
                    val elemTop = bounds.top * scale - cropOffsetY
                    val elemH = bounds.height() * scale
                    if (clipLeftEdge > 0f && elemLeft < clipLeftEdge) continue
                    if (clipBottomEdge > 0f && elemTop + elemH > clipBottomEdge - elemH * 0.3f) continue

                    if (settings.showBoxes) {
                        drawBoundingBox(bounds, scale, cropOffsetX, cropOffsetY, kanjiColor, settings.strokeWidth)
                    }
                    drawFuriganaLabel(
                        bounds, element.reading, scale, cropOffsetX, cropOffsetY,
                        kanjiColor, textMeasurer, furiganaStyle, outlineStroke,
                        isVerticalMode, cachedCharW, cachedCharH,
                        fillColor = elemFillColor,
                        strokeColor = elemStrokeColor
                    )
                }
            }
        }

        // Restore canvas state after partial mode clipping
        if (isPartial && clipBottomEdge > 0f) {
            drawContext.canvas.nativeCanvas.restore()
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
    outlineStroke: Stroke,
    showBoxes: Boolean,
    isVerticalMode: Boolean = false,
    clipLeftEdge: Float = 0f,
    clipBottomEdge: Float = 0f,
    cachedCharW: Float = -1f,
    cachedCharH: Float = -1f,
    measureCache: HashMap<String, androidx.compose.ui.text.TextLayoutResult>? = null,
    maxFuriganaHeight: Float = Float.MAX_VALUE,
    maxFuriganaWidth: Float = Float.MAX_VALUE,
    fillColor: Color = furiganaStyle.color,
    strokeColor: Color = if (furiganaStyle.color == Color.White) Color.Black else Color.White
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

        // Neighbor-aware: hide furigana if gap to right is too tight
        val availFuriganaWidth = (maxFuriganaWidth - 4f).coerceAtLeast(0f)
        val hideFurigana = availFuriganaWidth < cachedCharW || availFuriganaWidth < 6f

        // Overlap prevention: track bottom edge cursor for greedy top-to-bottom placement
        var bottomEdgeCursor = Float.NEGATIVE_INFINITY
        val furiganaVGap = 2f

        for (segment in segments) {
            val segTop = elemTop + segment.startIndex * charHeight
            val segHeight = (segment.endIndex - segment.startIndex) * charHeight

            // Quick validation
            if (segHeight <= 0 || segTop + segHeight < -50 || segTop > size.height + 50) continue

            // Per-segment clip: skip segments near or past the panel boundary
            // 30% margin prevents furigana for partially-visible kanji
            val vMargin = charHeight * 0.3f
            if (clipBottomEdge > 0f && segTop + segHeight > clipBottomEdge - vMargin) continue

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

            // Vertical outlined furigana to the RIGHT of this segment
            if (!hideFurigana) {
                // Greedy placement: ideal center, but shift down if it would overlap previous
                val charCount = segment.reading.length
                val totalFH = cachedCharH * charCount
                var furiganaTop = segTop + (segHeight - totalFH) / 2f
                if (furiganaTop < bottomEdgeCursor + furiganaVGap) {
                    furiganaTop = bottomEdgeCursor + furiganaVGap
                }
                bottomEdgeCursor = furiganaTop + totalFH

                drawVerticalFurigana(
                    reading = segment.reading,
                    anchorLeft = elemLeft + elemWidth + 2f,
                    anchorTop = segTop,
                    anchorHeight = segHeight,
                    textMeasurer = textMeasurer,
                    furiganaStyle = furiganaStyle,
                    outlineStroke = outlineStroke,
                    cachedCharW = cachedCharW,
                    cachedCharH = cachedCharH,
                    measureCache = measureCache,
                    fillColor = fillColor,
                    strokeColor = strokeColor,
                    overrideStartTop = furiganaTop
                )
            }
        }
    } else {
        // Horizontal mode: characters laid out left-to-right, furigana ABOVE
        // Skip element if kanji would be partially clipped by panel boundary.
        // Use 30% margin so half-visible kanji don't get floating furigana above them.
        val hMargin = elemHeight * 0.3f
        if (clipBottomEdge > 0f && elemTop + elemHeight > clipBottomEdge - hMargin) return

        val charWidth = elemWidth / textLength.toFloat()

        // Neighbor-aware: hide furigana if gap above is too tight
        val sampleH = measureCache?.get("\u3042")?.size?.height?.toFloat() ?: cachedCharH
        val availFuriganaHeight = (maxFuriganaHeight - 2f).coerceAtLeast(0f)
        val hideFurigana = availFuriganaHeight < sampleH || availFuriganaHeight < 6f

        // Phase 1: Measure + greedy left-to-right placement to prevent overlap
        data class FuriganaPlacement(
            val measured: androidx.compose.ui.text.TextLayoutResult,
            val textLeft: Float,
            val textTop: Float,
            val segLeft: Float,
            val segWidth: Float
        )
        val placements = mutableListOf<FuriganaPlacement>()
        var rightEdgeCursor = Float.NEGATIVE_INFINITY
        val furiganaGap = 2f

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

            // Skip furigana if lines are too dense
            if (hideFurigana) continue

            // Measure furigana text
            val measured = measureCache?.getOrPut(segment.reading) {
                textMeasurer.measure(segment.reading, furiganaStyle)
            } ?: textMeasurer.measure(segment.reading, furiganaStyle)
            val fw = measured.size.width.toFloat()
            val fh = measured.size.height.toFloat()

            // Greedy placement: ideal center, but shift right if it would overlap previous
            var textLeft = segLeft + (segWidth - fw) / 2f
            if (textLeft < rightEdgeCursor + furiganaGap) {
                textLeft = rightEdgeCursor + furiganaGap
            }
            val textTop = (elemTop - fh - 2f).coerceAtLeast(0f)
            rightEdgeCursor = textLeft + fw

            // Quick validation
            if (textLeft.isNaN() || textTop.isNaN()) continue
            if (textLeft + fw < -50 || textTop > size.height + 50) continue

            placements.add(FuriganaPlacement(measured, textLeft, textTop, segLeft, segWidth))
        }

        // Phase 2: Draw all furigana placements
        for (p in placements) {
            drawText(p.measured, color = strokeColor, topLeft = Offset(p.textLeft, p.textTop), drawStyle = outlineStroke)
            drawText(p.measured, color = fillColor, topLeft = Offset(p.textLeft, p.textTop))
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
    outlineStroke: Stroke,
    isVerticalMode: Boolean = false,
    cachedCharW: Float = -1f,
    cachedCharH: Float = -1f,
    fillColor: Color = furiganaStyle.color,
    strokeColor: Color = if (furiganaStyle.color == Color.White) Color.Black else Color.White
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
            outlineStroke = outlineStroke,
            cachedCharW = cachedCharW,
            cachedCharH = cachedCharH,
            fillColor = fillColor,
            strokeColor = strokeColor
        )
        return
    }

    // Horizontal mode: outlined furigana centered ABOVE element
    val measured = textMeasurer.measure(reading, furiganaStyle)
    val furiganaWidth = measured.size.width.toFloat()
    val furiganaHeight = measured.size.height.toFloat()

    val textLeft = elemLeft + (elemWidth - furiganaWidth) / 2f
    val textTop = (elemTop - furiganaHeight - 2f).coerceAtLeast(0f)

    // Quick validation
    if (textLeft.isNaN() || textTop.isNaN()) return
    if (textLeft + furiganaWidth < -50 || textTop > size.height + 50) return

    // Outlined furigana: stroke in contrast color, fill in adaptive/manual color
    drawText(measured, color = strokeColor, topLeft = Offset(textLeft, textTop), drawStyle = outlineStroke)
    drawText(measured, color = fillColor, topLeft = Offset(textLeft, textTop))
}

/**
 * Draw furigana text vertically: each character stacked top-to-bottom with outlined text,
 * positioned to the right of the kanji and vertically centered on the anchor region.
 */
private fun DrawScope.drawVerticalFurigana(
    reading: String,
    anchorLeft: Float,
    anchorTop: Float,
    anchorHeight: Float,
    textMeasurer: TextMeasurer,
    furiganaStyle: TextStyle,
    outlineStroke: Stroke,
    cachedCharW: Float = -1f,
    cachedCharH: Float = -1f,
    measureCache: HashMap<String, androidx.compose.ui.text.TextLayoutResult>? = null,
    fillColor: Color = furiganaStyle.color,
    strokeColor: Color = if (furiganaStyle.color == Color.White) Color.Black else Color.White,
    overrideStartTop: Float? = null
) {
    if (reading.isEmpty()) return

    // Use pre-computed dimensions if available, otherwise measure
    val charW: Float
    val charH: Float
    if (cachedCharW > 0f && cachedCharH > 0f) {
        charW = cachedCharW
        charH = cachedCharH
    } else {
        val sampleMeasured = textMeasurer.measure("あ", furiganaStyle)
        charW = sampleMeasured.size.width.toFloat()
        charH = sampleMeasured.size.height.toFloat()
    }

    val charCount = reading.length
    val totalHeight = charH * charCount

    // Quick validation — skip if off-screen
    if (anchorLeft.isNaN() || anchorTop.isNaN()) return
    if (anchorLeft > size.width + 50) return
    if (anchorTop + anchorHeight < -50 || anchorTop > size.height + 50) return

    // Draw each character stacked vertically — outlined text (stroke + fill)
    for (i in reading.indices) {
        val charTop = if (overrideStartTop != null) {
            overrideStartTop + i * charH
        } else {
            anchorTop + (anchorHeight - totalHeight) / 2f + i * charH
        }
        if (charTop + charH < 0) continue
        if (charTop > size.height) break

        val charLeft = anchorLeft + 2f  // Small gap from kanji

        // Measure individual char for proper centering (varying widths) — cached
        val charStr = reading[i].toString()
        val charMeasured = measureCache?.getOrPut(charStr) {
            textMeasurer.measure(charStr, furiganaStyle)
        } ?: textMeasurer.measure(charStr, furiganaStyle)
        val actualCharW = charMeasured.size.width.toFloat()
        val centeredLeft = charLeft + (charW - actualCharW) / 2f  // Center within column

        drawText(charMeasured, color = strokeColor, topLeft = Offset(centeredLeft, charTop), drawStyle = outlineStroke)
        drawText(charMeasured, color = fillColor, topLeft = Offset(centeredLeft, charTop))
    }
}

/** Compute per-element fill/stroke colors from background luminance or manual setting. */
private fun computeElementColors(
    element: TextElement,
    adaptiveEnabled: Boolean,
    defaultFill: Color,
    defaultStroke: Color
): Pair<Color, Color> {
    if (adaptiveEnabled && element.backgroundLuminance != null) {
        val lum = element.backgroundLuminance
        val inv = 255 - lum
        return Pair(
            Color(inv / 255f, inv / 255f, inv / 255f),
            Color(lum / 255f, lum / 255f, lum / 255f)
        )
    }
    return Pair(defaultFill, defaultStroke)
}

/** Compute gap above an element to its nearest horizontally-overlapping neighbor. */
private fun computeGapAbove(myTop: Float, myLeft: Float, myRight: Float, allRects: List<RectF>): Float {
    var nearestBottom = Float.NEGATIVE_INFINITY
    for (rect in allRects) {
        if (rect.bottom <= myTop && rect.bottom > nearestBottom
            && rect.right > myLeft && rect.left < myRight) {
            nearestBottom = rect.bottom
        }
    }
    return if (nearestBottom == Float.NEGATIVE_INFINITY) Float.MAX_VALUE else myTop - nearestBottom
}

/** Compute gap to the right of an element to its nearest vertically-overlapping neighbor. */
private fun computeGapRight(myRight: Float, myTop: Float, myBottom: Float, allRects: List<RectF>): Float {
    var nearestLeft = Float.POSITIVE_INFINITY
    for (rect in allRects) {
        if (rect.left >= myRight && rect.left < nearestLeft
            && rect.bottom > myTop && rect.top < myBottom) {
            nearestLeft = rect.left
        }
    }
    return if (nearestLeft == Float.POSITIVE_INFINITY) Float.MAX_VALUE else nearestLeft - myRight
}
