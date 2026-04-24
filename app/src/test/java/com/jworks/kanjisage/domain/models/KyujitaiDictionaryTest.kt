package com.jworks.kanjisage.domain.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KyujitaiDictionaryTest {

    @Test
    fun `getShinjitai returns modern form for known kyujitai`() {
        assertEquals("学", KyujitaiDictionary.getShinjitai("學"))
        assertEquals("国", KyujitaiDictionary.getShinjitai("國"))
        assertEquals("読", KyujitaiDictionary.getShinjitai("讀"))
    }

    @Test
    fun `getShinjitai returns null for shinjitai input`() {
        assertNull(KyujitaiDictionary.getShinjitai("学"))
        assertNull(KyujitaiDictionary.getShinjitai("国"))
    }

    @Test
    fun `getShinjitai returns null for unknown kanji`() {
        assertNull(KyujitaiDictionary.getShinjitai("猫"))
        assertNull(KyujitaiDictionary.getShinjitai("A"))
    }

    @Test
    fun `getKyujitai returns traditional forms for known shinjitai`() {
        val variants = KyujitaiDictionary.getKyujitai("学")
        assertTrue(variants.contains("學"))
    }

    @Test
    fun `getKyujitai returns multiple variants for ben`() {
        val variants = KyujitaiDictionary.getKyujitai("弁")
        assertTrue(variants.size >= 3)
        assertTrue(variants.contains("辯"))
        assertTrue(variants.contains("辨"))
        assertTrue(variants.contains("辮"))
    }

    @Test
    fun `getKyujitai returns empty list for unknown kanji`() {
        assertEquals(emptyList<String>(), KyujitaiDictionary.getKyujitai("猫"))
    }

    @Test
    fun `getKyujitai returns empty list for kyujitai input`() {
        assertEquals(emptyList<String>(), KyujitaiDictionary.getKyujitai("學"))
    }

    @Test
    fun `isKyujitai returns true for traditional forms`() {
        assertTrue(KyujitaiDictionary.isKyujitai("學"))
        assertTrue(KyujitaiDictionary.isKyujitai("國"))
        assertTrue(KyujitaiDictionary.isKyujitai("龍"))
    }

    @Test
    fun `isKyujitai returns false for modern forms`() {
        assertFalse(KyujitaiDictionary.isKyujitai("学"))
        assertFalse(KyujitaiDictionary.isKyujitai("国"))
    }

    @Test
    fun `isShinjitaiWithVariant returns true for modern forms that have kyujitai`() {
        assertTrue(KyujitaiDictionary.isShinjitaiWithVariant("学"))
        assertTrue(KyujitaiDictionary.isShinjitaiWithVariant("弁"))
    }

    @Test
    fun `isShinjitaiWithVariant returns false for kanji without variants`() {
        assertFalse(KyujitaiDictionary.isShinjitaiWithVariant("猫"))
    }

    @Test
    fun `hasVariant returns true for both kyujitai and shinjitai with variants`() {
        assertTrue(KyujitaiDictionary.hasVariant("學"))
        assertTrue(KyujitaiDictionary.hasVariant("学"))
    }

    @Test
    fun `hasVariant returns false for kanji with no variant relationship`() {
        assertFalse(KyujitaiDictionary.hasVariant("猫"))
        assertFalse(KyujitaiDictionary.hasVariant("山"))
    }

    @Test
    fun `roundtrip kyujitai to shinjitai and back`() {
        val shinjitai = KyujitaiDictionary.getShinjitai("學")
        assertEquals("学", shinjitai)
        val kyujitaiList = KyujitaiDictionary.getKyujitai(shinjitai!!)
        assertTrue(kyujitaiList.contains("學"))
    }
}
