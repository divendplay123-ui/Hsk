package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ExampleWord
import com.example.data.model.HanziEntry
import com.example.data.model.HskLevel
import com.example.data.model.HskStandard
import com.example.data.model.HskWordEntry
import com.example.data.repository.HanziCatalog
import com.example.domain.MakeMeAHanziRepository
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CharacterDetailSheet(
    entry: HanziEntry,
    onDismiss: () -> Unit,
    word: HskWordEntry? = null,
    standard: HskStandard = HskStandard.HSK_3_0,
    onSelectCharacterInWord: (HanziEntry) -> Unit = {},
    onSelectExampleWord: (hanzi: String, pinyin: String, meaningRu: String, level: HskLevel) -> Unit = { _, _, _, _ -> }
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val wordEntries = remember(word?.hanzi, standard, entry.character) {
        if (word != null) {
            val resolved = HanziCatalog.resolveWordEntries(word, standard)
            if (resolved.isNotEmpty()) resolved else listOf(entry)
        } else {
            listOf(entry)
        }
    }
    val isMultiCharWord = word != null && wordEntries.size > 1

    var replayTrigger by remember(entry.character, word?.hanzi) { mutableIntStateOf(0) }
    var singleCharReplayTrigger by remember(entry.character, word?.hanzi) { mutableIntStateOf(0) }
    var isFullWordPlayback by remember(word?.hanzi) { mutableStateOf(true) }
    var focusedCharIndex by remember(word?.hanzi) { mutableIntStateOf(0) }
    var activeStrokeIndex by remember(entry.character, word?.hanzi) { mutableIntStateOf(-1) }

    val charTimeWeights = remember(wordEntries) {
        wordEntries.map { charEntry ->
            MakeMeAHanziRepository.getGlyph(charEntry.character)?.totalWeight
                ?: charEntry.strokeCount.coerceAtLeast(1).toFloat()
        }
    }
    val charCumulativeStarts = remember(charTimeWeights) {
        val starts = FloatArray(charTimeWeights.size)
        var acc = 0f
        for (i in charTimeWeights.indices) {
            starts[i] = acc
            acc += charTimeWeights[i]
        }
        starts
    }
    val totalWordWeight = remember(charTimeWeights) {
        charTimeWeights.sum().coerceAtLeast(0.1f)
    }
    val totalWordStrokes = remember(wordEntries) {
        wordEntries.sumOf { it.strokeCount.coerceAtLeast(1) }
    }

    val fullWordProgress = remember(word?.hanzi, replayTrigger) { Animatable(0f) }

    LaunchedEffect(isMultiCharWord, word?.hanzi, replayTrigger, isFullWordPlayback) {
        if (isMultiCharWord && isFullWordPlayback) {
            fullWordProgress.snapTo(0f)
            val durationMs = (totalWordWeight * 490f).roundToInt().coerceIn(1800, 14500)
            fullWordProgress.animateTo(
                targetValue = totalWordWeight,
                animationSpec = tween(durationMillis = durationMs, easing = LinearEasing)
            )
            activeStrokeIndex = -1
        }
    }

    // Read continuous Animatable ONLY through derivedStateOf returning integer index so UI only recomposes when character changes
    val activeWordCharIndex by remember(isMultiCharWord, isFullWordPlayback, focusedCharIndex, charTimeWeights) {
        derivedStateOf {
            if (isMultiCharWord && isFullWordPlayback) {
                val step = fullWordProgress.value
                var foundIdx = wordEntries.lastIndex
                for (i in wordEntries.indices) {
                    val end = charCumulativeStarts[i] + charTimeWeights[i]
                    if (step < end - 0.001f) {
                        foundIdx = i
                        break
                    }
                }
                foundIdx
            } else {
                focusedCharIndex.coerceIn(0, wordEntries.lastIndex)
            }
        }
    }

    val inspectedEntry = wordEntries.getOrElse(activeWordCharIndex) { entry }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isMultiCharWord && word != null) {
                // ==================== FULL MULTI-CHARACTER WORD HEADER & ANIMATION ====================
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            HskLevelBadge(level = word.hskLevel)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh
                            ) {
                                Text(
                                    text = "Слово · ${wordEntries.size} иерогл. · $totalWordStrokes черт",
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = word.hanzi,
                                fontSize = 30.sp,
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = word.pinyin,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Text(
                            text = word.meaningRu,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("detail_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Закрыть"
                        )
                    }
                }

                // Multi-Character Tianzige Sequential Animation Card
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("detail_animated_word"),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val boxSizeDp = when {
                            wordEntries.size <= 2 -> 114.dp
                            wordEntries.size == 3 -> 94.dp
                            else -> 78.dp
                        }

                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            wordEntries.forEachIndexed { idx, charEntry ->
                                val charStart = charCumulativeStarts[idx]
                                val charWeight = charTimeWeights[idx].coerceAtLeast(0.01f)
                                val isThisCharActive = (idx == activeWordCharIndex)

                                val progressProvider: (() -> Float)? = if (isFullWordPlayback) {
                                    {
                                        ((fullWordProgress.value - charStart) / charWeight).coerceIn(0f, 1f)
                                    }
                                } else {
                                    if (idx == focusedCharIndex) null else { { 1f } }
                                }

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .clickable {
                                            focusedCharIndex = idx
                                            isFullWordPlayback = false
                                            singleCharReplayTrigger++
                                            onSelectCharacterInWord(charEntry)
                                        }
                                        .testTag(
                                            if (idx == 0) {
                                                "detail_animated_hanzi"
                                            } else {
                                                "detail_word_char_box_$idx"
                                            }
                                        )
                                ) {
                                    AnimatedHanziBox(
                                        character = charEntry.character,
                                        strokeCount = charEntry.strokeCount,
                                        replayTrigger = if (idx == focusedCharIndex) singleCharReplayTrigger else 0,
                                        onActiveStrokeIndex = { strokeIdx ->
                                            if (isThisCharActive) {
                                                activeStrokeIndex = strokeIdx
                                            }
                                        },
                                        externalProgressProvider = progressProvider,
                                        isHighlighted = isThisCharActive,
                                        modifier = Modifier.size(boxSizeDp)
                                    )
                                    Text(
                                        text = "${idx + 1}. ${charEntry.character} (${charEntry.pinyin})",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isThisCharActive) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isThisCharActive) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                            }
                        }

                        // Action buttons: Replay entire word vs Replay selected character
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilledTonalButton(
                                onClick = {
                                    isFullWordPlayback = true
                                    replayTrigger++
                                },
                                modifier = Modifier
                                    .heightIn(min = 40.dp)
                                    .testTag("detail_replay_animation_button"),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Replay,
                                    contentDescription = "Повторить анимацию всего слова",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "Повторить всё слово (${word.hanzi})",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    focusedCharIndex = activeWordCharIndex
                                    isFullWordPlayback = false
                                    singleCharReplayTrigger++
                                },
                                modifier = Modifier
                                    .heightIn(min = 40.dp)
                                    .testTag("detail_replay_single_char_button"),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "Только «${inspectedEntry.character}»",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            wordEntries.forEachIndexed { idx, charEntry ->
                                val selected = idx == activeWordCharIndex
                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        focusedCharIndex = idx
                                        isFullWordPlayback = false
                                        singleCharReplayTrigger++
                                        onSelectCharacterInWord(charEntry)
                                    },
                                    label = {
                                        Text("${idx + 1}. ${charEntry.character} · ${charEntry.meaningRu.substringBefore(';')}")
                                    },
                                    modifier = Modifier.testTag("detail_word_char_chip_$idx")
                                )
                            }
                        }
                    }
                }
            } else {
                // ==================== SINGLE CHARACTER HEADER & ANIMATION ====================
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AnimatedHanziBox(
                            character = inspectedEntry.character,
                            strokeCount = inspectedEntry.strokeCount,
                            replayTrigger = replayTrigger,
                            onActiveStrokeIndex = { activeStrokeIndex = it },
                            modifier = Modifier
                                .size(124.dp)
                                .testTag("detail_animated_hanzi")
                        )

                        FilledTonalButton(
                            onClick = { replayTrigger++ },
                            modifier = Modifier
                                .heightIn(min = 36.dp)
                                .testTag("detail_replay_animation_button"),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Replay,
                                contentDescription = "Повторить анимацию рисования",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "Повтор",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            HskLevelBadge(level = inspectedEntry.hskLevel)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh
                            ) {
                                Text(
                                    text = "Черт: ${inspectedEntry.strokeCount}",
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Text(
                            text = inspectedEntry.pinyin,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )

                        Text(
                            text = inspectedEntry.meaningRu,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = "Ключ (радикал): ${inspectedEntry.radical}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("detail_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Закрыть"
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Active Character Breakdown when in Multi-Character Word mode
            if (isMultiCharWord) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Иероглиф ${activeWordCharIndex + 1} из ${wordEntries.size}: «${inspectedEntry.character}» (${inspectedEntry.pinyin})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Значение: ${inspectedEntry.meaningRu}  ·  Ключ: ${inspectedEntry.radical}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Stroke Order Sequence synchronized with animation
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Brush,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (isMultiCharWord) {
                            "Порядок черт «${inspectedEntry.character}» (${inspectedEntry.strokeCount})"
                        } else {
                            "Порядок черт (${inspectedEntry.strokeCount})"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    inspectedEntry.strokeTypes.forEachIndexed { index, strokeType ->
                        val isActive = index == activeStrokeIndex
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isActive) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            border = BorderStroke(
                                width = if (isActive) 1.5.dp else 1.dp,
                                color = if (isActive) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant
                                }
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "${index + 1}.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isActive) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                                Text(
                                    text = strokeType.symbol,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = strokeType.nameRu,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // Examples — tapping any example word animates all characters of that word
            if (inspectedEntry.examples.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Примеры слов (нажмите для анимации слова)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    inspectedEntry.examples.forEach { ex: ExampleWord ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    onSelectExampleWord(
                                        ex.hanzi,
                                        ex.pinyin,
                                        ex.meaningRu,
                                        inspectedEntry.hskLevel
                                    )
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${ex.hanzi}  ·  ${ex.pinyin}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = ex.meaningRu,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.PlayArrow,
                                            contentDescription = "Анимировать слово ${ex.hanzi}",
                                            modifier = Modifier.size(14.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "Анимация",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
