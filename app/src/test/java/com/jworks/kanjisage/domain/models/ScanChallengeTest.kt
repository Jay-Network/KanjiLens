package com.jworks.kanjisage.domain.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScanChallengeTest {

    @Test
    fun `challenge pool is not empty`() {
        assertTrue(ScanChallengeKanji.CHALLENGE_POOL.isNotEmpty())
    }

    @Test
    fun `challenge pool has at least 40 entries`() {
        assertTrue(
            "Expected at least 40 challenges, got ${ScanChallengeKanji.CHALLENGE_POOL.size}",
            ScanChallengeKanji.CHALLENGE_POOL.size >= 40
        )
    }

    @Test
    fun `all challenges have single kanji target`() {
        for (challenge in ScanChallengeKanji.CHALLENGE_POOL) {
            assertEquals(
                "Challenge '${challenge.targetKanji}' should be a single character",
                1,
                challenge.targetKanji.length
            )
        }
    }

    @Test
    fun `all challenge targets are actual kanji`() {
        for (challenge in ScanChallengeKanji.CHALLENGE_POOL) {
            assertTrue(
                "'${challenge.targetKanji}' should be kanji",
                JapaneseTextUtil.containsKanji(challenge.targetKanji)
            )
        }
    }

    @Test
    fun `all challenges have non-empty readings`() {
        for (challenge in ScanChallengeKanji.CHALLENGE_POOL) {
            assertTrue(
                "Challenge '${challenge.targetKanji}' has empty reading",
                challenge.reading.isNotBlank()
            )
        }
    }

    @Test
    fun `all readings are hiragana`() {
        for (challenge in ScanChallengeKanji.CHALLENGE_POOL) {
            assertTrue(
                "Reading '${challenge.reading}' for '${challenge.targetKanji}' should contain hiragana",
                JapaneseTextUtil.containsHiragana(challenge.reading)
            )
        }
    }

    @Test
    fun `all challenges have non-empty meanings`() {
        for (challenge in ScanChallengeKanji.CHALLENGE_POOL) {
            assertTrue(
                "Challenge '${challenge.targetKanji}' has empty meaning",
                challenge.meaning.isNotBlank()
            )
        }
    }

    @Test
    fun `no duplicate kanji in pool`() {
        val kanji = ScanChallengeKanji.CHALLENGE_POOL.map { it.targetKanji }
        assertEquals(
            "Duplicate kanji found in challenge pool",
            kanji.size,
            kanji.toSet().size
        )
    }

    @Test
    fun `challenges default to not completed`() {
        for (challenge in ScanChallengeKanji.CHALLENGE_POOL) {
            assertFalse(
                "Challenge '${challenge.targetKanji}' should default to not completed",
                challenge.isCompleted
            )
        }
    }

    @Test
    fun `getRandomChallenge returns a valid challenge`() {
        val challenge = ScanChallengeKanji.getRandomChallenge()
        assertNotNull(challenge)
        assertTrue(challenge.targetKanji.isNotEmpty())
        assertTrue(challenge.reading.isNotEmpty())
        assertTrue(challenge.meaning.isNotEmpty())
    }

    @Test
    fun `getRandomChallenge returns challenges from pool`() {
        // Call multiple times to increase confidence it draws from the pool
        repeat(20) {
            val challenge = ScanChallengeKanji.getRandomChallenge()
            assertTrue(
                "'${challenge.targetKanji}' not found in challenge pool",
                ScanChallengeKanji.CHALLENGE_POOL.contains(challenge)
            )
        }
    }

    @Test
    fun `ScanChallenge copy with isCompleted works`() {
        val challenge = ScanChallenge("食", "しょく", "eat/food")
        assertFalse(challenge.isCompleted)
        val completed = challenge.copy(isCompleted = true)
        assertTrue(completed.isCompleted)
        assertEquals("食", completed.targetKanji)
    }
}
