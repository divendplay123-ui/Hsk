package com.example.ui

import com.example.data.model.HanziEntry
import com.example.data.model.HskWordEntry
import com.example.domain.DrawnStroke

enum class StudyMode(val label: String) {
    FLASHCARDS("Карточки"),
    QUIZ("Тест"),
    WRITE("Пропись")
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
