package com.example.ui

import com.example.data.model.HanziEntry
import com.example.data.model.HskWordEntry
import com.example.domain.DrawnStroke
import com.example.domain.RecallChallenge

enum class StudyMode(val label: String) {
    FLASHCARDS("Интервалы"),
    RECALL("Тоны и сборка"),
    MATCH("Пары"),
    QUIZ("Тест"),
    WRITE("Пропись")
}

enum class FlashcardDeckMode(val label: String) {
    SRS_QUEUE("Очередь SM-2"),
    ALL_WORDS("Все слова"),
    HARD_ONLY("Сложные")
}

enum class FlashcardDirection(val label: String) {
    HANZI_TO_RU("汉字 → Перевод"),
    RU_TO_HANZI("Перевод → 汉字")
}

enum class QuizQuestionType {
    WORD_TO_MEANING,
    MEANING_TO_WORD
}

data class QuizQuestion(
    val target: HskWordEntry,
    val options: List<HskWordEntry>,
    val type: QuizQuestionType
)

data class QuizUiState(
    val questions: List<QuizQuestion> = emptyList(),
    val currentIndex: Int = 0,
    val selectedOptionNumber: Int? = null,
    val correctCount: Int = 0,
    val streak: Int = 0,
    val isRoundFinished: Boolean = false
) {
    val currentQuestion: QuizQuestion?
        get() = questions.getOrNull(currentIndex)
}

data class ActiveRecallUiState(
    val challenges: List<RecallChallenge> = emptyList(),
    val currentIndex: Int = 0,
    val selectedToneOption: String? = null,
    val assembledChars: List<String> = emptyList(),
    val isAssembleSubmitted: Boolean = false,
    val isCorrect: Boolean? = null,
    val correctCount: Int = 0,
    val streak: Int = 0,
    val isRoundFinished: Boolean = false
) {
    val currentChallenge: RecallChallenge?
        get() = challenges.getOrNull(currentIndex)
}

data class MatchRoundUiState(
    val pairs: List<HskWordEntry> = emptyList(),
    val rightColumnWords: List<HskWordEntry> = emptyList(),
    val matchedNumbers: Set<Int> = emptySet(),
    val selectedLeftNumber: Int? = null,
    val selectedRightNumber: Int? = null,
    val wrongPairNumbers: Pair<Int, Int>? = null,
    val mistakesCount: Int = 0,
    val roundNumber: Int = 1
) {
    val isRoundFinished: Boolean
        get() = pairs.isNotEmpty() && matchedNumbers.size == pairs.size
}

data class WriteCheckResult(
    val isSuccess: Boolean,
    val matchedRank: Int?,
    val bestGuess: HanziEntry?,
    val confidence: Float
)

data class WritePracticeState(
    val targetWord: HskWordEntry? = null,
    val activeCharIndex: Int = 0,
    val target: HanziEntry? = null,
    val strokes: List<DrawnStroke> = emptyList(),
    val checkResult: WriteCheckResult? = null,
    val showHintAnimation: Boolean = false,
    val completedCount: Int = 0
)
