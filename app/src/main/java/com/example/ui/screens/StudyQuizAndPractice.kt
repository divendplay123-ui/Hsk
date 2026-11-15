package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.HanziEntry
import com.example.data.model.HskStandard
import com.example.data.model.HskWordEntry
import com.example.data.repository.HanziCatalog
import com.example.domain.StrokePoint
import com.example.ui.QuizQuestionType
import com.example.ui.QuizUiState
import com.example.ui.WritePracticeState
import com.example.ui.components.AnimatedHanziBox
import com.example.ui.components.HskLevelBadge
import com.example.ui.components.drawTianzigeGrid
import com.example.ui.theme.JadeCeladon
import com.example.ui.theme.JadeSoft
import com.example.ui.theme.VermilionSeal
import com.example.ui.theme.VermilionSoft
import kotlin.math.roundToInt

@Composable
fun QuizSection(
    quizState: QuizUiState,
    onAnswer: (Int) -> Unit,
    onNextQuestion: () -> Unit,
    onStartNewRound: () -> Unit,
    onOpenWordDetail: (HskWordEntry) -> Unit
) {
    if (quizState.isRoundFinished || quizState.questions.isEmpty()) {
        val total = quizState.questions.size.coerceAtLeast(1)
        val pct = ((quizState.correctCount * 100f) / total).roundToInt()

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.EmojiEvents,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = JadeCeladon
                )
                Text(
                    text = "Раунд завершён!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Верных ответов: ${quizState.correctCount} из $total ($pct%)",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Button(
                    onClick = onStartNewRound,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("quiz_new_round_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Начать новый раунд")
                }
            }
        }
        return
    }

    val question = quizState.currentQuestion ?: return
    val hasAnswered = quizState.selectedOptionNumber != null
    val progress = (quizState.currentIndex + 1).toFloat() / quizState.questions.size.toFloat()

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Question Counter & Streak
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Вопрос ${quizState.currentIndex + 1} из ${quizState.questions.size}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer
            ) {
                Text(
                    text = "Серия: ${quizState.streak}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
        )

        // Prompt Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HskLevelBadge(level = question.target.hskLevel)
                    Text(
                        text = if (question.type == QuizQuestionType.WORD_TO_MEANING) {
                            "№${question.target.number} · Выберите перевод"
                        } else {
                            "№${question.target.number} · Выберите слово"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (question.type == QuizQuestionType.WORD_TO_MEANING) {
                    val promptSize = when (question.target.hanzi.length) {
                        1 -> 80.sp
                        2 -> 64.sp
                        3 -> 48.sp
                        else -> 38.sp
                    }
                    Text(
                        text = question.target.hanzi,
                        fontSize = promptSize,
                        fontFamily = FontFamily.Serif,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                } else {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = question.target.meaningRu,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = question.target.pinyin,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(6.dp))
                }
            }
        }

        // Answer Options
        if (question.type == QuizQuestionType.WORD_TO_MEANING) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                question.options.forEachIndexed { idx, opt ->
                    val isSelected = quizState.selectedOptionNumber == opt.number
                    val isCorrect = opt.number == question.target.number
                    val (bg, border) = quizOptionColors(hasAnswered, isSelected, isCorrect)

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 54.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable(enabled = !hasAnswered) { onAnswer(opt.number) }
                            .testTag("quiz_option_$idx"),
                        shape = RoundedCornerShape(14.dp),
                        color = bg,
                        border = BorderStroke(1.5.dp, border)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = opt.meaningRu,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = opt.pinyin,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            if (hasAnswered && isCorrect) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = "Верный ответ",
                                    tint = JadeCeladon
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // 2x2 grid of Word options
            val rows = question.options.chunked(2)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                rows.forEachIndexed { rIdx, pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        pair.forEachIndexed { cIdx, opt ->
                            val isSelected = quizState.selectedOptionNumber == opt.number
                            val isCorrect = opt.number == question.target.number
                            val (bg, border) = quizOptionColors(hasAnswered, isSelected, isCorrect)
                            val optionFontSize = when (opt.hanzi.length) {
                                1 -> 44.sp
                                2 -> 34.sp
                                3 -> 26.sp
                                else -> 22.sp
                            }

                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 96.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable(enabled = !hasAnswered) { onAnswer(opt.number) }
                                    .testTag("quiz_option_${rIdx * 2 + cIdx}"),
                                shape = RoundedCornerShape(16.dp),
                                color = bg,
                                border = BorderStroke(1.5.dp, border)
                            ) {
                                Box(
                                    modifier = Modifier.padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = opt.hanzi,
                                        fontSize = optionFontSize,
                                        fontFamily = FontFamily.Serif,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Next Question & Inspect Animation after answering
        if (hasAnswered) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { onOpenWordDetail(question.target) },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("quiz_inspect_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Написание")
                }

                Button(
                    onClick = onNextQuestion,
                    modifier = Modifier
                        .weight(1.3f)
                        .heightIn(min = 48.dp)
                        .testTag("quiz_next_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        if (quizState.currentIndex + 1 >= quizState.questions.size) "Итоги"
                        else "Следующий"
                    )
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                }
            }
        }
    }
}

