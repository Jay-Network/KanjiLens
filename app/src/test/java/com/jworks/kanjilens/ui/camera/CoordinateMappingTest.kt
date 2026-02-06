package com.jworks.kanjilens.ui.camera

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests the coordinate mapping math used in OverlayCanvas.
 * Extracted as pure functions to avoid Compose/Canvas dependencies.
 */
class CoordinateMappingTest {

    // Simple Rect for testing (Android Rect not properly mocked in unit tests)
    data class TestRect(val left: Int, val top: Int, val right: Int, val bottom: Int) {
        fun width() = right - left
        fun height() = bottom - top
    }

    // Mirrors the logic in TextOverlay Canvas block
    private fun computeScale(
        canvasWidth: Float,
        canvasHeight: Float,
        imageWidth: Int,
        imageHeight: Int,
        rotationDegrees: Int
    ): Pair<Float, Float> {
        val isRotated = rotationDegrees == 90 || rotationDegrees == 270
        val effectiveWidth = if (isRotated) imageHeight else imageWidth
        val effectiveHeight = if (isRotated) imageWidth else imageHeight
        return Pair(
            canvasWidth / effectiveWidth,
            canvasHeight / effectiveHeight
        )
    }

    // Mirrors the FILL_CENTER logic in OverlayCanvas
    private fun mapImageToScreen(
        imageBounds: TestRect,
        imageWidth: Int,
        imageHeight: Int,
        canvasWidth: Float,
        canvasHeight: Float,
        rotationDegrees: Int
    ): TestRect {
        val isRotated = rotationDegrees == 90 || rotationDegrees == 270
        val effectiveWidth = (if (isRotated) imageHeight else imageWidth).toFloat()
        val effectiveHeight = (if (isRotated) imageWidth else imageHeight).toFloat()

        // FILL_CENTER: uniform scale to fill canvas
        val scale = maxOf(canvasWidth / effectiveWidth, canvasHeight / effectiveHeight)

        // Calculate visible crop region in IMAGE coordinates
        val scaledImageWidth = effectiveWidth * scale
        val scaledImageHeight = effectiveHeight * scale

        // Crop offset: how much of the scaled image is cut off on each side
        val cropOffsetX = (scaledImageWidth - canvasWidth) / 2f
        val cropOffsetY = (scaledImageHeight - canvasHeight) / 2f

        // Transform image coordinates to screen coordinates
        return TestRect(
            (imageBounds.left * scale - cropOffsetX).toInt(),
            (imageBounds.top * scale - cropOffsetY).toInt(),
            (imageBounds.right * scale - cropOffsetX).toInt(),
            (imageBounds.bottom * scale - cropOffsetY).toInt()
        )
    }

    @Test
    fun `no rotation maps directly`() {
        // Image 1280x720, canvas 1080x1920 (portrait phone)
        val (scaleX, scaleY) = computeScale(1080f, 1920f, 1280, 720, 0)
        assertEquals(1080f / 1280f, scaleX, 0.001f)
        assertEquals(1920f / 720f, scaleY, 0.001f)
    }

    @Test
    fun `90 degree rotation swaps image dimensions`() {
        // Camera sensor is landscape 1280x720, but rotated 90 degrees
        // effective: width=720, height=1280
        val (scaleX, scaleY) = computeScale(1080f, 1920f, 1280, 720, 90)
        assertEquals(1080f / 720f, scaleX, 0.001f)
        assertEquals(1920f / 1280f, scaleY, 0.001f)
    }

    @Test
    fun `270 degree rotation also swaps`() {
        val (scaleX, scaleY) = computeScale(1080f, 1920f, 1280, 720, 270)
        assertEquals(1080f / 720f, scaleX, 0.001f)
        assertEquals(1920f / 1280f, scaleY, 0.001f)
    }

    @Test
    fun `180 degree rotation does not swap`() {
        val (scaleX, scaleY) = computeScale(1080f, 1920f, 1280, 720, 180)
        assertEquals(1080f / 1280f, scaleX, 0.001f)
        assertEquals(1920f / 720f, scaleY, 0.001f)
    }

    @Test
    fun `bounding box scales correctly`() {
        // Image 640x480 on canvas 320x240 (half size)
        val (scaleX, scaleY) = computeScale(320f, 240f, 640, 480, 0)
        // A box at (100, 50) with width 200, height 100 in image coords
        val mappedLeft = 100 * scaleX
        val mappedTop = 50 * scaleY
        val mappedWidth = 200 * scaleX
        val mappedHeight = 100 * scaleY
        assertEquals(50f, mappedLeft, 0.001f)
        assertEquals(25f, mappedTop, 0.001f)
        assertEquals(100f, mappedWidth, 0.001f)
        assertEquals(50f, mappedHeight, 0.001f)
    }

