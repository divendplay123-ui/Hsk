package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.HskWordEntry
import com.example.domain.RecallChallenge
import com.example.ui.ActiveRecallUiState
import com.example.ui.MatchRoundUiState
import com.example.ui.components.HskLevelBadge
import com.example.ui.theme.AmberGold
import com.example.ui.theme.AmberSoft
import com.example.ui.theme.IndigoInk
import com.example.ui.theme.IndigoSoft
import com.example.ui.theme.JadeCeladon
import com.example.ui.theme.JadeSoft
import com.example.ui.theme.PlumInk
import com.example.ui.theme.PlumSoft
import com.example.ui.theme.VermilionSeal
import com.example.ui.theme.VermilionSoft
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ActiveRecallSection(
    state: ActiveRecallUiState,
    onAnswerTone: (String) -> Unit,
    onTapAssembleTile: (String) -> Unit,
    onBackspaceAssemble: () -> Unit,
    onClearAssemble: () -> Unit,
    onSubmitAssemble: () -> Unit,
    onNextQuestion: () -> Unit,
    onStartNewRound: () -> Unit,
    onOpenWordDetail: (HskWordEntry) -> Unit
) {
    if (state.isRoundFinished || state.challenges.isEmpty()) {
        val total = state.challenges.size.coerceAtLeast(1)
        val pct = ((state.correctCount * 100f) / total).roundToInt()

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
                    text = "Тренировка завершена!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Точность: ${state.correctCount} из $total ($pct%)",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Ответы автоматически обновили интервалы повторения SM-2.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Button(
                    onClick = onStartNewRound,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("recall_new_round_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Новый раунд")
                }
            }
        }
        return
    }

    val challenge = state.currentChallenge ?: return
    val progress = (state.currentIndex + 1).toFloat() / state.challenges.size.toFloat()
    val isAnswered = state.isCorrect != null

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Задание ${state.currentIndex + 1} из ${state.challenges.size}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer
            ) {
                Text(
                    text = "Серия: ${state.streak}",
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

        when (challenge) {
            is RecallChallenge.ToneChallenge -> {
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
                            HskLevelBadge(level = challenge.target.hskLevel)
                            Text(
                                text = "Выберите верные тоны",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        val fontSize = when (challenge.target.hanzi.length) {
                            1 -> 74.sp
                            2 -> 56.sp
                            3 -> 42.sp
                            else -> 34.sp
                        }
                        Text(
                            text = challenge.target.hanzi,
                            fontSize = fontSize,
                            fontFamily = FontFamily.Serif,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = challenge.target.meaningRu,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    challenge.options.forEachIndexed { idx, opt ->
                        val isSelected = state.selectedToneOption == opt
                        val isRight = opt == challenge.correctOption
                        val (bg, border) = recallColors(isAnswered, isSelected, isRight)

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable(enabled = !isAnswered) { onAnswerTone(opt) }
                                .testTag("recall_option_$idx"),
                            shape = RoundedCornerShape(14.dp),
                            color = bg,
                            border = BorderStroke(1.5.dp, border)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = opt,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (isAnswered && isRight) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = "Верно",
                                        tint = JadeCeladon
                                    )
                                }
                            }
                        }
                    }
                }
            }

            is RecallChallenge.AssembleChallenge -> {
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
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HskLevelBadge(level = challenge.target.hskLevel)
                            Text(
                                text = "Соберите слово из иероглифов",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            text = challenge.target.meaningRu,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = challenge.target.pinyin,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )

                        // Assembly slot display
                        val slotBg = when (state.isCorrect) {
                            true -> JadeSoft
                            false -> VermilionSoft
                            null -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        }
                        val slotBorder = when (state.isCorrect) {
                            true -> JadeCeladon
                            false -> VermilionSeal
                            null -> MaterialTheme.colorScheme.outline
                        }
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 64.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = slotBg,
                            border = BorderStroke(1.5.dp, slotBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (state.assembledChars.isEmpty()) {
                                        "Нажмите иероглифы ниже (${challenge.target.hanzi.length} зн.)"
                                    } else {
                                        state.assembledChars.joinToString(" ")
                                    },
                                    fontSize = if (state.assembledChars.isEmpty()) 14.sp else 32.sp,
                                    fontFamily = if (state.assembledChars.isEmpty()) FontFamily.Default else FontFamily.Serif,
                                    color = if (state.assembledChars.isEmpty()) {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                if (!isAnswered && state.assembledChars.isNotEmpty()) {
                                    IconButton(
                                        onClick = onBackspaceAssemble,
                                        modifier = Modifier.testTag("recall_backspace_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Backspace,
                                            contentDescription = "Удалить последний"
                                        )
                                    }
                                }
                            }
                        }

                        if (isAnswered && state.isCorrect == false) {
                            Text(
                                text = "Правильный ответ: ${challenge.target.hanzi}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = JadeCeladon
                            )
                        }
                    }
                }

                // Tile Bank
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    challenge.tileBank.forEachIndexed { idx, tile ->
                        Surface(
                            modifier = Modifier
                                .size(width = 72.dp, height = 64.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable(enabled = !isAnswered) { onTapAssembleTile(tile) }
                                .testTag("recall_tile_$idx"),
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = tile,
                                    fontSize = 30.sp,
                                    fontFamily = FontFamily.Serif
                                )
                            }
                        }
                    }
                }

                if (!isAnswered) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onClearAssemble,
                            enabled = state.assembledChars.isNotEmpty(),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Очистить")
                        }
                        Button(
                            onClick = onSubmitAssemble,
                            enabled = state.assembledChars.isNotEmpty(),
                            modifier = Modifier
                                .weight(1.4f)
                                .heightIn(min = 48.dp)
                                .testTag("recall_submit_button"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Проверить")
                        }
                    }
                }
            }
        }

        if (isAnswered) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { onOpenWordDetail(challenge.target) },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Написание")
                }

                Button(
                    onClick = onNextQuestion,
                    modifier = Modifier
                        .weight(1.3f)
                        .heightIn(min = 48.dp)
                        .testTag("recall_next_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        if (state.currentIndex + 1 >= state.challenges.size) "Итоги"
                        else "Дальше"
                    )
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                }
            }
        }
    }
}

