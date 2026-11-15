package com.example.data.model

import com.example.domain.SrsCardState

enum class HskStandard(
    val key: String,
    val label: String,
    val shortLabel: String,
    val descriptionRu: String
) {
    HSK_3_0(
        key = "HSK30",
        label = "HSK 3.0 (новый)",
        shortLabel = "HSK 3.0",
        descriptionRu = "2 000 слов · 936 иерогл. (1–4) + 165 ВУЗ"
    ),
    HSK_2_0(
        key = "HSK20",
        label = "HSK 2.0 (старый)",
        shortLabel = "HSK 2.0",
        descriptionRu = "1 200 слов · 903 иерогл. (1–4) + 165 ВУЗ"
    );

    companion object {
        fun fromKey(key: String?): HskStandard =
            entries.firstOrNull { it.key == key } ?: HSK_3_0
    }
}

enum class HskLevel(
    val number: Int,
    val label: String,
    val descriptionRu: String
) {
    HSK1(1, "HSK 1", "Базовый уровень (всегда активен)"),
    HSK2(2, "HSK 2", "Элементарный уровень"),
    HSK3(3, "HSK 3", "Пороговый уровень"),
    HSK4(4, "HSK 4", "Средний уровень"),
    UNI(5, "ВУЗ", "Программа вуза (уроки 1–13)");

    companion object {
        fun fromNumber(number: Int): HskLevel = entries.firstOrNull { it.number == number } ?: HSK1
    }
}

enum class StrokeType(
    val code: Char,
    val symbol: String,
    val nameRu: String
) {
    HORIZONTAL('1', "一", "Горизонтальная"),
    VERTICAL('2', "丨", "Вертикальная"),
    LEFT_FALLING('3', "丿", "Откидная влево"),
    DOT_OR_RIGHT_FALLING('4', "丶", "Точка / вправо"),
    TURNING_OR_HOOK('5', "乙", "Ломаная / крюк");

    companion object {
        fun fromCode(code: Char): StrokeType =
            entries.firstOrNull { it.code == code } ?: HORIZONTAL
    }
}

data class ExampleWord(
    val hanzi: String,
    val pinyin: String,
    val meaningRu: String
)

data class HskWordEntry(
    val number: Int,
    val hskLevel: HskLevel,
    val hanzi: String,
    val pinyin: String,
    val pos: String,
    val meaningRu: String,
    val meaningEn: String = ""
)

data class HanziEntry(
    val character: String,
    val pinyin: String,
    val meaningRu: String,
    val hskLevel: HskLevel,
    val strokeCode: String,
    val radical: String,
    val examples: List<ExampleWord> = emptyList()
) {
    val strokeCount: Int get() = strokeCode.length

    val strokeTypes: List<StrokeType>
        get() = strokeCode.map { StrokeType.fromCode(it) }
}

data class RecognizedCandidate(
    val entry: HanziEntry,
    val confidence: Float
)

data class AppSettings(
    val hskStandard: HskStandard = HskStandard.HSK_3_0,
    val includeHsk2: Boolean = false,
    val includeHsk3: Boolean = false,
    val includeHsk4: Boolean = false,
    val includeUni: Boolean = true,
    val showGridGuide: Boolean = true,
    val strictStrokeCount: Boolean = false,
    val recentCharacters: List<String> = emptyList(),
    val masteredCharacters: Set<String> = emptySet(),
    val masteredWords: Set<Int> = emptySet(),
    val masteredWordsHsk20: Set<Int> = emptySet(),
    val srsRecordsHsk30: Map<Int, SrsCardState> = emptyMap(),
    val srsRecordsHsk20: Map<Int, SrsCardState> = emptyMap(),
    val dailyReviewsCount: Int = 0,
    val dailyReviewsEpochDay: Long = 0L,
    val studyStreakDays: Int = 0,
    val lastStudyEpochDay: Long = 0L
) {
    val enabledLevels: Set<HskLevel>
        get() = buildSet {
            add(HskLevel.HSK1)
            if (includeHsk2) add(HskLevel.HSK2)
            if (includeHsk3) add(HskLevel.HSK3)
            if (includeHsk4) add(HskLevel.HSK4)
            if (includeUni) add(HskLevel.UNI)
        }

    val activeMasteredWords: Set<Int>
        get() = if (hskStandard == HskStandard.HSK_2_0) masteredWordsHsk20 else masteredWords

    val activeSrsRecords: Map<Int, SrsCardState>
        get() = if (hskStandard == HskStandard.HSK_2_0) srsRecordsHsk20 else srsRecordsHsk30
}
