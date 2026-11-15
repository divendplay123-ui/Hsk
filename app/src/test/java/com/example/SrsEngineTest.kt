package com.example

import com.example.data.model.HskLevel
import com.example.data.model.HskStandard
import com.example.data.repository.HanziCatalog
import com.example.domain.SrsCardState
import com.example.domain.SrsEngine
import com.example.domain.SrsRating
import com.example.domain.StudyTechniquesEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SrsEngineTest {

    @Test
    fun sm2ProgressesIntervalsAndRespectsLapses() {
        val t0 = 1_700_000_000_000L

        // First Good -> 1 day
        val s1 = SrsEngine.review(null, SrsRating.GOOD, t0)
        assertEquals(1, s1.repetitions)
        assertEquals(1.0, s1.intervalDays, 0.001)
        assertEquals(0, s1.lapses)

        // Second Good -> 6 days
        val s2 = SrsEngine.review(s1, SrsRating.GOOD, t0 + 86_400_000L)
        assertEquals(2, s2.repetitions)
        assertEquals(6.0, s2.intervalDays, 0.001)
        assertTrue(SrsEngine.isMasteredLongTerm(s2))

        // Third Good -> ~15 days (6 * 2.5)
        val s3 = SrsEngine.review(s2, SrsRating.GOOD, t0 + 7L * 86_400_000L)
        assertEquals(3, s3.repetitions)
        assertEquals(15.0, s3.intervalDays, 0.001)

        // Again -> lapse, resets repetitions, schedules 10 min, drops ease factor
        val lapsed = SrsEngine.review(s3, SrsRating.AGAIN, t0 + 8L * 86_400_000L)
        assertEquals(0, lapsed.repetitions)
        assertEquals(1, lapsed.lapses)
        assertTrue(lapsed.easeFactor < s3.easeFactor)
        assertTrue(lapsed.easeFactor >= 1.3f)
        assertTrue(lapsed.intervalDays < 0.02)

        // Easy on fresh card -> 4 days & boosts ease
        val easy = SrsEngine.review(null, SrsRating.EASY, t0)
        assertEquals(1, easy.repetitions)
        assertEquals(4.0, easy.intervalDays, 0.001)
        assertTrue(easy.easeFactor > 2.5f)
    }

    @Test
    fun encodeAndDecodeRoundTripPreservesState() {
        val sample = mapOf(
            1 to SrsCardState(
                repetitions = 3,
                intervalDays = 15.0,
                easeFactor = 2.65f,
                dueAtMillis = 1_750_000_000_000L,
                lapses = 1,
                lastReviewMillis = 1_748_000_000_000L
            ),
            42 to SrsCardState(
                repetitions = 0,
                intervalDays = 0.007,
                easeFactor = 1.80f,
                dueAtMillis = 1_749_000_600_000L,
                lapses = 2,
                lastReviewMillis = 1_749_000_000_000L
            )
        )
        val encoded = SrsEngine.encodeRecords(sample)
        val decoded = SrsEngine.decodeRecords(encoded)
        assertEquals(sample.keys, decoded.keys)
        assertEquals(3, decoded[1]?.repetitions)
        assertEquals(15.0, decoded[1]?.intervalDays ?: 0.0, 0.01)
        assertEquals(2.65f, decoded[1]?.easeFactor ?: 0f, 0.01f)
        assertEquals(2, decoded[42]?.lapses)
    }

    @Test
    fun queuePrioritizesOverdueCardsBeforeNewAndFuture() {
        val pool = HanziCatalog.wordsForLevels(setOf(HskLevel.HSK1), HskStandard.HSK_3_0).take(5)
        val now = 2_000_000_000_000L
        val records = mapOf(
            pool[0].number to SrsCardState(repetitions = 2, intervalDays = 6.0, dueAtMillis = now + 500_000_000L), // future
            pool[2].number to SrsCardState(repetitions = 1, intervalDays = 1.0, dueAtMillis = now - 10_000L),      // due
            pool[4].number to SrsCardState(repetitions = 1, intervalDays = 1.0, dueAtMillis = now - 100_000L, lapses = 1) // most overdue
        )
        val queue = SrsEngine.buildSrsQueue(pool, records, nowMillis = now)
        assertEquals(pool[4].number, queue[0].number)
        assertEquals(pool[2].number, queue[1].number)
        assertTrue(queue.take(4).any { it.number == pool[1].number })
        assertFalse(queue.take(4).any { it.number == pool[0].number })
    }

    @Test
    fun studyTechniquesGenerateValidToneAndMatchChallenges() {
        val pool = HanziCatalog.wordsForLevels(setOf(HskLevel.HSK1), HskStandard.HSK_3_0)
        val recall = StudyTechniquesEngine.buildRecallRound(pool, roundSize = 8)
        assertEquals(8, recall.size)

        val variants = StudyTechniquesEngine.generateToneVariants("nǐ hǎo")
        assertTrue(variants.size >= 4)
        assertFalse("nǐ hǎo" in variants)

        val pairs = StudyTechniquesEngine.buildMatchPairs(pool, pairCount = 5)
        assertEquals(5, pairs.size)
        assertEquals(5, pairs.map { it.hanzi }.distinct().size)
        assertNotNull(StudyTechniquesEngine.buildToneChallenge(pool.first { it.pinyin.isNotBlank() }))
    }
}