    @Test
    fun `1-to-1 scale when canvas matches image`() {
        val (scaleX, scaleY) = computeScale(1280f, 720f, 1280, 720, 0)
        assertEquals(1.0f, scaleX, 0.001f)
        assertEquals(1.0f, scaleY, 0.001f)
    }

    // ========== Z Flip 7 Square Sensor Tests ==========

    @Test
    fun `Z Flip 7 - square sensor FILL_CENTER on portrait screen`() {
        // Z Flip 7 scenario: square sensor, portrait screen
        val imageWidth = 2992
        val imageHeight = 2992
        val canvasWidth = 1080f
        val canvasHeight = 2340f
        val rotation = 0

        // Image bounds: kanji at center of frame (200x200 px box)
        val imageBounds = TestRect(1396, 1396, 1596, 1596)

        // Expected: box should appear centered on screen
        val screenBounds = mapImageToScreen(
            imageBounds, imageWidth, imageHeight,
            canvasWidth, canvasHeight, rotation
        )

        // Debug without using toString()
        println("Screen bounds: left=${screenBounds.left} top=${screenBounds.top} right=${screenBounds.right} bottom=${screenBounds.bottom}")
        println("Canvas: ${canvasWidth}x${canvasHeight}")

        // Verify it's centered (within tolerance)
        val centerX = (screenBounds.left + screenBounds.right) / 2f
        val centerY = (screenBounds.top + screenBounds.bottom) / 2f

        println("Center X: $centerX, expected: ${canvasWidth/2}")
        println("Center Y: $centerY, expected: ${canvasHeight/2}")

        assertEquals(canvasWidth / 2, centerX, 20f)  // Allow 20px tolerance
        assertEquals(canvasHeight / 2, centerY, 20f)
    }

    @Test
    fun `Z Flip 7 - square sensor 90 degree rotation`() {
        // Z Flip 7 scenario: square sensor, landscape orientation
        val imageWidth = 2992
        val imageHeight = 2992
        val canvasWidth = 2340f
        val canvasHeight = 1080f
        val rotation = 90

        // Same center position as above
        val imageBounds = TestRect(1396, 1396, 1596, 1596)

        val screenBounds = mapImageToScreen(
            imageBounds, imageWidth, imageHeight,
            canvasWidth, canvasHeight, rotation
        )

        // Verify it's still centered after rotation
        val centerX = (screenBounds.left + screenBounds.right) / 2f
        val centerY = (screenBounds.top + screenBounds.bottom) / 2f

        assertEquals(canvasWidth / 2, centerX, 20f)
        assertEquals(canvasHeight / 2, centerY, 20f)
    }

    @Test
    fun `Z Flip 7 - top-left VISIBLE region`() {
        // Kanji in top-left VISIBLE portion of screen after FILL_CENTER crop
        // Z Flip 7: 2992x2992 square sensor on 1080x2340 portrait screen
        // Scale = 2340/2992 = 0.782
        // Visible X range in image coords: [805, 2187]
        val imageWidth = 2992
        val imageHeight = 2992
        val canvasWidth = 1080f
        val canvasHeight = 2340f
        val rotation = 0

        // Top-left corner of VISIBLE region
        val imageBounds = TestRect(820, 100, 920, 200)

        val screenBounds = mapImageToScreen(
            imageBounds, imageWidth, imageHeight,
            canvasWidth, canvasHeight, rotation
        )

        // Should be in top-left region of screen
        assertTrue("Left should be >= 0, was ${screenBounds.left}", screenBounds.left >= 0)
        assertTrue("Top should be >= 0", screenBounds.top >= 0)
        assertTrue("Left should be in left half", screenBounds.left < canvasWidth / 2)

        // Should be in upper portion
        val centerY = (screenBounds.top + screenBounds.bottom) / 2f
        assertTrue("Should be in top half", centerY < canvasHeight / 2)
    }

    @Test
    fun `Z Flip 7 - bottom-right VISIBLE region`() {
        // Kanji in bottom-right VISIBLE portion after FILL_CENTER crop
        // Visible X range: [805, 2187]
        val imageWidth = 2992
        val imageHeight = 2992
        val canvasWidth = 1080f
        val canvasHeight = 2340f
        val rotation = 0

        // Bottom-right corner of VISIBLE region
        val imageBounds = TestRect(2072, 2792, 2172, 2892)

        val screenBounds = mapImageToScreen(
            imageBounds, imageWidth, imageHeight,
            canvasWidth, canvasHeight, rotation
        )

        // Should be in bottom-right region
        val centerX = (screenBounds.left + screenBounds.right) / 2f
        val centerY = (screenBounds.top + screenBounds.bottom) / 2f
        assertTrue("Should be in right half", centerX > canvasWidth / 2)
        assertTrue("Should be in bottom half", centerY > canvasHeight / 2)

        // Should not overflow canvas
        assertTrue("Right should be <= canvas width", screenBounds.right <= canvasWidth.toInt() + 10)
        assertTrue("Bottom should be <= canvas height", screenBounds.bottom <= canvasHeight.toInt() + 10)
    }

