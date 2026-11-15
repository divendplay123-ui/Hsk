package com.example.domain

import com.example.data.model.HskWordEntry
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

enum class SrsRating(
    val code: Int,
    val labelRu: String
) {
    AGAIN(0, "Снова"),
    HARD(1, "Трудно"),
    GOOD(2, "Хорошо"),
    EASY(3, "Легко");

    companion object {
        fun fromCode(code: Int): SrsRating = entries.firstOrNull { it.code == code } ?: GOOD
    }
}

data class SrsCardState(
    val repetitions: Int = 0,
    val intervalDays: Double = 0.0,
    val easeFactor: Float = 2.5f,
    val dueAtMillis: Long = 0L,
    val lapses: Int = 0,
    val lastReviewMillis: Long = 0L
)

object SrsEngine {

    private const val DAY_MS = 86_400_000L
    private const val HOUR_MS = 3_600_000L
    private const val MINUTE_MS = 60_000L

    private const val RELEARN_TEN_MIN_DAYS = 10.0 / 1440.0
    private const val HALF_DAY = 0.5

    /**
     * SM-2 spaced repetition update with short-term learning steps for lapses/hard first recalls.
     */
    fun review(
        current: SrsCardState?,
        rating: SrsRating,
        nowMillis: Long = System.currentTimeMillis()
    ): SrsCardState {
        val state = current ?: SrsCardState()
        val prevEf = state.easeFactor.coerceIn(1.3f, 3.0f)

        return when (rating) {
            SrsRating.AGAIN -> {
                val nextEf = max(1.3f, prevEf - 0.20f)
                SrsCardState(
                    repetitions = 0,
                    intervalDays = RELEARN_TEN_MIN_DAYS,
                    easeFactor = nextEf,
                    dueAtMillis = nowMillis + 10L * MINUTE_MS,
                    lapses = state.lapses + 1,
                    lastReviewMillis = nowMillis
                )
            }

            SrsRating.HARD -> {
                val nextEf = max(1.3f, prevEf - 0.14f)
                val nextReps = if (state.repetitions == 0) 1 else state.repetitions
                val nextIntervalDays = when {
                    state.repetitions == 0 || state.intervalDays < 1.0 -> HALF_DAY
                    else -> max(1.0, state.intervalDays * 1.2)
                }
                SrsCardState(
                    repetitions = nextReps,
                    intervalDays = nextIntervalDays,
                    easeFactor = nextEf,
                    dueAtMillis = nowMillis + (nextIntervalDays * DAY_MS).toLong(),
                    lapses = state.lapses,
                    lastReviewMillis = nowMillis
                )
            }

            SrsRating.GOOD -> {
                val nextReps = state.repetitions + 1
                val nextIntervalDays = when {
                    state.repetitions == 0 -> 1.0
                    state.repetitions == 1 -> 6.0
                    else -> max(state.intervalDays + 1.0, (state.intervalDays * prevEf).roundToInt().toDouble())
                }
                SrsCardState(
                    repetitions = nextReps,
                    intervalDays = nextIntervalDays,
                    easeFactor = prevEf,
                    dueAtMillis = nowMillis + (nextIntervalDays * DAY_MS).toLong(),
                    lapses = state.lapses,
                    lastReviewMillis = nowMillis
                )
            }

            SrsRating.EASY -> {
                val nextEf = min(3.0f, prevEf + 0.15f)
                val nextReps = state.repetitions + 1
                val nextIntervalDays = when {
                    state.repetitions == 0 -> 4.0
                    state.repetitions == 1 -> 10.0
                    else -> max(state.intervalDays + 2.0, (state.intervalDays * nextEf * 1.3).roundToInt().toDouble())
                }
                SrsCardState(
                    repetitions = nextReps,
                    intervalDays = nextIntervalDays,
                    easeFactor = nextEf,
                    dueAtMillis = nowMillis + (nextIntervalDays * DAY_MS).toLong(),
                    lapses = state.lapses,
                    lastReviewMillis = nowMillis
                )
            }
        }
    }

    fun formatIntervalPreview(
        current: SrsCardState?,
        rating: SrsRating,
        nowMillis: Long = System.currentTimeMillis()
    ): String {
        val next = review(current, rating, nowMillis)
        return formatDaysLabel(next.intervalDays)
    }

