package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AppSettings
import com.example.data.model.HanziEntry
import com.example.data.model.HskLevel
import com.example.data.model.HskStandard
import com.example.data.model.HskWordEntry
import com.example.data.model.RecognizedCandidate
import com.example.data.model.StrokeType
import com.example.data.repository.HanziCatalog
import com.example.data.repository.SettingsRepository
import com.example.domain.DrawnStroke
import com.example.domain.StrokePoint
import com.example.domain.StrokeRecognizer
import com.example.ui.components.AndroidGlyphRasterizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield

class HanziViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val QUIZ_ROUND_SIZE = 10
    }

    private val settingsRepository = SettingsRepository(application.applicationContext)

    val settings: StateFlow<AppSettings> = settingsRepository.settingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = AppSettings()
        )

    private val _drawnStrokes = MutableStateFlow<List<DrawnStroke>>(emptyList())
    val drawnStrokes: StateFlow<List<DrawnStroke>> = _drawnStrokes.asStateFlow()

    private val _selectedEntry = MutableStateFlow<HanziEntry?>(null)
    val selectedEntry: StateFlow<HanziEntry?> = _selectedEntry.asStateFlow()

    private val _dictionaryQuery = MutableStateFlow("")
    val dictionaryQuery: StateFlow<String> = _dictionaryQuery.asStateFlow()

    private val _dictionaryLevelFilter = MutableStateFlow<HskLevel?>(null)
    val dictionaryLevelFilter: StateFlow<HskLevel?> = _dictionaryLevelFilter.asStateFlow()

    private val _dictionaryShowWords = MutableStateFlow(false)
    val dictionaryShowWords: StateFlow<Boolean> = _dictionaryShowWords.asStateFlow()

    // ==================== Study Tab State ====================
    private val _studyMode = MutableStateFlow(StudyMode.FLASHCARDS)
    val studyMode: StateFlow<StudyMode> = _studyMode.asStateFlow()

    private val _onlyUnmastered = MutableStateFlow(false)
    val onlyUnmastered: StateFlow<Boolean> = _onlyUnmastered.asStateFlow()

    private val _flashcardShuffleSeed = MutableStateFlow(0)
    private val _flashcardIndex = MutableStateFlow(0)
    val flashcardIndex: StateFlow<Int> = _flashcardIndex.asStateFlow()

    private val _isFlashcardFlipped = MutableStateFlow(false)
    val isFlashcardFlipped: StateFlow<Boolean> = _isFlashcardFlipped.asStateFlow()

    val studyDeck: StateFlow<List<HskWordEntry>> = combine(
        settings,
        _onlyUnmastered,
        _flashcardShuffleSeed
    ) { currentSettings, unmasteredOnly, seed ->
        val pool = HanziCatalog.wordsForLevels(
            currentSettings.enabledLevels,
            currentSettings.hskStandard
        )
        val masteredSet = currentSettings.activeMasteredWords
        val filtered = if (unmasteredOnly) {
            pool.filter { it.number !in masteredSet }
        } else {
            pool
        }
        val effective = filtered.ifEmpty { pool }
        if (seed == 0) effective else effective.shuffled(kotlin.random.Random(seed))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = HanziCatalog.wordsForLevels(setOf(HskLevel.HSK1), HskStandard.HSK_3_0)
    )

    private val _quizState = MutableStateFlow(
        buildQuizState(HanziCatalog.wordsForLevels(setOf(HskLevel.HSK1), HskStandard.HSK_3_0))
    )
    val quizState: StateFlow<QuizUiState> = _quizState.asStateFlow()

    private val _writePracticeState = MutableStateFlow(
        buildWritePracticeState(
            word = HanziCatalog.wordsForLevels(setOf(HskLevel.HSK1), HskStandard.HSK_3_0).firstOrNull(),
            standard = HskStandard.HSK_3_0
        )
    )
    val writePracticeState: StateFlow<WritePracticeState> = _writePracticeState.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.Default) {
            HanziCatalog.entriesForLevels(setOf(HskLevel.HSK1), HskStandard.HSK_3_0).forEachIndexed { idx, entry ->
                AndroidGlyphRasterizer.getFeaturesForCharacter(entry.character)
                if (idx % 24 == 0) yield()
            }
        }
        viewModelScope.launch {
            var lastStandard: HskStandard? = null
            settings
                .map { it.hskStandard to it.enabledLevels }
                .distinctUntilChanged()
                .collect { (standard, enabledLevels) ->
                    val standardChanged = lastStandard != null && lastStandard != standard
                    lastStandard = standard

                    if (standardChanged) {
                        _flashcardIndex.value = 0
                        _isFlashcardFlipped.value = false
                        val pool = getActiveWordStudyPool(enabledLevels, standard)
                        _quizState.value = buildQuizState(pool)
                        _writePracticeState.value = buildWritePracticeState(
                            word = pool.randomOrNull(),
                            standard = standard,
                            completedCount = _writePracticeState.value.completedCount
                        )
                    } else {
                        val currentQuiz = _quizState.value
                        val isQuizIdle = currentQuiz.currentIndex == 0 &&
                            currentQuiz.selectedOptionNumber == null &&
                            !currentQuiz.isRoundFinished
                        if (isQuizIdle || currentQuiz.questions.isEmpty()) {
                            _quizState.value = buildQuizState(getActiveWordStudyPool(enabledLevels, standard))
                        }
                        val writeState = _writePracticeState.value
                        val currentWord = writeState.targetWord
                        if (writeState.strokes.isEmpty() &&
                            writeState.checkResult == null &&
                            (currentWord == null || currentWord.hskLevel !in enabledLevels)
                        ) {
                            val pool = getActiveWordStudyPool(enabledLevels, standard)
                            _writePracticeState.value = buildWritePracticeState(
                                word = pool.randomOrNull(),
                                standard = standard
                            )
                        }
                    }
                }
        }
    }

    val localCandidates: StateFlow<List<RecognizedCandidate>> = combine(
        _drawnStrokes,
        settings
    ) { strokes, currentSettings ->
        StrokeRecognizer.matchCandidates(
            drawnStrokes = strokes,
            enabledLevels = currentSettings.enabledLevels,
            strictStrokeCount = currentSettings.strictStrokeCount,
            hskStandard = currentSettings.hskStandard,
            templateFeaturesProvider = AndroidGlyphRasterizer::getFeaturesForCharacter
        )
    }.flowOn(Dispatchers.Default).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = emptyList()
    )

    val dictionaryEntries: StateFlow<List<HanziEntry>> = combine(
        _dictionaryQuery,
        _dictionaryLevelFilter,
        settings
    ) { query, levelFilter, currentSettings ->
        val effectiveLevels = if (levelFilter != null && levelFilter in currentSettings.enabledLevels) {
            setOf(levelFilter)
        } else {
            currentSettings.enabledLevels
        }
        HanziCatalog.search(query, effectiveLevels, currentSettings.hskStandard)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = HanziCatalog.entriesForLevels(setOf(HskLevel.HSK1), HskStandard.HSK_3_0)
    )

    val dictionaryWords: StateFlow<List<HskWordEntry>> = combine(
        _dictionaryQuery,
        _dictionaryLevelFilter,
        settings
    ) { query, levelFilter, currentSettings ->
        val effectiveLevels = if (levelFilter != null && levelFilter in currentSettings.enabledLevels) {
            setOf(levelFilter)
        } else {
            currentSettings.enabledLevels
        }
        HanziCatalog.searchWords(query, effectiveLevels, currentSettings.hskStandard)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = HanziCatalog.wordsForLevels(setOf(HskLevel.HSK1), HskStandard.HSK_3_0)
    )

    fun addDrawnStroke(points: List<StrokePoint>, canvasSize: Float) {
        if (points.isEmpty()) return
        val classified = StrokeRecognizer.classifyDrawnStroke(points, canvasSize)
        _drawnStrokes.value = _drawnStrokes.value + classified
    }

    fun addManualStroke(type: StrokeType) {
        val stroke = StrokeRecognizer.fromManualStroke(type)
        _drawnStrokes.value = _drawnStrokes.value + stroke
    }

    fun undoLastStroke() {
        val current = _drawnStrokes.value
        if (current.isNotEmpty()) {
            _drawnStrokes.value = current.dropLast(1)
        }
    }

    fun clearCanvas() {
        _drawnStrokes.value = emptyList()
    }

    fun openCharacterDetail(entry: HanziEntry) {
        _selectedEntry.value = entry
        viewModelScope.launch {
            settingsRepository.addRecentCharacter(entry.character)
        }
    }

    fun closeCharacterDetail() {
        _selectedEntry.value = null
    }

    fun updateDictionaryQuery(query: String) {
        _dictionaryQuery.value = query
    }

    fun selectDictionaryLevelFilter(level: HskLevel?) {
        _dictionaryLevelFilter.value = level
    }

    fun setDictionaryShowWords(showWords: Boolean) {
        _dictionaryShowWords.value = showWords
    }

    fun openWordDetail(word: HskWordEntry) {
        val standard = settings.value.hskStandard
        val firstEntry = word.hanzi
            .map { it.toString() }
            .firstNotNullOfOrNull { HanziCatalog.findByCharacter(it, standard) }
        if (firstEntry != null) {
            openCharacterDetail(firstEntry)
        }
    }

    fun openCharacterByChar(charStr: String) {
        val standard = settings.value.hskStandard
        val entry = HanziCatalog.findByCharacter(charStr, standard)
            ?: charStr.map { it.toString() }.firstNotNullOfOrNull { HanziCatalog.findByCharacter(it, standard) }
        if (entry != null) {
            openCharacterDetail(entry)
        }
    }

    fun setHskStandard(standard: HskStandard) {
        if (settings.value.hskStandard == standard) return
        _flashcardIndex.value = 0
        _isFlashcardFlipped.value = false
        viewModelScope.launch {
            settingsRepository.setHskStandard(standard)
        }
    }

    fun setIncludeHsk2(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setIncludeHsk2(enabled)
            if (!enabled && _dictionaryLevelFilter.value == HskLevel.HSK2) {
                _dictionaryLevelFilter.value = null
            }
        }
    }

    fun setIncludeHsk3(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setIncludeHsk3(enabled)
            if (!enabled && _dictionaryLevelFilter.value == HskLevel.HSK3) {
                _dictionaryLevelFilter.value = null
            }
        }
    }

    fun setIncludeHsk4(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setIncludeHsk4(enabled)
            if (!enabled && _dictionaryLevelFilter.value == HskLevel.HSK4) {
                _dictionaryLevelFilter.value = null
            }
        }
    }

    fun setShowGridGuide(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setShowGridGuide(enabled) }
    }

    fun setStrictStrokeCount(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setStrictStrokeCount(enabled) }
    }

    // ==================== Study Actions ====================

    fun setStudyMode(mode: StudyMode) {
        _studyMode.value = mode
        if (mode == StudyMode.QUIZ && _quizState.value.questions.isEmpty()) {
            startNewQuizRound()
        } else if (mode == StudyMode.WRITE && _writePracticeState.value.target == null) {
            nextWritePracticeTarget()
        }
    }

    fun toggleOnlyUnmastered(onlyUnmastered: Boolean) {
        _onlyUnmastered.value = onlyUnmastered
        _flashcardIndex.value = 0
        _isFlashcardFlipped.value = false
    }

    fun flipFlashcard() {
        _isFlashcardFlipped.value = !_isFlashcardFlipped.value
    }

    fun nextFlashcard() {
        val deckSize = studyDeck.value.size
        if (deckSize == 0) return
        _flashcardIndex.value = (_flashcardIndex.value + 1) % deckSize
        _isFlashcardFlipped.value = false
    }

    fun prevFlashcard() {
        val deckSize = studyDeck.value.size
        if (deckSize == 0) return
        _flashcardIndex.value = (_flashcardIndex.value - 1 + deckSize) % deckSize
        _isFlashcardFlipped.value = false
    }

    fun shuffleFlashcards() {
        _flashcardShuffleSeed.value = _flashcardShuffleSeed.value + 1
        _flashcardIndex.value = 0
        _isFlashcardFlipped.value = false
    }

    fun markFlashcardResult(word: HskWordEntry, mastered: Boolean) {
        val standard = settings.value.hskStandard
        viewModelScope.launch {
            settingsRepository.setWordMastered(word.number, mastered, standard)
        }
        nextFlashcard()
    }

    fun toggleMastered(entry: HanziEntry) {
        val currentlyMastered = entry.character in settings.value.masteredCharacters
        viewModelScope.launch {
            settingsRepository.setCharacterMastered(entry.character, !currentlyMastered)
        }
    }

    fun startNewQuizRound() {
        val pool = getActiveWordStudyPool()
        _quizState.value = buildQuizState(pool)
    }

    fun answerQuiz(optionNumber: Int) {
        val state = _quizState.value
        val question = state.currentQuestion ?: return
        if (state.selectedOptionNumber != null) return

        val isCorrect = optionNumber == question.target.number
        if (isCorrect) {
            val standard = settings.value.hskStandard
            viewModelScope.launch {
                settingsRepository.setWordMastered(question.target.number, true, standard)
            }
        }
        _quizState.value = state.copy(
            selectedOptionNumber = optionNumber,
            correctCount = if (isCorrect) state.correctCount + 1 else state.correctCount,
            streak = if (isCorrect) state.streak + 1 else 0
        )
    }

    fun nextQuizQuestion() {
        val state = _quizState.value
        val nextIdx = state.currentIndex + 1
        if (nextIdx >= state.questions.size) {
            _quizState.value = state.copy(isRoundFinished = true)
        } else {
            _quizState.value = state.copy(
                currentIndex = nextIdx,
                selectedOptionNumber = null
            )
        }
    }

    fun addWritePracticeStroke(points: List<StrokePoint>, canvasSize: Float) {
        if (points.isEmpty()) return
        val classified = StrokeRecognizer.classifyDrawnStroke(points, canvasSize)
        val current = _writePracticeState.value
        _writePracticeState.value = current.copy(
            strokes = current.strokes + classified,
            checkResult = null
        )
    }

    fun undoWritePracticeStroke() {
        val current = _writePracticeState.value
        if (current.strokes.isNotEmpty()) {
            _writePracticeState.value = current.copy(
                strokes = current.strokes.dropLast(1),
                checkResult = null
            )
        }
    }

    fun clearWritePracticeCanvas() {
        val current = _writePracticeState.value
        _writePracticeState.value = current.copy(
            strokes = emptyList(),
            checkResult = null
        )
    }

    fun toggleWriteHint() {
        val current = _writePracticeState.value
        _writePracticeState.value = current.copy(showHintAnimation = !current.showHintAnimation)
    }

    fun selectWritePracticeCharIndex(index: Int) {
        val current = _writePracticeState.value
        val word = current.targetWord ?: return
        val standard = settings.value.hskStandard
        val entries = word.hanzi.mapNotNull { HanziCatalog.findByCharacter(it.toString(), standard) }
        val safeIdx = index.coerceIn(0, (entries.size - 1).coerceAtLeast(0))
        val targetEntry = entries.getOrNull(safeIdx) ?: return
        _writePracticeState.value = current.copy(
            activeCharIndex = safeIdx,
            target = targetEntry,
            strokes = emptyList(),
            checkResult = null,
            showHintAnimation = false
        )
    }

    fun checkWritePractice() {
        val current = _writePracticeState.value
        val target = current.target ?: return
        if (current.strokes.isEmpty()) return

        val currentSettings = settings.value
        val standard = currentSettings.hskStandard
        viewModelScope.launch(Dispatchers.Default) {
            val candidates = StrokeRecognizer.matchCandidates(
                drawnStrokes = current.strokes,
                enabledLevels = currentSettings.enabledLevels,
                strictStrokeCount = false,
                hskStandard = standard,
                templateFeaturesProvider = AndroidGlyphRasterizer::getFeaturesForCharacter,
                limit = 12
            )
            val rankIndex = candidates.indexOfFirst { it.entry.character == target.character }
            val matchedRank = if (rankIndex >= 0) rankIndex + 1 else null
            val isSuccess = matchedRank != null && matchedRank <= 3
            val bestGuess = candidates.firstOrNull()?.entry
            val confidence = if (rankIndex >= 0) {
                candidates[rankIndex].confidence
            } else {
                candidates.firstOrNull()?.confidence ?: 0f
            }

            if (isSuccess) {
                settingsRepository.setCharacterMastered(target.character, true)
                current.targetWord?.let { word ->
                    settingsRepository.setWordMastered(word.number, true, standard)
                }
            }

            _writePracticeState.value = _writePracticeState.value.copy(
                checkResult = WriteCheckResult(
                    isSuccess = isSuccess,
                    matchedRank = matchedRank,
                    bestGuess = bestGuess,
                    confidence = confidence
                ),
                completedCount = if (isSuccess) current.completedCount + 1 else current.completedCount
            )
        }
    }

    fun nextWritePracticeTarget() {
        val standard = settings.value.hskStandard
        val pool = getActiveWordStudyPool(settings.value.enabledLevels, standard)
        val currentWord = _writePracticeState.value.targetWord
        val nextWord = pool.filter { it.number != currentWord?.number }.randomOrNull()
            ?: pool.firstOrNull()
        _writePracticeState.value = buildWritePracticeState(
            word = nextWord,
            standard = standard,
            completedCount = _writePracticeState.value.completedCount
        )
    }

    private fun getActiveWordStudyPool(
        enabledLevels: Set<HskLevel> = settings.value.enabledLevels,
        standard: HskStandard = settings.value.hskStandard
    ): List<HskWordEntry> {
        val currentSettings = settings.value
        val allEnabled = HanziCatalog.wordsForLevels(enabledLevels, standard)
        val masteredSet = if (standard == HskStandard.HSK_2_0) {
            currentSettings.masteredWordsHsk20
        } else {
            currentSettings.masteredWords
        }
        return if (_onlyUnmastered.value) {
            allEnabled.filter { it.number !in masteredSet }.ifEmpty { allEnabled }
        } else {
            allEnabled
        }
    }

    private fun buildWritePracticeState(
        word: HskWordEntry?,
        standard: HskStandard = HskStandard.HSK_3_0,
        completedCount: Int = 0
    ): WritePracticeState {
        val firstCharEntry = word?.hanzi
            ?.mapNotNull { HanziCatalog.findByCharacter(it.toString(), standard) }
            ?.firstOrNull()
            ?: HanziCatalog.allEntriesFor(standard).firstOrNull()
        return WritePracticeState(
            targetWord = word,
            activeCharIndex = 0,
            target = firstCharEntry,
            completedCount = completedCount
        )
    }

    private fun buildQuizState(pool: List<HskWordEntry>): QuizUiState {
        if (pool.size < 4) return QuizUiState()
        val targets = pool.shuffled().take(QUIZ_ROUND_SIZE)
        val questions = targets.mapIndexed { idx, target ->
            val distractors = pool
                .filter { it.hanzi != target.hanzi && it.meaningRu != target.meaningRu }
                .shuffled()
                .distinctBy { it.hanzi }
                .take(3)
                .let { picked ->
                    if (picked.size == 3) picked
                    else (picked + pool.filter { it.number != target.number && it !in picked }.shuffled().take(3 - picked.size))
                }
            val options = (listOf(target) + distractors).shuffled()
            val type = if (idx % 2 == 0) {
                QuizQuestionType.WORD_TO_MEANING
            } else {
                QuizQuestionType.MEANING_TO_WORD
            }
            QuizQuestion(target = target, options = options, type = type)
        }
        return QuizUiState(questions = questions)
    }
}
