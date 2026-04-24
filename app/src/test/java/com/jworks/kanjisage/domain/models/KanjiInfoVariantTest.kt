package com.jworks.kanjisage.domain.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KanjiInfoVariantTest {

    private fun kanjiInfo(literal: String) = KanjiInfo(
        literal = literal,
        grade = null,
        strokeCount = 1,
        frequency = null,
        jlpt = null,
        onReadings = emptyList(),
        kunReadings = emptyList(),
        meanings = emptyList()
    )

    @Test
    fun `shinjitai kanji reports kyujitai variants`() {
        val info = kanjiInfo("学")
        assertTrue(info.kyujitaiVariants.contains("學"))
        assertNull(info.shinjitaiVariant)
        assertFalse(info.isKyujitai)
        assertTrue(info.hasVariant)
    }

    @Test
    fun `kyujitai kanji reports shinjitai variant`() {
        val info = kanjiInfo("學")
        assertEquals("学", info.shinjitaiVariant)
        assertTrue(info.isKyujitai)
        assertTrue(info.hasVariant)
    }

    @Test
    fun `kanji without variants reports no variant`() {
        val info = kanjiInfo("猫")
        assertTrue(info.kyujitaiVariants.isEmpty())
        assertNull(info.shinjitaiVariant)
        assertFalse(info.isKyujitai)
        assertFalse(info.hasVariant)
    }

    @Test
    fun `ben shinjitai has multiple kyujitai variants`() {
        val info = kanjiInfo("弁")
        assertTrue(info.kyujitaiVariants.size >= 3)
        assertTrue(info.hasVariant)
    }
}
