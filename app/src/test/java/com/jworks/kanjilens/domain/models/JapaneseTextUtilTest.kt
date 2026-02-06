package com.jworks.kanjilens.domain.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JapaneseTextUtilTest {

    // --- containsKanji ---

    @Test
    fun `containsKanji returns true for CJK unified ideographs`() {
        assertTrue(JapaneseTextUtil.containsKanji("漢字"))
        assertTrue(JapaneseTextUtil.containsKanji("東京タワー"))
        assertTrue(JapaneseTextUtil.containsKanji("abc日def"))
    }

    @Test
    fun `containsKanji returns false for kana only`() {
        assertFalse(JapaneseTextUtil.containsKanji("ひらがな"))
        assertFalse(JapaneseTextUtil.containsKanji("カタカナ"))
    }

    @Test
    fun `containsKanji returns false for ASCII`() {
        assertFalse(JapaneseTextUtil.containsKanji("hello world"))
        assertFalse(JapaneseTextUtil.containsKanji("12345"))
        assertFalse(JapaneseTextUtil.containsKanji(""))
    }

    // --- containsJapanese ---

    @Test
    fun `containsJapanese returns true for hiragana`() {
        assertTrue(JapaneseTextUtil.containsJapanese("あいうえお"))
    }

    @Test
    fun `containsJapanese returns true for katakana`() {
        assertTrue(JapaneseTextUtil.containsJapanese("アイウエオ"))
    }

    @Test
    fun `containsJapanese returns true for kanji`() {
        assertTrue(JapaneseTextUtil.containsJapanese("漢字"))
    }

    @Test
    fun `containsJapanese returns true for mixed text`() {
        assertTrue(JapaneseTextUtil.containsJapanese("Hello こんにちは"))
    }

    @Test
    fun `containsJapanese returns false for ASCII only`() {
        assertFalse(JapaneseTextUtil.containsJapanese("hello"))
        assertFalse(JapaneseTextUtil.containsJapanese(""))
    }

    // --- containsHiragana ---

    @Test
    fun `containsHiragana detects hiragana`() {
        assertTrue(JapaneseTextUtil.containsHiragana("こんにちは"))
        assertFalse(JapaneseTextUtil.containsHiragana("カタカナ"))
        assertFalse(JapaneseTextUtil.containsHiragana("漢字"))
    }

    // --- containsKatakana ---

    @Test
    fun `containsKatakana detects katakana`() {
        assertTrue(JapaneseTextUtil.containsKatakana("カタカナ"))
        assertFalse(JapaneseTextUtil.containsKatakana("ひらがな"))
        assertFalse(JapaneseTextUtil.containsKatakana("漢字"))
    }

    // --- kanjiCount ---

    @Test
    fun `kanjiCount returns correct count`() {
        assertEquals(2, JapaneseTextUtil.kanjiCount("漢字"))
        assertEquals(2, JapaneseTextUtil.kanjiCount("東京タワー"))
        assertEquals(0, JapaneseTextUtil.kanjiCount("ひらがな"))
        assertEquals(0, JapaneseTextUtil.kanjiCount(""))
    }

    // --- japaneseRatio ---

    @Test
    fun `japaneseRatio returns 1 for all-Japanese text`() {
        assertEquals(1.0f, JapaneseTextUtil.japaneseRatio("あいう"), 0.01f)
        assertEquals(1.0f, JapaneseTextUtil.japaneseRatio("漢字"), 0.01f)
    }

    @Test
    fun `japaneseRatio returns 0 for non-Japanese text`() {
        assertEquals(0.0f, JapaneseTextUtil.japaneseRatio("hello"), 0.01f)
        assertEquals(0.0f, JapaneseTextUtil.japaneseRatio("12345"), 0.01f)
    }

    @Test
    fun `japaneseRatio returns 0 for empty string`() {
        assertEquals(0.0f, JapaneseTextUtil.japaneseRatio(""), 0.01f)
    }

    @Test
    fun `japaneseRatio returns correct ratio for mixed text`() {
        // "abc漢字" = 5 chars, 2 Japanese
        assertEquals(0.4f, JapaneseTextUtil.japaneseRatio("abc漢字"), 0.01f)
    }

    @Test
    fun `japaneseRatio below threshold filters URLs and numbers`() {
        // Simulates the MIN_JAPANESE_RATIO = 0.3 filter in ProcessCameraFrameUseCase
        val url = "https://example.com/日本"
        assertTrue(JapaneseTextUtil.japaneseRatio(url) < 0.3f)
    }
}
