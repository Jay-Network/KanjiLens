package com.jworks.kanjilens.ui.camera

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests the coordinate mapping math used in OverlayCanvas.
 * Extracted as pure functions to avoid Compose/Canvas dependencies.
 */
class CoordinateMappingTest {

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
}