    fun formatDaysLabel(days: Double): String {
        val totalMinutes = (days * 1440.0).roundToInt()
        return when {
            totalMinutes < 60 -> "${max(1, totalMinutes)} мин"
            days < 1.0 -> "${max(1, (days * 24.0).roundToInt())} ч"
            days < 14.0 -> "${max(1, days.roundToInt())} дн."
            days < 60.0 -> "${max(2, (days / 7.0).roundToInt())} нед."
            else -> "${max(2, (days / 30.0).roundToInt())} мес."
        }
    }

    fun isDue(state: SrsCardState?, nowMillis: Long = System.currentTimeMillis()): Boolean =
        state != null && state.dueAtMillis <= nowMillis

    fun isMasteredLongTerm(state: SrsCardState?): Boolean =
        state != null && state.repetitions >= 2 && state.intervalDays >= 6.0

    fun isWeakOrHard(state: SrsCardState?): Boolean =
        state != null && (state.lapses > 0 || state.easeFactor < 2.25f || state.repetitions == 0)

    /**
     * Builds an ordered SM-2 study queue:
     * 1. Due cards (most overdue first, ties broken by more lapses)
     * 2. New/unreviewed cards from the pool (or only unreviewed if requested)
     * 3. If nothing is due and all words are reviewed, falls back to soonest upcoming reviews
     */
    fun buildSrsQueue(
        pool: List<HskWordEntry>,
        records: Map<Int, SrsCardState>,
        nowMillis: Long = System.currentTimeMillis(),
        newLimit: Int = 25,
        hardOnly: Boolean = false
    ): List<HskWordEntry> {
        if (pool.isEmpty()) return emptyList()

        if (hardOnly) {
            val weak = pool.filter { word ->
                val st = records[word.number]
                isWeakOrHard(st)
            }.sortedWith(
                compareByDescending<HskWordEntry> { records[it.number]?.lapses ?: 0 }
                    .thenBy { records[it.number]?.easeFactor ?: 2.5f }
                    .thenBy { it.number }
            )
            if (weak.isNotEmpty()) return weak
        }

        val dueCards = pool.filter { word ->
            isDue(records[word.number], nowMillis)
        }.sortedWith(
            compareBy<HskWordEntry> { records[it.number]?.dueAtMillis ?: 0L }
                .thenByDescending { records[it.number]?.lapses ?: 0 }
        )

        val newCards = pool.filter { word ->
            word.number !in records
        }.take(newLimit)

        val combined = (dueCards + newCards).distinctBy { it.number }
        if (combined.isNotEmpty()) return combined

        // Everything reviewed and nothing due yet: show soonest upcoming cards for extra practice
        return pool.sortedBy { records[it.number]?.dueAtMillis ?: Long.MAX_VALUE }
    }

    fun encodeRecords(records: Map<Int, SrsCardState>): String {
        if (records.isEmpty()) return ""
        return records.entries.joinToString(";") { (num, st) ->
            val efInt = (st.easeFactor * 100f).roundToInt()
            val ivInt = (st.intervalDays * 1000.0).roundToInt()
            "$num:${st.repetitions},$ivInt,$efInt,${st.dueAtMillis},${st.lapses},${st.lastReviewMillis}"
        }
    }

    fun decodeRecords(raw: String): Map<Int, SrsCardState> {
        if (raw.isBlank()) return emptyMap()
        val result = LinkedHashMap<Int, SrsCardState>()
        raw.split(";").forEach { entry ->
            val sep = entry.indexOf(':')
            if (sep <= 0) return@forEach
            val num = entry.substring(0, sep).toIntOrNull() ?: return@forEach
            val parts = entry.substring(sep + 1).split(",")
            if (parts.size < 5) return@forEach
            val reps = parts[0].toIntOrNull() ?: 0
            val ivDays = (parts[1].toIntOrNull() ?: 0) / 1000.0
            val ef = ((parts[2].toIntOrNull() ?: 250) / 100f).coerceIn(1.3f, 3.0f)
            val due = parts[3].toLongOrNull() ?: 0L
            val lapses = parts[4].toIntOrNull() ?: 0
            val last = parts.getOrNull(5)?.toLongOrNull() ?: 0L
            result[num] = SrsCardState(
                repetitions = reps,
                intervalDays = ivDays,
                easeFactor = ef,
                dueAtMillis = due,
                lapses = lapses,
                lastReviewMillis = last
            )
        }
        return result
    }
}
