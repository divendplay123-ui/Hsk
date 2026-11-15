package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppSettings
import com.example.data.model.HanziEntry
import com.example.data.model.HskStandard
import com.example.data.model.HskWordEntry
import com.example.data.repository.HanziCatalog
import com.example.domain.SrsCardState
import com.example.domain.SrsEngine
import com.example.domain.SrsRating
import com.example.domain.StrokePoint
import com.example.ui.ActiveRecallUiState
import com.example.ui.FlashcardDeckMode
import com.example.ui.FlashcardDirection
import com.example.ui.MatchRoundUiState
import com.example.ui.QuizUiState
import com.example.ui.StudyMode
import com.example.ui.WritePracticeState
import com.example.ui.components.HskLevelBadge
import com.example.ui.components.drawTianzigeGrid
import com.example.ui.theme.AmberGold
import com.example.ui.theme.AmberSoft
import com.example.ui.theme.IndigoInk
import com.example.ui.theme.IndigoSoft
import com.example.ui.theme.JadeCeladon
import com.example.ui.theme.JadeSoft
import com.example.ui.theme.VermilionSeal
import com.example.ui.theme.VermilionSoft

@Composable
fun StudyScreen(
    settings: AppSettings,
    studyMode: StudyMode,
    onlyUnmastered: Boolean,
    flashcardDeckMode: FlashcardDeckMode = FlashcardDeckMode.SRS_QUEUE,
    flashcardDirection: FlashcardDirection = FlashcardDirection.HANZI_TO_RU,
    studyDeck: List<HskWordEntry>,
    flashcardIndex: Int,
    isFlashcardFlipped: Boolean,
    recallState: ActiveRecallUiState = ActiveRecallUiState(),
    matchState: MatchRoundUiState = MatchRoundUiState(),
    quizState: QuizUiState,
    writePracticeState: WritePracticeState,
    onSelectHskStandard: (HskStandard) -> Unit = {},
    onSelectStudyMode: (StudyMode) -> Unit,
    onToggleOnlyUnmastered: (Boolean) -> Unit,
    onSelectDeckMode: (FlashcardDeckMode) -> Unit = {},
    onToggleDirection: (FlashcardDirection) -> Unit = {},
    onRefreshSrsSession: () -> Unit = {},
    onFlipFlashcard: () -> Unit,
    onNextFlashcard: () -> Unit,
    onPrevFlashcard: () -> Unit,
    onShuffleFlashcards: () -> Unit,
    onRateFlashcardSrs: (HskWordEntry, SrsRating) -> Unit = { _, _ -> },
    onAnswerToneRecall: (String) -> Unit = {},
    onTapAssembleTile: (String) -> Unit = {},
    onBackspaceAssemble: () -> Unit = {},
    onClearAssemble: () -> Unit = {},
    onSubmitAssembleRecall: () -> Unit = {},
    onNextRecallQuestion: () -> Unit = {},
    onStartNewRecallRound: () -> Unit = {},
    onSelectMatchLeft: (Int) -> Unit = {},
    onSelectMatchRight: (Int) -> Unit = {},
    onStartNewMatchRound: () -> Unit = {},
    onAnswerQuiz: (Int) -> Unit,
    onNextQuizQuestion: () -> Unit,
    onStartNewQuizRound: () -> Unit,
    onAddWriteStroke: (List<StrokePoint>, Float) -> Unit,
    onUndoWriteStroke: () -> Unit,
    onClearWriteCanvas: () -> Unit,
    onCheckWritePractice: () -> Unit,
    onToggleWriteHint: () -> Unit,
    onSelectWriteCharIndex: (Int) -> Unit,
    onNextWriteTarget: () -> Unit,
    onOpenCharacterDetail: (HanziEntry) -> Unit,
    onOpenWordDetail: (HskWordEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    val standard = settings.hskStandard
    val activeMastered = settings.activeMasteredWords
    val activeSrs = settings.activeSrsRecords
    val activeWordPool = HanziCatalog.wordsForLevels(settings.enabledLevels, standard)
    val nowMillis = System.currentTimeMillis()
    val dueCount = activeWordPool.count { SrsEngine.isDue(activeSrs[it.number], nowMillis) }
    val consolidatedCount = activeWordPool.count { SrsEngine.isMasteredLongTerm(activeSrs[it.number]) }
    val masteredInActive = activeWordPool.count { it.number in activeMastered }
    val totalInActive = activeWordPool.size.coerceAtLeast(1)

    val todayEpochDay = nowMillis / 86_400_000L
    val todayReviews = if (settings.dailyReviewsEpochDay == todayEpochDay) settings.dailyReviewsCount else 0
    val activeStreak = if (settings.lastStudyEpochDay >= todayEpochDay - 1L) settings.studyStreakDays else 0

    var showTechniquesGuide by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.widthIn(max = 620.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            StudyProgressCard(
                settings = settings,
                masteredCount = masteredInActive,
                totalCount = totalInActive,
                dueCount = dueCount,
                consolidatedCount = consolidatedCount,
                todayReviews = todayReviews,
                streakDays = activeStreak,
                onlyUnmastered = onlyUnmastered,
                onSelectHskStandard = onSelectHskStandard,
                onToggleOnlyUnmastered = onToggleOnlyUnmastered,
                onOpenGuide = { showTechniquesGuide = true }
            )

            StudyModeBar(
                selectedMode = studyMode,
                onSelectMode = onSelectStudyMode
            )

            when (studyMode) {
                StudyMode.FLASHCARDS -> {
                    FlashcardsSection(
                        deck = studyDeck,
                        currentIndex = flashcardIndex,
                        isFlipped = isFlashcardFlipped,
                        deckMode = flashcardDeckMode,
                        direction = flashcardDirection,
                        srsRecords = activeSrs,
                        masteredWords = activeMastered,
                        hskStandard = standard,
                        onSelectDeckMode = onSelectDeckMode,
                        onToggleDirection = onToggleDirection,
                        onRefreshSession = onRefreshSrsSession,
                        onFlip = onFlipFlashcard,
                        onPrev = onPrevFlashcard,
                        onNext = onNextFlashcard,
                        onShuffle = onShuffleFlashcards,
                        onRateSrs = onRateFlashcardSrs,
                        onOpenCharacterDetail = onOpenCharacterDetail,
                        onOpenWordDetail = onOpenWordDetail
                    )
                }

                StudyMode.RECALL -> {
                    ActiveRecallSection(
                        state = recallState,
                        onAnswerTone = onAnswerToneRecall,
                        onTapAssembleTile = onTapAssembleTile,
                        onBackspaceAssemble = onBackspaceAssemble,
                        onClearAssemble = onClearAssemble,
                        onSubmitAssemble = onSubmitAssembleRecall,
                        onNextQuestion = onNextRecallQuestion,
                        onStartNewRound = onStartNewRecallRound,
                        onOpenWordDetail = onOpenWordDetail
                    )
                }

                StudyMode.MATCH -> {
                    MatchPairsSection(
                        state = matchState,
                        onSelectLeft = onSelectMatchLeft,
                        onSelectRight = onSelectMatchRight,
                        onStartNewRound = onStartNewMatchRound
                    )
                }

                StudyMode.QUIZ -> {
                    QuizSection(
                        quizState = quizState,
                        onAnswer = onAnswerQuiz,
                        onNextQuestion = onNextQuizQuestion,
                        onStartNewRound = onStartNewQuizRound,
                        onOpenWordDetail = onOpenWordDetail
                    )
                }

                StudyMode.WRITE -> {
                    WritePracticeSection(
                        state = writePracticeState,
                        showGridGuide = settings.showGridGuide,
                        hskStandard = standard,
                        onStrokeCompleted = onAddWriteStroke,
                        onUndo = onUndoWriteStroke,
                        onClear = onClearWriteCanvas,
                        onCheck = onCheckWritePractice,
                        onToggleHint = onToggleWriteHint,
                        onSelectCharIndex = onSelectWriteCharIndex,
                        onNextTarget = onNextWriteTarget,
                        onOpenDetail = onOpenCharacterDetail
                    )
                }
            }
        }
    }

    if (showTechniquesGuide) {
        StudyTechniquesGuideSheet(onDismiss = { showTechniquesGuide = false })
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StudyProgressCard(
    settings: AppSettings,
    masteredCount: Int,
    totalCount: Int,
    dueCount: Int,
    consolidatedCount: Int,
    todayReviews: Int,
    streakDays: Int,
    onlyUnmastered: Boolean,
    onSelectHskStandard: (HskStandard) -> Unit,
    onToggleOnlyUnmastered: (Boolean) -> Unit,
    onOpenGuide: () -> Unit
) {
    val progress = (masteredCount.toFloat() / totalCount.toFloat()).coerceIn(0f, 1f)
    val levelsLabel = settings.enabledLevels.sortedBy { it.number }.joinToString(" + ") { it.label }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Заучивание · ${settings.hskStandard.shortLabel} · $levelsLabel",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Изучено: $masteredCount из $totalCount слов",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onOpenGuide,
                    modifier = Modifier.testTag("study_techniques_help_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.HelpOutline,
                        contentDescription = "Как работают техники запоминания",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // SM-2 Metrics Strip
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MetricPill(
                    text = "К повтору: $dueCount",
                    bg = if (dueCount > 0) VermilionSoft else MaterialTheme.colorScheme.surfaceVariant,
                    fg = if (dueCount > 0) VermilionSeal else MaterialTheme.colorScheme.onSurfaceVariant
                )
                MetricPill(
                    text = "Закреплено: $consolidatedCount",
                    bg = JadeSoft,
                    fg = JadeCeladon
                )
                MetricPill(
                    text = "Сегодня: $todayReviews",
                    bg = IndigoSoft,
                    fg = IndigoInk
                )
                if (streakDays > 0) {
                    MetricPill(
                        text = "Серия: $streakDays дн.",
                        bg = AmberSoft,
                        fg = AmberGold
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = settings.hskStandard == HskStandard.HSK_3_0,
                    onClick = { onSelectHskStandard(HskStandard.HSK_3_0) },
                    label = { Text("HSK 3.0") },
                    modifier = Modifier.testTag("study_std_hsk30")
                )
                FilterChip(
                    selected = settings.hskStandard == HskStandard.HSK_2_0,
                    onClick = { onSelectHskStandard(HskStandard.HSK_2_0) },
                    label = { Text("HSK 2.0") },
                    modifier = Modifier.testTag("study_std_hsk20")
                )
                Spacer(Modifier.weight(1f))
                FilterChip(
                    selected = onlyUnmastered,
                    onClick = { onToggleOnlyUnmastered(!onlyUnmastered) },
                    label = { Text("Только новые") },
                    modifier = Modifier.testTag("study_only_new_chip")
                )
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = JadeCeladon,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            if (onlyUnmastered && masteredCount >= totalCount) {
                Text(
                    text = "Все активные слова отмечены как изученные — показываем весь набор для повторения.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun MetricPill(
    text: String,
    bg: Color,
    fg: Color
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bg
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = fg,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun StudyModeBar(
    selectedMode: StudyMode,
    onSelectMode: (StudyMode) -> Unit
) {
    val firstRow = listOf(StudyMode.FLASHCARDS, StudyMode.QUIZ, StudyMode.WRITE)
    val secondRow = listOf(StudyMode.RECALL, StudyMode.MATCH)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            firstRow.forEach { mode ->
                StudyModeChip(
                    mode = mode,
                    selected = mode == selectedMode,
                    onClick = { onSelectMode(mode) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            secondRow.forEach { mode ->
                StudyModeChip(
                    mode = mode,
                    selected = mode == selectedMode,
                    onClick = { onSelectMode(mode) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun StudyModeChip(
    mode: StudyMode,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val icon: ImageVector = when (mode) {
        StudyMode.FLASHCARDS -> Icons.Filled.Schedule
        StudyMode.RECALL -> Icons.Filled.Psychology
        StudyMode.MATCH -> Icons.Filled.GridView
        StudyMode.QUIZ -> Icons.Filled.Quiz
        StudyMode.WRITE -> Icons.Filled.Brush
    }

    Surface(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("study_mode_${mode.name.lowercase()}"),
        shape = RoundedCornerShape(14.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        border = BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = if (selected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Spacer(Modifier.width(5.dp))
            Text(
                text = mode.label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (selected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlashcardsSection(
    deck: List<HskWordEntry>,
    currentIndex: Int,
    isFlipped: Boolean,
    deckMode: FlashcardDeckMode,
    direction: FlashcardDirection,
    srsRecords: Map<Int, SrsCardState>,
    masteredWords: Set<Int>,
    hskStandard: HskStandard,
    onSelectDeckMode: (FlashcardDeckMode) -> Unit,
    onToggleDirection: (FlashcardDirection) -> Unit,
    onRefreshSession: () -> Unit,
    onFlip: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onShuffle: () -> Unit,
    onRateSrs: (HskWordEntry, SrsRating) -> Unit,
    onOpenCharacterDetail: (HanziEntry) -> Unit,
    onOpenWordDetail: (HskWordEntry) -> Unit
) {
    if (deck.isEmpty()) return
    val safeIndex = currentIndex.coerceIn(0, deck.lastIndex)
    val word = deck[safeIndex]
    val srsState = srsRecords[word.number]
    val isMastered = word.number in masteredWords
    val charEntries = word.hanzi
        .mapNotNull { HanziCatalog.findByCharacter(it.toString(), hskStandard) }
        .distinctBy { it.character }

    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 360),
        label = "flashcard_flip"
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Queue Filter + Active Recall Direction Selector
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(FlashcardDeckMode.entries) { mode ->
                FilterChip(
                    selected = deckMode == mode,
                    onClick = { onSelectDeckMode(mode) },
                    label = { Text(mode.label) },
                    modifier = Modifier.testTag("srs_deck_mode_${mode.name.lowercase()}")
                )
            }
            item {
                val nextDir = if (direction == FlashcardDirection.HANZI_TO_RU) {
                    FlashcardDirection.RU_TO_HANZI
                } else {
                    FlashcardDirection.HANZI_TO_RU
                }
                FilterChip(
                    selected = direction == FlashcardDirection.RU_TO_HANZI,
                    onClick = { onToggleDirection(nextDir) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.SwapHoriz,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = { Text(direction.label) },
                    modifier = Modifier.testTag("flashcard_direction_chip")
                )
            }
        }

        // Counter + SRS Status + Refresh/Shuffle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Карточка ${safeIndex + 1} из ${deck.size}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                SrsStatusBadge(state = srsState, isMasteredFallback = isMastered)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (deckMode == FlashcardDeckMode.SRS_QUEUE) {
                    IconButton(
                        onClick = onRefreshSession,
                        modifier = Modifier.testTag("flashcard_refresh_srs_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Обновить очередь SM-2"
                        )
                    }
                }
                IconButton(
                    onClick = onShuffle,
                    modifier = Modifier.testTag("flashcard_shuffle_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Shuffle,
                        contentDescription = "Перемешать карточки"
                    )
                }
            }
        }

        // 3D Flip Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 280.dp)
                .graphicsLayer {
                    rotationY = rotation
                    cameraDistance = 12f * density
                }
                .clip(RoundedCornerShape(24.dp))
                .clickable(onClick = onFlip)
                .testTag("flashcard_surface"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline)
        ) {
            if (rotation <= 90f) {
                // Front face
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 280.dp)
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HskLevelBadge(level = word.hskLevel)
                        Text(
                            text = if (word.pos.isNotBlank()) "№${word.number} · ${word.pos}" else "№${word.number}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.Center)
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (direction == FlashcardDirection.RU_TO_HANZI) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = word.meaningRu,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "Вспомните иероглиф и пиньинь",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        } else if (word.hanzi.length == 1) {
                            Box(
                                modifier = Modifier.size(144.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                val gridColor = MaterialTheme.colorScheme.outlineVariant
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    drawTianzigeGrid(gridColor)
                                }
                                Text(
                                    text = word.hanzi,
                                    fontSize = 90.sp,
                                    fontFamily = FontFamily.Serif,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        } else {
                            val wordFontSize = when (word.hanzi.length) {
                                2 -> 68.sp
                                3 -> 52.sp
                                else -> 40.sp
                            }
                            Text(
                                text = word.hanzi,
                                fontSize = wordFontSize,
                                fontFamily = FontFamily.Serif,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                                lineHeight = wordFontSize * 1.15f
                            )
                        }
                    }

                    Text(
                        text = "Нажмите на карточку, чтобы проверить себя",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            } else {
                // Back face
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 280.dp)
                        .graphicsLayer { rotationY = 180f }
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HskLevelBadge(level = word.hskLevel)
                        Text(
                            text = if (word.pos.isNotBlank()) "№${word.number} · ${word.pos}" else "№${word.number}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = word.hanzi,
                            fontSize = if (word.hanzi.length <= 2) 44.sp else 34.sp,
                            fontFamily = FontFamily.Serif,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = word.pinyin,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = word.meaningRu,
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center
                        )

                        if (charEntries.size > 1) {
                            Spacer(Modifier.height(4.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                charEntries.forEach { ch ->
                                    Surface(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable { onOpenCharacterDetail(ch) },
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                    ) {
                                        Text(
                                            text = "${ch.character} · ${ch.pinyin}",
                                            style = MaterialTheme.typography.labelMedium,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = { onOpenWordDetail(word) },
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("flashcard_watch_strokes_button"),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Написание и разбор")
                    }
                }
            }
        }

        // 4 SM-2 Spaced Repetition Rating Buttons with live interval previews
        val nowMillis = System.currentTimeMillis()
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SrsRatingButton(
                label = "Снова",
                interval = SrsEngine.formatIntervalPreview(srsState, SrsRating.AGAIN, nowMillis),
                bg = VermilionSoft,
                fg = VermilionSeal,
                onClick = { onRateSrs(word, SrsRating.AGAIN) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("flashcard_repeat_button")
            )
            SrsRatingButton(
                label = "Трудно",
                interval = SrsEngine.formatIntervalPreview(srsState, SrsRating.HARD, nowMillis),
                bg = AmberSoft,
                fg = AmberGold,
                onClick = { onRateSrs(word, SrsRating.HARD) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("flashcard_hard_button")
            )
            SrsRatingButton(
                label = "Хорошо",
                interval = SrsEngine.formatIntervalPreview(srsState, SrsRating.GOOD, nowMillis),
                bg = JadeCeladon,
                fg = Color.White,
                onClick = { onRateSrs(word, SrsRating.GOOD) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("flashcard_know_button")
            )
            SrsRatingButton(
                label = "Легко",
                interval = SrsEngine.formatIntervalPreview(srsState, SrsRating.EASY, nowMillis),
                bg = IndigoSoft,
                fg = IndigoInk,
                onClick = { onRateSrs(word, SrsRating.EASY) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("flashcard_easy_button")
            )
        }

        // Prev / Next navigation
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedButton(
                onClick = onPrev,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("flashcard_prev_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Предыдущая")
                Spacer(Modifier.width(6.dp))
                Text("Назад")
            }
            OutlinedButton(
                onClick = onNext,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("flashcard_next_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Пропустить")
                Spacer(Modifier.width(6.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Следующая")
            }
        }
    }
}

@Composable
private fun SrsStatusBadge(
    state: SrsCardState?,
    isMasteredFallback: Boolean
) {
    val now = System.currentTimeMillis()
    val (label, bg, fg) = when {
        state == null && isMasteredFallback -> Triple("Изучено", JadeSoft, JadeCeladon)
        state == null -> Triple("Новое", IndigoSoft, IndigoInk)
        SrsEngine.isDue(state, now) -> Triple("Пора повторить", VermilionSoft, VermilionSeal)
        SrsEngine.isMasteredLongTerm(state) -> Triple(
            "Интервал: ${SrsEngine.formatDaysLabel(state.intervalDays)}",
            JadeSoft,
            JadeCeladon
        )
        else -> Triple(
            "В процессе · ${SrsEngine.formatDaysLabel(state.intervalDays)}",
            AmberSoft,
            AmberGold
        )
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bg
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = fg,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun SrsRatingButton(
    label: String,
    interval: String,
    bg: Color,
    fg: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 54.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = bg, contentColor = fg),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = interval,
                style = MaterialTheme.typography.labelSmall,
                color = fg.copy(alpha = 0.85f),
                maxLines = 1
            )
        }
    }
}
