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
import com.example.domain.MakeMeAHanziRepository
import com.example.domain.RecallChallenge
import com.example.domain.SrsEngine
import com.example.domain.SrsRating
import com.example.domain.StrokePoint
import com.example.domain.StrokeRecognizer
import com.example.domain.StudyTechniquesEngine
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

    private val _selectedWord = MutableStateFlow<HskWordEntry?>(null)
    val selectedWord: StateFlow<HskWordEntry?> = _selectedWord.asStateFlow()

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

    private val _flashcardDeckMode = MutableStateFlow(FlashcardDeckMode.SRS_QUEUE)
    val flashcardDeckMode: StateFlow<FlashcardDeckMode> = _flashcardDeckMode.asStateFlow()

    private val _flashcardDirection = MutableStateFlow(FlashcardDirection.HANZI_TO_RU)
    val flashcardDirection: StateFlow<FlashcardDirection> = _flashcardDirection.asStateFlow()

    private val _flashcardShuffleSeed = MutableStateFlow(0)
    private val _flashcardSessionNumbers = MutableStateFlow<List<Int>>(emptyList())
    private val _flashcardIndex = MutableStateFlow(0)
    val flashcardIndex: StateFlow<Int> = _flashcardIndex.asStateFlow()

    private val _isFlashcardFlipped = MutableStateFlow(false)
    val isFlashcardFlipped: StateFlow<Boolean> = _isFlashcardFlipped.asStateFlow()

    val studyDeck: StateFlow<List<HskWordEntry>> = combine(
        settings,
        _onlyUnmastered,
        _flashcardDeckMode,
        _flashcardShuffleSeed,
        _flashcardSessionNumbers
    ) { currentSettings, unmasteredOnly, deckMode, seed, sessionNums ->
        val pool = HanziCatalog.wordsForLevels(
            currentSettings.enabledLevels,
            currentSettings.hskStandard
        )
        val byNum = pool.associateBy { it.number }
        val resolvedFromSession = sessionNums.mapNotNull { byNum[it] }
        if (resolvedFromSession.isNotEmpty()) {
            resolvedFromSession
        } else {
            buildStudyDeckFor(pool, currentSettings, unmasteredOnly, deckMode, seed)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = HanziCatalog.wordsForLevels(setOf(HskLevel.HSK1), HskStandard.HSK_3_0)
    )

    private val _recallState = MutableStateFlow(
        buildRecallState(HanziCatalog.wordsForLevels(setOf(HskLevel.HSK1), HskStandard.HSK_3_0))
    )
    val recallState: StateFlow<ActiveRecallUiState> = _recallState.asStateFlow()

    private val _matchState = MutableStateFlow(
        buildMatchState(HanziCatalog.wordsForLevels(setOf(HskLevel.HSK1), HskStandard.HSK_3_0))
    )
    val matchState: StateFlow<MatchRoundUiState> = _matchState.asStateFlow()

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
            MakeMeAHanziRepository.preload()
            HanziCatalog.entriesForLevels(setOf(HskLevel.HSK1), HskStandard.HSK_3_0).forEachIndexed { idx, entry ->
                AndroidGlyphRasterizer.getFeaturesForCharacter(entry.character)
                if (idx % 24 == 0) yield()
            }
        }
        viewModelScope.launch {
            var lastStandard: HskStandard? = null
            var lastLevels: Set<HskLevel>? = null
            combine(
                settings.map { it.hskStandard to it.enabledLevels }.distinctUntilChanged(),
                _flashcardDeckMode,
                _onlyUnmastered,
                _flashcardShuffleSeed
            ) { (standard, enabledLevels), deckMode, unmasteredOnly, seed ->
                listOf(standard, enabledLevels, deckMode, unmasteredOnly, seed)
            }.collect { tuple ->
                @Suppress("UNCHECKED_CAST")
                val standard = tuple[0] as HskStandard
                @Suppress("UNCHECKED_CAST")
                val enabledLevels = tuple[1] as Set<HskLevel>
                val deckMode = tuple[2] as FlashcardDeckMode
                val unmasteredOnly = tuple[3] as Boolean
                val seed = tuple[4] as Int

                val currentSettings = settings.value
                val poolAll = HanziCatalog.wordsForLevels(enabledLevels, standard)
                val built = buildStudyDeckFor(poolAll, currentSettings, unmasteredOnly, deckMode, seed)
                _flashcardSessionNumbers.value = built.map { it.number }
                _flashcardIndex.value = 0
                _isFlashcardFlipped.value = false

                val standardChanged = lastStandard != null && lastStandard != standard
                val levelsChanged = lastLevels != null && lastLevels != enabledLevels
                lastStandard = standard
                lastLevels = enabledLevels

                if (standardChanged || levelsChanged) {
                    val pool = getActiveWordStudyPool(enabledLevels, standard)
                    _quizState.value = buildQuizState(pool)
                    _recallState.value = buildRecallState(pool, currentSettings.activeSrsRecords)
                    _matchState.value = buildMatchState(pool, currentSettings.activeSrsRecords)
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
                    val currentRecall = _recallState.value
                    if (currentRecall.challenges.isEmpty()) {
                        _recallState.value = buildRecallState(
                            getActiveWordStudyPool(enabledLevels, standard),
                            currentSettings.activeSrsRecords
                        )
                    }
                    val currentMatch = _matchState.value
                    if (currentMatch.pairs.isEmpty()) {
                        _matchState.value = buildMatchState(
                            getActiveWordStudyPool(enabledLevels, standard),
                            currentSettings.activeSrsRecords
                        )
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
        _selectedWord.value = null
        _selectedEntry.value = entry
        viewModelScope.launch {
            settingsRepository.addRecentCharacter(entry.character)
        }
    }

    fun selectCharacterInWord(entry: HanziEntry) {
        _selectedEntry.value = entry
        viewModelScope.launch {
            settingsRepository.addRecentCharacter(entry.character)
        }
    }

    fun closeCharacterDetail() {
        _selectedEntry.value = null
        _selectedWord.value = null
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
        val resolvedEntries = HanziCatalog.resolveWordEntries(word, standard)
        val firstEntry = resolvedEntries.firstOrNull() ?: return
        _selectedWord.value = word
        _selectedEntry.value = firstEntry
        viewModelScope.launch {
            resolvedEntries.forEach { entry ->
                settingsRepository.addRecentCharacter(entry.character)
            }
        }
    }

    fun openExampleWordDetail(
        hanzi: String,
        pinyin: String,
        meaningRu: String,
        fallbackLevel: HskLevel = HskLevel.HSK1
    ) {
        val standard = settings.value.hskStandard
        val catalogWord = HanziCatalog.findWordByHanzi(hanzi, standard)
        val word = catalogWord ?: HskWordEntry(
            number = 0,
            hskLevel = fallbackLevel,
            hanzi = hanzi.trim(),
            pinyin = pinyin.trim(),
            pos = "",
            meaningEn = "",
            meaningRu = meaningRu.trim()
        )
        openWordDetail(word)
    }

    fun openCharacterByChar(charStr: String) {
        val standard = settings.value.hskStandard
        val trimmed = charStr.trim()
        if (trimmed.length > 1) {
            val catalogWord = HanziCatalog.findWordByHanzi(trimmed, standard)
            if (catalogWord != null) {
                openWordDetail(catalogWord)
                return
            }
        }
        val entry = HanziCatalog.findByCharacter(trimmed, standard)
            ?: trimmed.map { it.toString() }.firstNotNullOfOrNull { HanziCatalog.findByCharacter(it, standard) }
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

    fun setIncludeUni(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setIncludeUni(enabled)
            if (!enabled && _dictionaryLevelFilter.value == HskLevel.UNI) {
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
        when (mode) {
            StudyMode.QUIZ -> if (_quizState.value.questions.isEmpty()) startNewQuizRound()
            StudyMode.RECALL -> if (_recallState.value.challenges.isEmpty()) startNewRecallRound()
            StudyMode.MATCH -> if (_matchState.value.pairs.isEmpty()) startNewMatchRound()
            StudyMode.WRITE -> if (_writePracticeState.value.target == null) nextWritePracticeTarget()
            StudyMode.FLASHCARDS -> Unit
        }
    }

    fun toggleOnlyUnmastered(onlyUnmastered: Boolean) {
        _onlyUnmastered.value = onlyUnmastered
        _flashcardIndex.value = 0
        _isFlashcardFlipped.value = false
    }

    fun setFlashcardDeckMode(mode: FlashcardDeckMode) {
        _flashcardDeckMode.value = mode
        _flashcardIndex.value = 0
        _isFlashcardFlipped.value = false
    }

    fun setFlashcardDirection(direction: FlashcardDirection) {
        _flashcardDirection.value = direction
        _isFlashcardFlipped.value = false
    }

    fun refreshSrsSession() {
        val currentSettings = settings.value
        val pool = HanziCatalog.wordsForLevels(currentSettings.enabledLevels, currentSettings.hskStandard)
        val built = buildStudyDeckFor(
            pool = pool,
            currentSettings = currentSettings,
            unmasteredOnly = _onlyUnmastered.value,
            deckMode = _flashcardDeckMode.value,
            seed = _flashcardShuffleSeed.value
        )
        _flashcardSessionNumbers.value = built.map { it.number }
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

    fun rateFlashcardSrs(word: HskWordEntry, rating: SrsRating) {
        val standard = settings.value.hskStandard
        if (rating == SrsRating.AGAIN && _flashcardDeckMode.value == FlashcardDeckMode.SRS_QUEUE) {
            val currentNums = _flashcardSessionNumbers.value.toMutableList()
            val idx = _flashcardIndex.value
            if (currentNums.isNotEmpty() && idx in currentNums.indices) {
                val reinsertPos = (idx + 4).coerceAtMost(currentNums.size)
                currentNums.add(reinsertPos, word.number)
                _flashcardSessionNumbers.value = currentNums
            }
        }
        viewModelScope.launch {
            settingsRepository.recordSrsReview(word.number, rating, standard)
        }
        nextFlashcard()
    }

    fun markFlashcardResult(word: HskWordEntry, mastered: Boolean) {
        rateFlashcardSrs(word, if (mastered) SrsRating.GOOD else SrsRating.AGAIN)
    }

    fun toggleMastered(entry: HanziEntry) {
        val currentlyMastered = entry.character in settings.value.masteredCharacters
        viewModelScope.launch {
            settingsRepository.setCharacterMastered(entry.character, !currentlyMastered)
        }
    }

    // ==================== Active Recall (Tones & Assembly) ====================

    fun startNewRecallRound() {
        val pool = getActiveWordStudyPool()
        _recallState.value = buildRecallState(pool, settings.value.activeSrsRecords)
    }

    fun answerToneRecall(option: String) {
        val state = _recallState.value
        val challenge = state.currentChallenge as? RecallChallenge.ToneChallenge ?: return
        if (state.isCorrect != null) return

        val isCorrect = option == challenge.correctOption
        val standard = settings.value.hskStandard
        viewModelScope.launch {
            settingsRepository.recordSrsReview(
                wordNumber = challenge.target.number,
                rating = if (isCorrect) SrsRating.GOOD else SrsRating.AGAIN,
                standard = standard
            )
        }
        _recallState.value = state.copy(
            selectedToneOption = option,
            isCorrect = isCorrect,
            correctCount = if (isCorrect) state.correctCount + 1 else state.correctCount,
            streak = if (isCorrect) state.streak + 1 else 0
        )
    }

    fun tapAssembleTile(tile: String) {
        val state = _recallState.value
        val challenge = state.currentChallenge as? RecallChallenge.AssembleChallenge ?: return
        if (state.isAssembleSubmitted) return
        if (state.assembledChars.size >= challenge.target.hanzi.length + 1) return

        _recallState.value = state.copy(assembledChars = state.assembledChars + tile)
    }

    fun backspaceAssemble() {
        val state = _recallState.value
        if (state.isAssembleSubmitted || state.assembledChars.isEmpty()) return
        _recallState.value = state.copy(assembledChars = state.assembledChars.dropLast(1))
    }

    fun clearAssemble() {
        val state = _recallState.value
        if (state.isAssembleSubmitted) return
        _recallState.value = state.copy(assembledChars = emptyList())
    }

    fun submitAssembleRecall() {
        val state = _recallState.value
        val challenge = state.currentChallenge as? RecallChallenge.AssembleChallenge ?: return
        if (state.isAssembleSubmitted || state.assembledChars.isEmpty()) return

        val userWord = state.assembledChars.joinToString("")
        val isCorrect = userWord == challenge.target.hanzi
        val standard = settings.value.hskStandard
        viewModelScope.launch {
            settingsRepository.recordSrsReview(
                wordNumber = challenge.target.number,
                rating = if (isCorrect) SrsRating.GOOD else SrsRating.AGAIN,
                standard = standard
            )
        }
        _recallState.value = state.copy(
            isAssembleSubmitted = true,
            isCorrect = isCorrect,
            correctCount = if (isCorrect) state.correctCount + 1 else state.correctCount,
            streak = if (isCorrect) state.streak + 1 else 0
        )
    }

    fun nextRecallQuestion() {
        val state = _recallState.value
        val nextIdx = state.currentIndex + 1
        if (nextIdx >= state.challenges.size) {
            _recallState.value = state.copy(isRoundFinished = true)
        } else {
            _recallState.value = state.copy(
                currentIndex = nextIdx,
                selectedToneOption = null,
                assembledChars = emptyList(),
                isAssembleSubmitted = false,
                isCorrect = null
            )
        }
    }

    // ==================== Match Pairs ====================

    fun startNewMatchRound() {
        val pool = getActiveWordStudyPool()
        val nextRound = _matchState.value.roundNumber + 1
        _matchState.value = buildMatchState(pool, settings.value.activeSrsRecords, nextRound)
    }

    fun selectMatchLeft(wordNumber: Int) {
        val state = _matchState.value
        if (wordNumber in state.matchedNumbers) return
        val nextLeft = if (state.selectedLeftNumber == wordNumber) null else wordNumber
        evaluateMatchSelection(state.copy(selectedLeftNumber = nextLeft, wrongPairNumbers = null))
    }

    fun selectMatchRight(wordNumber: Int) {
        val state = _matchState.value
        if (wordNumber in state.matchedNumbers) return
        val nextRight = if (state.selectedRightNumber == wordNumber) null else wordNumber
        evaluateMatchSelection(state.copy(selectedRightNumber = nextRight, wrongPairNumbers = null))
    }

    private fun evaluateMatchSelection(updated: MatchRoundUiState) {
        val left = updated.selectedLeftNumber
        val right = updated.selectedRightNumber
        if (left == null || right == null) {
            _matchState.value = updated
            return
        }
        val standard = settings.value.hskStandard
        if (left == right) {
            viewModelScope.launch {
                settingsRepository.recordSrsReview(left, SrsRating.GOOD, standard)
            }
            _matchState.value = updated.copy(
                matchedNumbers = updated.matchedNumbers + left,
                selectedLeftNumber = null,
                selectedRightNumber = null,
                wrongPairNumbers = null
            )
        } else {
            viewModelScope.launch {
                settingsRepository.recordSrsReview(left, SrsRating.HARD, standard)
            }
            _matchState.value = updated.copy(
                selectedLeftNumber = null,
                selectedRightNumber = null,
                wrongPairNumbers = left to right,
                mistakesCount = updated.mistakesCount + 1
            )
        }
    }

    // ==================== Quiz ====================

    fun startNewQuizRound() {
        val pool = getActiveWordStudyPool()
        _quizState.value = buildQuizState(pool)
    }

    fun answerQuiz(optionNumber: Int) {
        val state = _quizState.value
        val question = state.currentQuestion ?: return
        if (state.selectedOptionNumber != null) return

        val isCorrect = optionNumber == question.target.number
        val standard = settings.value.hskStandard
        viewModelScope.launch {
            settingsRepository.recordSrsReview(
                wordNumber = question.target.number,
                rating = if (isCorrect) SrsRating.GOOD else SrsRating.AGAIN,
                standard = standard
            )
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
                    settingsRepository.recordSrsReview(word.number, SrsRating.GOOD, standard)
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

    private fun buildStudyDeckFor(
        pool: List<HskWordEntry>,
        currentSettings: AppSettings,
        unmasteredOnly: Boolean,
        deckMode: FlashcardDeckMode,
        seed: Int
    ): List<HskWordEntry> {
        val masteredSet = currentSettings.activeMasteredWords
        val basePool = if (unmasteredOnly) {
            pool.filter { it.number !in masteredSet }.ifEmpty { pool }
        } else {
            pool
        }
        val ordered = when (deckMode) {
            FlashcardDeckMode.SRS_QUEUE -> SrsEngine.buildSrsQueue(
                pool = basePool,
                records = currentSettings.activeSrsRecords
            )
            FlashcardDeckMode.HARD_ONLY -> SrsEngine.buildSrsQueue(
                pool = basePool,
                records = currentSettings.activeSrsRecords,
                hardOnly = true
            )
            FlashcardDeckMode.ALL_WORDS -> basePool
        }
        return if (seed == 0) ordered else ordered.shuffled(kotlin.random.Random(seed))
    }

    private fun buildRecallState(
        pool: List<HskWordEntry>,
        srsRecords: Map<Int, com.example.domain.SrsCardState> = emptyMap()
    ): ActiveRecallUiState {
        val challenges = StudyTechniquesEngine.buildRecallRound(pool, srsRecords)
        return ActiveRecallUiState(challenges = challenges)
    }

    private fun buildMatchState(
        pool: List<HskWordEntry>,
        srsRecords: Map<Int, com.example.domain.SrsCardState> = emptyMap(),
        roundNumber: Int = 1
    ): MatchRoundUiState {
        val pairs = StudyTechniquesEngine.buildMatchPairs(pool, srsRecords)
        return MatchRoundUiState(
            pairs = pairs,
            rightColumnWords = pairs.shuffled(),
            roundNumber = roundNumber
        )
    }
}
