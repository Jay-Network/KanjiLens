package com.jworks.kanjisage.domain.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DictionaryResultTest {

    @Test
    fun `DictionaryResult holds word and reading`() {
        val result = DictionaryResult(
            word = "食べる",
            reading = "たべる",
            senses = emptyList(),
            isCommon = true
        )
        assertEquals("食べる", result.word)
        assertEquals("たべる", result.reading)
        assertTrue(result.isCommon)
    }

    @Test
    fun `DictionaryResult with senses`() {
        val senses = listOf(
            DictionarySense(
                partOfSpeech = listOf("Ichidan verb", "Transitive verb"),
                glosses = listOf("to eat", "to live on")
            ),
            DictionarySense(
                partOfSpeech = listOf("Ichidan verb"),
                glosses = listOf("to receive (a blow)")
            )
        )
        val result = DictionaryResult("食べる", "たべる", senses, true)
        assertEquals(2, result.senses.size)
        assertEquals(listOf("to eat", "to live on"), result.senses[0].glosses)
        assertEquals(listOf("Ichidan verb", "Transitive verb"), result.senses[0].partOfSpeech)
    }

    @Test
    fun `DictionarySense with empty glosses`() {
        val sense = DictionarySense(emptyList(), emptyList())
        assertTrue(sense.partOfSpeech.isEmpty())
        assertTrue(sense.glosses.isEmpty())
    }

    @Test
    fun `uncommon word marked correctly`() {
        val result = DictionaryResult("齟齬", "そご", emptyList(), isCommon = false)
        assertFalse(result.isCommon)
    }

    @Test
    fun `FuriganaResult holds frequency`() {
        val result = FuriganaResult("食べる", "たべる", frequency = 42)
        assertEquals("食べる", result.word)
        assertEquals("たべる", result.reading)
        assertEquals(42, result.frequency)
    }

    @Test
    fun `FuriganaResult frequency defaults to null`() {
        val result = FuriganaResult("日本", "にほん")
        assertEquals(null, result.frequency)
    }

    @Test
    fun `KanjiSegment tracks position`() {
        val segment = KanjiSegment("東京", "とうきょう", startIndex = 0, endIndex = 2)
        assertEquals("東京", segment.text)
        assertEquals("とうきょう", segment.reading)
        assertEquals(0, segment.startIndex)
        assertEquals(2, segment.endIndex)
    }

    @Test
    fun `KanjiSegment copy adjusts indices`() {
        val segment = KanjiSegment("食べ", "たべ", 3, 5)
        val adjusted = segment.copy(startIndex = 0, endIndex = 2)
        assertEquals(0, adjusted.startIndex)
        assertEquals(2, adjusted.endIndex)
        assertEquals("食べ", adjusted.text)
    }

    @Test
    fun `KanjiInfo stores JLPT and grade`() {
        val info = KanjiInfo(
            literal = "食",
            grade = 2,
            strokeCount = 9,
            frequency = 328,
            jlpt = 4,
            onReadings = listOf("ショク", "ジキ"),
            kunReadings = listOf("た.べる", "く.う"),
            meanings = listOf("eat", "food")
        )
        assertEquals("食", info.literal)
        assertEquals(2, info.grade)
        assertEquals(9, info.strokeCount)
        assertEquals(328, info.frequency)
        assertEquals(4, info.jlpt)
        assertEquals(2, info.onReadings.size)
        assertEquals(2, info.kunReadings.size)
        assertEquals(2, info.meanings.size)
    }

    @Test
    fun `KanjiInfo gradeLabel formats correctly`() {
        val info = KanjiInfo("日", 1, 4, 1, 4, listOf("ニチ"), listOf("ひ"), listOf("day"))
        assertEquals("Grade 1", info.gradeLabel)
    }

    @Test
    fun `KanjiInfo gradeLabel null when grade is null`() {
        val info = KanjiInfo("龍", null, 16, null, null, listOf("リュウ"), emptyList(), listOf("dragon"))
        assertEquals(null, info.gradeLabel)
    }

    @Test
    fun `KanjiInfo jlptLabel maps to N-levels`() {
        val n1 = KanjiInfo("龍", null, 16, null, 1, emptyList(), emptyList(), emptyList())
        assertEquals("JLPT N1", n1.jlptLabel)

        val n4 = KanjiInfo("日", 1, 4, 1, 4, emptyList(), emptyList(), emptyList())
        assertEquals("JLPT N4", n4.jlptLabel)
    }

    @Test
    fun `KanjiInfo jlptLabel null when jlpt is null`() {
        val info = KanjiInfo("龍", null, 16, null, null, emptyList(), emptyList(), emptyList())
        assertEquals(null, info.jlptLabel)
    }
}