@Composable
private fun quizOptionColors(
    hasAnswered: Boolean,
    isSelected: Boolean,
    isCorrect: Boolean
): Pair<Color, Color> {
    if (!hasAnswered) {
        return MaterialTheme.colorScheme.surface to MaterialTheme.colorScheme.outlineVariant
    }
    return when {
        isCorrect -> JadeSoft to JadeCeladon
        isSelected -> VermilionSoft to VermilionSeal
        else -> MaterialTheme.colorScheme.surface to MaterialTheme.colorScheme.outlineVariant
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WritePracticeSection(
    state: WritePracticeState,
    showGridGuide: Boolean,
    hskStandard: HskStandard = HskStandard.HSK_3_0,
    onStrokeCompleted: (List<StrokePoint>, Float) -> Unit,
    onUndo: () -> Unit,
    onClear: () -> Unit,
    onCheck: () -> Unit,
    onToggleHint: () -> Unit,
    onSelectCharIndex: (Int) -> Unit,
    onNextTarget: () -> Unit,
    onOpenDetail: (HanziEntry) -> Unit
) {
    val target = state.target ?: return
    val targetWord = state.targetWord
    val wordChars = remember(targetWord, hskStandard) {
        targetWord?.hanzi?.mapNotNull { HanziCatalog.findByCharacter(it.toString(), hskStandard) } ?: listOf(target)
    }

    val activePathPoints = remember(target.character) { mutableStateListOf<StrokePoint>() }
    var canvasDimensionPx by remember { mutableFloatStateOf(600f) }

    val inkColor = MaterialTheme.colorScheme.onSurface
    val gridColor = MaterialTheme.colorScheme.outline
    val surfaceColor = MaterialTheme.colorScheme.surface
    val warmTint = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Target Word + Character Prompt Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            HskLevelBadge(level = targetWord?.hskLevel ?: target.hskLevel)
                            if (targetWord != null) {
                                Text(
                                    text = "Слово №${targetWord.number}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "· Черт: ${target.strokeCount}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = if (targetWord != null) {
                                "Слово: ${targetWord.meaningRu}"
                            } else {
                                "Напишите: ${target.meaningRu}"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (targetWord != null) {
                                "${targetWord.pinyin} · Сейчас пишем: «${target.pinyin}» (${target.meaningRu})"
                            } else {
                                "${target.pinyin} · Ключ: ${target.radical}"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    FilledTonalButton(
                        onClick = onToggleHint,
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("write_hint_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = if (state.showHintAnimation) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = "Подсказка написания",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(if (state.showHintAnimation) "Скрыть" else "Подсказка")
                    }
                }

                if (wordChars.size > 1) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        wordChars.forEachIndexed { idx, ch ->
                            val selected = idx == state.activeCharIndex
                            FilterChip(
                                selected = selected,
                                onClick = { onSelectCharIndex(idx) },
                                label = {
                                    Text(
                                        text = if (state.showHintAnimation || selected) {
                                            "${idx + 1}. ${ch.character} (${ch.pinyin})"
                                        } else {
                                            "${idx + 1}-й иерогл. (${ch.pinyin})"
                                        }
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }

        // Animated Hint Card when toggled
        if (state.showHintAnimation) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                AnimatedHanziBox(
                    character = target.character,
                    strokeCount = target.strokeCount,
                    replayTrigger = 0,
                    onActiveStrokeIndex = {},
                    modifier = Modifier
                        .size(118.dp)
                        .clickable { onOpenDetail(target) }
                )
            }
        }

        // Check Result Banner
        state.checkResult?.let { res ->
            val bg = if (res.isSuccess) JadeSoft else VermilionSoft
            val fg = if (res.isSuccess) JadeCeladon else VermilionSeal
            val msg = if (res.isSuccess) {
                "Отлично! «${target.character}» распознан (${(res.confidence * 100).roundToInt()}%)."
            } else {
                val guessText = res.bestGuess?.let { "Больше похоже на «${it.character}» (${it.meaningRu})." }
                    ?: "Попробуйте ещё раз или посмотрите подсказку."
                "Пока не совпало с «${target.character}». $guessText"
            }
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = bg,
                border = BorderStroke(1.dp, fg.copy(alpha = 0.45f))
            ) {
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = fg,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        // Drawing Pad
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 380.dp)
                    .aspectRatio(1f),
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.radialGradient(listOf(surfaceColor, warmTint)))
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("write_practice_canvas")
                            .onSizeChanged { s ->
                                if (s.width > 0) canvasDimensionPx = s.width.toFloat()
                            }
                            .pointerInput(target.character) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        activePathPoints.clear()
                                        activePathPoints.add(StrokePoint(offset.x, offset.y))
                                    },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        activePathPoints.add(
                                            StrokePoint(change.position.x, change.position.y)
                                        )
                                    },
                                    onDragEnd = {
                                        if (activePathPoints.isNotEmpty()) {
                                            onStrokeCompleted(
                                                activePathPoints.toList(),
                                                canvasDimensionPx
                                            )
                                            activePathPoints.clear()
                                        }
                                    },
                                    onDragCancel = {
                                        activePathPoints.clear()
                                    }
                                )
                            }
                    ) {
                        if (showGridGuide) drawTianzigeGrid(gridColor)

                        val strokeStyle = Stroke(
                            width = size.minDimension * 0.036f,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )

                        fun drawPts(pts: List<StrokePoint>, color: Color) {
                            if (pts.isEmpty()) return
                            if (pts.size == 1) {
                                drawCircle(
                                    color = color,
                                    radius = strokeStyle.width / 2f,
                                    center = androidx.compose.ui.geometry.Offset(pts[0].x, pts[0].y)
                                )
                                return
                            }
                            val path = Path().apply {
                                moveTo(pts[0].x, pts[0].y)
                                for (i in 1 until pts.size) {
                                    val prev = pts[i - 1]
                                    val curr = pts[i]
                                    quadraticTo(
                                        prev.x,
                                        prev.y,
                                        (prev.x + curr.x) * 0.5f,
                                        (prev.y + curr.y) * 0.5f
                                    )
                                }
                                lineTo(pts.last().x, pts.last().y)
                            }
                            drawPath(path = path, color = color, style = strokeStyle)
                        }

                        state.strokes.forEach { drawPts(it.points, inkColor) }
                        if (activePathPoints.isNotEmpty()) drawPts(activePathPoints, inkColor.copy(alpha = 0.85f))
                    }
                }
            }
        }

        // Practice Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onUndo,
                enabled = state.strokes.isNotEmpty(),
                modifier = Modifier
                    .weight(0.9f)
                    .heightIn(min = 48.dp)
                    .testTag("write_undo_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Назад", modifier = Modifier.size(18.dp))
            }

            OutlinedButton(
                onClick = onClear,
                enabled = state.strokes.isNotEmpty(),
                modifier = Modifier
                    .weight(0.9f)
                    .heightIn(min = 48.dp)
                    .testTag("write_clear_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.DeleteOutline, contentDescription = "Сброс", modifier = Modifier.size(18.dp))
            }

            Button(
                onClick = onCheck,
                enabled = state.strokes.isNotEmpty(),
                modifier = Modifier
                    .weight(1.6f)
                    .heightIn(min = 48.dp)
                    .testTag("write_check_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Проверить")
            }

            FilledTonalButton(
                onClick = onNextTarget,
                modifier = Modifier
                    .weight(1.2f)
                    .heightIn(min = 48.dp)
                    .testTag("write_next_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Дальше")
            }
        }
    }
}
