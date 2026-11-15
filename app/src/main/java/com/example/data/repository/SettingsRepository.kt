package com.example.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.AppSettings
import com.example.data.model.HskStandard
import com.example.domain.SrsEngine
import com.example.domain.SrsRating
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "hanzi_hsk_settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val HSK_STANDARD = stringPreferencesKey("hsk_standard")
        val INCLUDE_HSK2 = booleanPreferencesKey("include_hsk2")
        val INCLUDE_HSK3 = booleanPreferencesKey("include_hsk3")
        val INCLUDE_HSK4 = booleanPreferencesKey("include_hsk4")
        val INCLUDE_UNI = booleanPreferencesKey("include_uni")
        val SHOW_GRID_GUIDE = booleanPreferencesKey("show_grid_guide")
        val STRICT_STROKE_COUNT = booleanPreferencesKey("strict_stroke_count")
        val RECENT_CHARACTERS = stringPreferencesKey("recent_characters")
        val MASTERED_CHARACTERS = stringPreferencesKey("mastered_characters")
        val MASTERED_WORDS = stringPreferencesKey("mastered_words")
        val MASTERED_WORDS_HSK20 = stringPreferencesKey("mastered_words_hsk20")
        val SRS_RECORDS_HSK30 = stringPreferencesKey("srs_records_hsk30")
        val SRS_RECORDS_HSK20 = stringPreferencesKey("srs_records_hsk20")
        val DAILY_REVIEWS_COUNT = intPreferencesKey("daily_reviews_count")
        val DAILY_REVIEWS_EPOCH_DAY = longPreferencesKey("daily_reviews_epoch_day")
        val STUDY_STREAK_DAYS = intPreferencesKey("study_streak_days")
        val LAST_STUDY_EPOCH_DAY = longPreferencesKey("last_study_epoch_day")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        val recentRaw = prefs[Keys.RECENT_CHARACTERS].orEmpty()
        val recentList = if (recentRaw.isBlank()) {
            emptyList()
        } else {
            recentRaw.split("|").filter { it.isNotBlank() }
        }
        val masteredRaw = prefs[Keys.MASTERED_CHARACTERS].orEmpty()
        val masteredSet = if (masteredRaw.isBlank()) {
            emptySet()
        } else {
            masteredRaw.split("|").filter { it.isNotBlank() }.toSet()
        }
        val masteredWordsRaw = prefs[Keys.MASTERED_WORDS].orEmpty()
        val masteredWordsSet = if (masteredWordsRaw.isBlank()) {
            emptySet()
        } else {
            masteredWordsRaw.split("|").mapNotNull { it.toIntOrNull() }.toSet()
        }
        val masteredWordsHsk20Raw = prefs[Keys.MASTERED_WORDS_HSK20].orEmpty()
        val masteredWordsHsk20Set = if (masteredWordsHsk20Raw.isBlank()) {
            emptySet()
        } else {
            masteredWordsHsk20Raw.split("|").mapNotNull { it.toIntOrNull() }.toSet()
        }
        AppSettings(
            hskStandard = HskStandard.fromKey(prefs[Keys.HSK_STANDARD]),
            includeHsk2 = prefs[Keys.INCLUDE_HSK2] ?: false,
            includeHsk3 = prefs[Keys.INCLUDE_HSK3] ?: false,
            includeHsk4 = prefs[Keys.INCLUDE_HSK4] ?: false,
            includeUni = prefs[Keys.INCLUDE_UNI] ?: false,
            showGridGuide = prefs[Keys.SHOW_GRID_GUIDE] ?: true,
            strictStrokeCount = prefs[Keys.STRICT_STROKE_COUNT] ?: false,
            recentCharacters = recentList,
            masteredCharacters = masteredSet,
            masteredWords = masteredWordsSet,
            masteredWordsHsk20 = masteredWordsHsk20Set,
            srsRecordsHsk30 = SrsEngine.decodeRecords(prefs[Keys.SRS_RECORDS_HSK30].orEmpty()),
            srsRecordsHsk20 = SrsEngine.decodeRecords(prefs[Keys.SRS_RECORDS_HSK20].orEmpty()),
            dailyReviewsCount = prefs[Keys.DAILY_REVIEWS_COUNT] ?: 0,
            dailyReviewsEpochDay = prefs[Keys.DAILY_REVIEWS_EPOCH_DAY] ?: 0L,
            studyStreakDays = prefs[Keys.STUDY_STREAK_DAYS] ?: 0,
            lastStudyEpochDay = prefs[Keys.LAST_STUDY_EPOCH_DAY] ?: 0L
        )
    }

    suspend fun setHskStandard(standard: HskStandard) {
        context.dataStore.edit { it[Keys.HSK_STANDARD] = standard.key }
    }

    suspend fun setIncludeHsk2(enabled: Boolean) {
        context.dataStore.edit { it[Keys.INCLUDE_HSK2] = enabled }
    }

    suspend fun setIncludeHsk3(enabled: Boolean) {
        context.dataStore.edit { it[Keys.INCLUDE_HSK3] = enabled }
    }

    suspend fun setIncludeHsk4(enabled: Boolean) {
        context.dataStore.edit { it[Keys.INCLUDE_HSK4] = enabled }
    }

    suspend fun setIncludeUni(enabled: Boolean) {
        context.dataStore.edit { it[Keys.INCLUDE_UNI] = enabled }
    }

    suspend fun setShowGridGuide(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_GRID_GUIDE] = enabled }
    }

    suspend fun setStrictStrokeCount(enabled: Boolean) {
        context.dataStore.edit { it[Keys.STRICT_STROKE_COUNT] = enabled }
    }

    suspend fun addRecentCharacter(character: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.RECENT_CHARACTERS]
                .orEmpty()
                .split("|")
                .filter { it.isNotBlank() && it != character }
            val updated = (listOf(character) + current).take(12)
            prefs[Keys.RECENT_CHARACTERS] = updated.joinToString("|")
        }
    }

    suspend fun setCharacterMastered(character: String, mastered: Boolean) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.MASTERED_CHARACTERS]
                .orEmpty()
                .split("|")
                .filter { it.isNotBlank() }
                .toMutableSet()
            if (mastered) current.add(character) else current.remove(character)
            prefs[Keys.MASTERED_CHARACTERS] = current.joinToString("|")
        }
    }

    suspend fun setWordMastered(
        wordNumber: Int,
        mastered: Boolean,
        standard: HskStandard = HskStandard.HSK_3_0
    ) {
        val prefKey = if (standard == HskStandard.HSK_2_0) {
            Keys.MASTERED_WORDS_HSK20
        } else {
            Keys.MASTERED_WORDS
        }
        context.dataStore.edit { prefs ->
            val current = prefs[prefKey]
                .orEmpty()
                .split("|")
                .filter { it.isNotBlank() }
                .toMutableSet()
            val key = wordNumber.toString()
            if (mastered) current.add(key) else current.remove(key)
            prefs[prefKey] = current.joinToString("|")
        }
    }

    suspend fun recordSrsReview(
        wordNumber: Int,
        rating: SrsRating,
        standard: HskStandard,
        nowMillis: Long = System.currentTimeMillis()
    ) {
        val srsKey = if (standard == HskStandard.HSK_2_0) {
            Keys.SRS_RECORDS_HSK20
        } else {
            Keys.SRS_RECORDS_HSK30
        }
        val masteredKey = if (standard == HskStandard.HSK_2_0) {
            Keys.MASTERED_WORDS_HSK20
        } else {
            Keys.MASTERED_WORDS
        }
        val todayEpochDay = nowMillis / 86_400_000L

        context.dataStore.edit { prefs ->
            val currentMap = SrsEngine.decodeRecords(prefs[srsKey].orEmpty()).toMutableMap()
            val updatedState = SrsEngine.review(currentMap[wordNumber], rating, nowMillis)
            currentMap[wordNumber] = updatedState
            prefs[srsKey] = SrsEngine.encodeRecords(currentMap)

            // Sync masteredWords flag: Good/Easy marks word known; Again removes it
            val masteredSet = prefs[masteredKey]
                .orEmpty()
                .split("|")
                .filter { it.isNotBlank() }
                .toMutableSet()
            val numKey = wordNumber.toString()
            when (rating) {
                SrsRating.AGAIN -> masteredSet.remove(numKey)
                SrsRating.GOOD, SrsRating.EASY -> masteredSet.add(numKey)
                SrsRating.HARD -> Unit
            }
            prefs[masteredKey] = masteredSet.joinToString("|")

            // Update daily counter & streak
            val prevDailyDay = prefs[Keys.DAILY_REVIEWS_EPOCH_DAY] ?: 0L
            val prevDailyCount = prefs[Keys.DAILY_REVIEWS_COUNT] ?: 0
            prefs[Keys.DAILY_REVIEWS_EPOCH_DAY] = todayEpochDay
            prefs[Keys.DAILY_REVIEWS_COUNT] = if (prevDailyDay == todayEpochDay) prevDailyCount + 1 else 1

            val lastStreakDay = prefs[Keys.LAST_STUDY_EPOCH_DAY] ?: 0L
            val currentStreak = prefs[Keys.STUDY_STREAK_DAYS] ?: 0
            val nextStreak = when {
                lastStreakDay == todayEpochDay -> currentStreak.coerceAtLeast(1)
                lastStreakDay == todayEpochDay - 1L -> currentStreak + 1
                else -> 1
            }
            prefs[Keys.LAST_STUDY_EPOCH_DAY] = todayEpochDay
            prefs[Keys.STUDY_STREAK_DAYS] = nextStreak
        }
    }
}