    // ========== Vertical Text Mode Tests ==========

    /**
     * Mirrors the vertical segment positioning logic in OverlayCanvas.drawKanjiSegments
     * when isVerticalMode=true: charHeight = elemHeight / textLength,
     * segTop = elemTop + startIndex * charHeight
     */
    private data class VerticalSegmentResult(
        val segTop: Float,
        val segHeight: Float,
        val furiganaBgLeft: Float,
        val furiganaBgTop: Float
    )

    private fun computeVerticalSegment(
        elemLeft: Float,
        elemTop: Float,
        elemWidth: Float,
        elemHeight: Float,
        textLength: Int,
        startIndex: Int,
        endIndex: Int,
        furiganaWidth: Float,
        furiganaHeight: Float
    ): VerticalSegmentResult {
        val charHeight = elemHeight / textLength.toFloat()
        val segTop = elemTop + startIndex * charHeight
        val segHeight = (endIndex - startIndex) * charHeight

        val padH = 6f
        val padV = 3f
        val bgWidth = furiganaWidth + padH * 2
        val bgHeight = furiganaHeight + padV * 2
        val bgLeft = elemLeft + elemWidth + 2f
        val bgTop = (segTop + (segHeight - bgHeight) / 2f).coerceAtLeast(0f)

        return VerticalSegmentResult(segTop, segHeight, bgLeft, bgTop)
    }

    @Test
    fun `vertical mode - charHeight divides element evenly`() {
        // Element with 4 characters, 400px tall
        val elemHeight = 400f
        val textLength = 4
        val charHeight = elemHeight / textLength
        assertEquals(100f, charHeight, 0.001f)
    }

    @Test
    fun `vertical mode - segment top positioned correctly`() {
        // 4-character element at y=100, segment starts at index 2
        val result = computeVerticalSegment(
            elemLeft = 50f, elemTop = 100f, elemWidth = 80f, elemHeight = 400f,
            textLength = 4, startIndex = 2, endIndex = 4,
            furiganaWidth = 40f, furiganaHeight = 16f
        )
        // segTop = 100 + 2 * 100 = 300
        assertEquals(300f, result.segTop, 0.001f)
        // segHeight = (4-2) * 100 = 200
        assertEquals(200f, result.segHeight, 0.001f)
    }

    @Test
    fun `vertical mode - furigana placed to the right of element`() {
        val result = computeVerticalSegment(
            elemLeft = 50f, elemTop = 100f, elemWidth = 80f, elemHeight = 400f,
            textLength = 4, startIndex = 0, endIndex = 2,
            furiganaWidth = 40f, furiganaHeight = 16f
        )
        // bgLeft = elemLeft + elemWidth + 2 = 50 + 80 + 2 = 132
        assertEquals(132f, result.furiganaBgLeft, 0.001f)
    }

    @Test
    fun `vertical mode - furigana vertically centered on segment`() {
        val result = computeVerticalSegment(
            elemLeft = 50f, elemTop = 0f, elemWidth = 80f, elemHeight = 400f,
            textLength = 4, startIndex = 0, endIndex = 2,
            furiganaWidth = 40f, furiganaHeight = 16f
        )
        // segTop = 0, segHeight = 200
        // bgHeight = 16 + 3*2 = 22
        // bgTop = 0 + (200 - 22) / 2 = 89
        assertEquals(89f, result.furiganaBgTop, 0.001f)
    }

    @Test
    fun `vertical mode - furigana clamped to top of screen`() {
        // Segment at the very top, furigana centering would go negative
        val result = computeVerticalSegment(
            elemLeft = 50f, elemTop = 0f, elemWidth = 80f, elemHeight = 20f,
            textLength = 1, startIndex = 0, endIndex = 1,
            furiganaWidth = 40f, furiganaHeight = 30f
        )
        // segHeight = 20, bgHeight = 30 + 6 = 36
        // bgTop = (0 + (20 - 36) / 2) = -8 -> clamped to 0
        assertEquals(0f, result.furiganaBgTop, 0.001f)
    }

    @Test
    fun `FILL_CENTER crops correctly on narrow canvas`() {
        // Wide image, narrow canvas - should crop horizontally
        val imageWidth = 1920
        val imageHeight = 1080
        val canvasWidth = 800f
        val canvasHeight = 1200f
        val rotation = 0

        // Item at horizontal center
        val imageBounds = TestRect(860, 490, 1060, 590)  // Center of image

        val screenBounds = mapImageToScreen(
            imageBounds, imageWidth, imageHeight,
            canvasWidth, canvasHeight, rotation
        )

        // Should be centered horizontally on screen
        val centerX = (screenBounds.left + screenBounds.right) / 2f
        assertEquals(canvasWidth / 2, centerX, 20f)
    }
}