@Composable
fun MatchPairsSection(
    state: MatchRoundUiState,
    onSelectLeft: (Int) -> Unit,
    onSelectRight: (Int) -> Unit,
    onStartNewRound: () -> Unit
) {
    if (state.pairs.isEmpty()) return

    if (state.isRoundFinished) {
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
                    text = "Все пары найдены!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (state.mistakesCount == 0) {
                        "Идеально: ни одной ошибки!"
                    } else {
                        "Ошибок за раунд: ${state.mistakesCount}"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Button(
                    onClick = onStartNewRound,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("match_new_round_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Следующие 5 пар")
                }
            }
        }
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Сопоставьте иероглиф и перевод (${state.matchedNumbers.size}/${state.pairs.size})",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (state.mistakesCount == 0) JadeSoft else AmberSoft
            ) {
                Text(
                    text = "Ошибок: ${state.mistakesCount}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (state.mistakesCount == 0) JadeCeladon else AmberGold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Left Column: Hanzi
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                state.pairs.forEachIndexed { idx, word ->
                    val isMatched = word.number in state.matchedNumbers
                    val isSelected = state.selectedLeftNumber == word.number
                    val isWrong = state.wrongPairNumbers?.first == word.number

                    val (bg, border) = matchCardColors(isMatched, isSelected, isWrong)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 72.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable(enabled = !isMatched) { onSelectLeft(word.number) }
                            .testTag("match_left_$idx"),
                        shape = RoundedCornerShape(14.dp),
                        color = bg,
                        border = BorderStroke(1.5.dp, border)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = word.hanzi,
                                fontSize = when {
                                    word.hanzi.length <= 2 -> 28.sp
                                    else -> 22.sp
                                },
                                fontFamily = FontFamily.Serif,
                                color = if (isMatched) {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                textAlign = TextAlign.Center
                            )
                            if (isMatched) {
                                Text(
                                    text = word.pinyin,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = JadeCeladon
                                )
                            }
                        }
                    }
                }
            }

            // Right Column: Russian meaning
            Column(
                modifier = Modifier.weight(1.25f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                state.rightColumnWords.forEachIndexed { idx, word ->
                    val isMatched = word.number in state.matchedNumbers
                    val isSelected = state.selectedRightNumber == word.number
                    val isWrong = state.wrongPairNumbers?.second == word.number

                    val (bg, border) = matchCardColors(isMatched, isSelected, isWrong)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 72.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable(enabled = !isMatched) { onSelectRight(word.number) }
                            .testTag("match_right_$idx"),
                        shape = RoundedCornerShape(14.dp),
                        color = bg,
                        border = BorderStroke(1.5.dp, border)
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = word.meaningRu,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                color = if (isMatched) {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyTechniquesGuideSheet(
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("study_guide_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Техники запоминания в приложении",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Все ответы из любого тренажёра автоматически обновляют ваш личный график интервальных повторений SM-2.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            TechniqueGuideCard(
                icon = Icons.Outlined.Schedule,
                badgeColor = VermilionSoft,
                iconTint = VermilionSeal,
                title = "1. Интервальное повторение (SM-2)",
                description = "Оценивайте себя честно: «Снова» возвращает слово через 3–4 карточки, «Трудно» сжимает интервал, «Хорошо» (1 → 6 → 15 дн.) и «Легко» уводят слово в долгую память. Переключатель «Перевод → 汉字» тренирует активное вспоминание."
            )
            TechniqueGuideCard(
                icon = Icons.Outlined.Psychology,
                badgeColor = IndigoSoft,
                iconTint = IndigoInk,
                title = "2. Тоны и сборка (Active Recall)",
                description = "Учит не путать похожие по звучанию слова с разными тонами (mā / má / mǎ / mà) и собирать сложные слова из отдельных иероглифов по памяти."
            )
            TechniqueGuideCard(
                icon = Icons.Outlined.GridView,
                badgeColor = PlumSoft,
                iconTint = PlumInk,
                title = "3. Пары ассоциаций",
                description = "Быстрое сопоставление формы иероглифа со значением без подсказки пиньиня укрепляет прямую визуально-смысловую связь."
            )
            TechniqueGuideCard(
                icon = Icons.Outlined.AutoAwesome,
                badgeColor = AmberSoft,
                iconTint = AmberGold,
                title = "4. Тест и моторная пропись",
                description = "Чередование узнавания в тесте и рисования по сетке 田字格 закрепляет зрительную и мышечную память."
            )
        }
    }
}

@Composable
private fun TechniqueGuideCard(
    icon: ImageVector,
    badgeColor: Color,
    iconTint: Color,
    title: String,
    description: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = badgeColor,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun recallColors(
    isAnswered: Boolean,
    isSelected: Boolean,
    isRight: Boolean
): Pair<Color, Color> {
    if (!isAnswered) {
        return MaterialTheme.colorScheme.surface to MaterialTheme.colorScheme.outlineVariant
    }
    return when {
        isRight -> JadeSoft to JadeCeladon
        isSelected -> VermilionSoft to VermilionSeal
        else -> MaterialTheme.colorScheme.surface to MaterialTheme.colorScheme.outlineVariant
    }
}

@Composable
private fun matchCardColors(
    isMatched: Boolean,
    isSelected: Boolean,
    isWrong: Boolean
): Pair<Color, Color> = when {
    isMatched -> JadeSoft.copy(alpha = 0.55f) to JadeCeladon.copy(alpha = 0.45f)
    isWrong -> VermilionSoft to VermilionSeal
    isSelected -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.primary
    else -> MaterialTheme.colorScheme.surface to MaterialTheme.colorScheme.outlineVariant
}
