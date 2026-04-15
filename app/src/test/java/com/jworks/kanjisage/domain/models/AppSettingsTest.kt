package com.jworks.kanjisage.domain.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSettingsTest {

    @Test
    fun `default settings have correct kanji color`() {
        val settings = AppSettings()
        assertEquals(0xFF4CAF50, settings.kanjiColor)
    }

    @Test
    fun `default settings have correct kana color`() {
        val settings = AppSettings()
        assertEquals(0xFF2196F3, settings.kanaColor)
    }

    @Test
    fun `default stroke width is 2`() {
        val settings = AppSettings()
        assertEquals(2f, settings.strokeWidth, 0.001f)
    }

    @Test
    fun `default label font size is 14`() {
        val settings = AppSettings()
        assertEquals(14f, settings.labelFontSize, 0.001f)
    }

    @Test
    fun `default frame skip is 1`() {
        val settings = AppSettings()
        assertEquals(1, settings.frameSkip)
    }

    @Test
    fun `debug HUD is off by default`() {
        val settings = AppSettings()
        assertFalse(settings.showDebugHud)
    }

    @Test
    fun `bounding boxes are hidden by default`() {
        val settings = AppSettings()
        assertFalse(settings.showBoxes)
    }

    @Test
    fun `furigana is bold by default`() {
        val settings = AppSettings()
        assertTrue(settings.furiganaIsBold)
    }

    @Test
    fun `furigana uses white text by default`() {
        val settings = AppSettings()
        assertTrue(settings.furiganaUseWhiteText)
    }

    @Test
    fun `default boundary ratio is full screen`() {
        val settings = AppSettings()
        assertEquals(1.0f, settings.partialModeBoundaryRatio, 0.001f)
    }

    @Test
    fun `vertical text mode is off by default`() {
        val settings = AppSettings()
        assertFalse(settings.verticalTextMode)
    }

    @Test
    fun `adaptive color is on by default`() {
        val settings = AppSettings()
        assertTrue(settings.furiganaAdaptiveColor)
    }

    @Test
    fun `AI enhance is on by default`() {
        val settings = AppSettings()
        assertTrue(settings.aiEnhanceEnabled)
    }

    @Test
    fun `token counters start at zero`() {
        val settings = AppSettings()
        assertEquals(0L, settings.geminiInputTokens)
        assertEquals(0L, settings.geminiOutputTokens)
        assertEquals(0L, settings.claudeInputTokens)
        assertEquals(0L, settings.claudeOutputTokens)
    }

    @Test
    fun `copy preserves modified fields`() {
        val settings = AppSettings(
            labelFontSize = 20f,
            verticalTextMode = true,
            showDebugHud = true
        )
        assertEquals(20f, settings.labelFontSize, 0.001f)
        assertTrue(settings.verticalTextMode)
        assertTrue(settings.showDebugHud)
        // Unmodified fields retain defaults
        assertEquals(2f, settings.strokeWidth, 0.001f)
        assertFalse(settings.showBoxes)
    }
}
