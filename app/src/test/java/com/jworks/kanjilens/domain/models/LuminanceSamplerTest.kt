package com.jworks.kanjilens.domain.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class LuminanceSamplerTest {

    @Test
    fun `mapToBufferCoords rotation 0 is identity`() {
        val result = LuminanceSampler.mapToBufferCoords(100, 200, 640, 480, 0)
        assertNotNull(result)
        assertEquals(Pair(100, 200), result)
    }

    @Test
    fun `mapToBufferCoords rotation 90`() {
        // ML Kit (100, 200) at rotation 90: bufX = mlY, bufY = origH - 1 - mlX
        val result = LuminanceSampler.mapToBufferCoords(100, 200, 640, 480, 90)
        assertNotNull(result)
        assertEquals(Pair(200, 480 - 1 - 100), result)
        assertEquals(Pair(200, 379), result)
    }

    @Test
    fun `mapToBufferCoords rotation 180`() {
        // bufX = origW - 1 - mlX, bufY = origH - 1 - mlY
        val result = LuminanceSampler.mapToBufferCoords(100, 200, 640, 480, 180)
        assertNotNull(result)
        assertEquals(Pair(640 - 1 - 100, 480 - 1 - 200), result)
        assertEquals(Pair(539, 279), result)
    }

    @Test
    fun `mapToBufferCoords rotation 270`() {
        // bufX = origW - 1 - mlY, bufY = mlX
        val result = LuminanceSampler.mapToBufferCoords(100, 200, 640, 480, 270)
        assertNotNull(result)
        assertEquals(Pair(640 - 1 - 200, 100), result)
        assertEquals(Pair(439, 100), result)
    }

    @Test
    fun `mapToBufferCoords returns null for unknown rotation`() {
        assertNull(LuminanceSampler.mapToBufferCoords(100, 200, 640, 480, 45))
    }

    @Test
    fun `mapToBufferCoords origin corner rotation 0`() {
        val result = LuminanceSampler.mapToBufferCoords(0, 0, 640, 480, 0)
        assertEquals(Pair(0, 0), result)
    }

    @Test
    fun `mapToBufferCoords max corner rotation 0`() {
        val result = LuminanceSampler.mapToBufferCoords(639, 479, 640, 480, 0)
        assertEquals(Pair(639, 479), result)
    }

    @Test
    fun `mapToBufferCoords origin rotation 90 maps to bottom-left of sensor`() {
        // ML Kit (0,0) at 90 deg rotation -> sensor (0, origH-1) = (0, 479)
        val result = LuminanceSampler.mapToBufferCoords(0, 0, 640, 480, 90)
        assertEquals(Pair(0, 479), result)
    }

    // sampleLuminance tests require android.graphics.Rect which is unavailable
    // in JVM unit tests. These should be run as androidTest (instrumented) tests.
}
