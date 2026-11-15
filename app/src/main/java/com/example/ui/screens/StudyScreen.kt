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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Style
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.domain.StrokePoint
import com.example.ui.QuizUiState
import com.example.ui.StudyMode
import com.example.ui.WritePracticeState
import com.example.ui.components.HskLevelBadge
import com.example.ui.components.drawTianzigeGrid
import com.example.ui.theme.JadeCeladon

@Composable
fun StudyScreen(
    settings: AppSettings,
    studyMode: StudyMode,
    onlyUnmastered: Boolean,
    studyDeck: List<HskWordEntry>,
    flashcardIndex: Int,
    isFlashcardFlipped: Boolean,
    quizState: QuizUiState,
    writePracticeState: WritePracticeState,
    onSelectHskStandard: (HskStandard) -> Unit = {},
    onSelectStudyMode: (StudyMode) -> Unit,
    onToggleOnlyUnmastered: (Boolean) -> Unit,
    onFlipFlashcard: () -> Unit,
    onNextFlashcard: () -> Unit,
    onPrevFlashcard: () -> Unit,
    onShuffleFlashcards: () -> Unit,
    onMarkFlashcardResult: (HskWordEntry, Boolean) -> Unit,
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
    val activeWordPool = HanziCatalog.wordsForLevels(settings.enabledLevels, standard)
    val masteredInActive = activeWordPool.count { it.number in activeMastered }
    val totalInActive = activeWordPool.size.coerceAtLeast(1)

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
            // Header + Mastery Progress Card
            StudyProgressCard(
                settings = settings,
                masteredCount = masteredInActive,
                totalCount = totalInActive,
                onlyUnmastered = onlyUnmastered,
                onSelectHskStandard = onSelectHskStandard,
                onToggleOnlyUnmastered = onToggleOnlyUnmastered
            )

            // Mode Switcher
            StudyModeBar(
                selectedMode = studyMode,
                onSelectMode = onSelectStudyMode
            )

            // Active Mode Content
            when (studyMode) {
                StudyMode.FLASHCARDS -> {
                    FlashcardsSection(
                        deck = studyDeck,
                        currentIndex = flashcardIndex,
                        isFlipped = isFlashcardFlipped,
                        masteredWords = activeMastered,
                        hskStandard = standard,
                        onFlip = onFlipFlashcard,
                        onPrev = onPrevFlashcard,
                        onNext = onNextFlashcard,
                        onShuffle = onShuffleFlashcards,
                        onMarkResult = onMarkFlashcardResult,
                        onOpenCharacterDetail = onOpenCharacterDetail,
                        onOpenWordDetail = onOpenWordDetail
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
}

@Composable
private fun StudyProgressCard(
    settings: AppSettings,
    masteredCount: Int,
    totalCount: Int,
    onlyUnmastered: Boolean,
    onSelectHskStandard: (HskStandard) -> Unit,
    onToggleOnlyUnmastered: (Boolean) -> Unit
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
                        text = "Заучивание слов · ${settings.hskStandard.shortLabel} · $levelsLabel",
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

                FilterChip(
                    selected = onlyUnmastered,
                    onClick = { onToggleOnlyUnmastered(!onlyUnmastered) },
                    label = { Text("Только новые") },
                    modifier = Modifier.testTag("study_only_new_chip")
                )
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
private fun StudyModeBar(
    selectedMode: StudyMode,
    onSelectMode: (StudyMode) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StudyMode.entries.forEach { mode ->
            val selected = mode == selectedMode
            val icon: ImageVector = when (mode) {
                StudyMode.FLASHCARDS -> Icons.Filled.Style
                StudyMode.QUIZ -> Icons.Filled.Quiz
                StudyMode.WRITE -> Icons.Filled.Brush
            }

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onSelectMode(mode) }
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
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp),
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
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlashcardsSection(
    deck: List<HskWordEntry>,
    currentIndex: Int,
    isFlipped: Boolean,
    masteredWords: Set<Int>,
    hskStandard: HskStandard,
    onFlip: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onShuffle: () -> Unit,
    onMarkResult: (HskWordEntry, Boolean) -> Unit,
    onOpenCharacterDetail: (HanziEntry) -> Unit,
    onOpenWordDetail: (HskWordEntry) -> Unit
) {
    if (deck.isEmpty()) return
    val safeIndex = currentIndex.coerceIn(0, deck.lastIndex)
    val word = deck[safeIndex]
    val isMastered = word.number in masteredWords
    val charEntries = word.hanzi
        .mapNotNull { HanziCatalog.findByCharacter(it.toString(), hskStandard) }
        .distinctBy { it.character }

    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 360),
        label = "flashcard_flip"
    )

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Counter + Shuffle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Слово ${safeIndex + 1} из ${deck.size}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isMastered) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = "Изучено",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(Modifier.width(6.dp))
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
                .heightIn(min = 290.dp)
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
                        .heightIn(min = 290.dp)
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
                        if (word.hanzi.length == 1) {
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
                        text = "Нажмите, чтобы увидеть пиньинь и перевод",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            } else {
                // Back face (counter-rotated so text reads normally)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 290.dp)
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

        // Rating actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = { onMarkResult(word, false) },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .testTag("flashcard_repeat_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text("Повторить")
            }

            Button(
                onClick = { onMarkResult(word, true) },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .testTag("flashcard_know_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = JadeCeladon)
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text("Знаю!")
            }
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
                Text("Дальше")
                Spacer(Modifier.width(6.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Следующая")
            }
        }
    }
}
