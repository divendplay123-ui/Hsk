package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.AppSettings
import com.example.data.model.HanziEntry
import com.example.data.model.HskLevel
import com.example.data.model.HskStandard
import com.example.data.model.HskWordEntry
import com.example.data.repository.HanziCatalog
import com.example.ui.components.HskLevelBadge

@Composable
fun DictionaryScreen(
    settings: AppSettings,
    query: String,
    selectedLevelFilter: HskLevel?,
    entries: List<HanziEntry>,
    words: List<HskWordEntry> = emptyList(),
    showWords: Boolean = false,
    onSelectHskStandard: (HskStandard) -> Unit = {},
    onToggleShowWords: (Boolean) -> Unit = {},
    onQueryChange: (String) -> Unit,
    onSelectLevelFilter: (HskLevel?) -> Unit,
    onSelectCharacter: (HanziEntry) -> Unit,
    onSelectWord: (HskWordEntry) -> Unit = {},
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val standard = settings.hskStandard
    val totalWordsInStandard = HanziCatalog.allWordsFor(standard).size
    val recentEntries = settings.recentCharacters
        .mapNotNull { HanziCatalog.findByCharacter(it, standard) }
        .filter { it.hskLevel in settings.enabledLevels }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 156.dp),
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Editorial Studio Banner
        item(span = { GridItemSpan(maxLineSpan) }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(136.dp),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_calligraphy_studio),
                        contentDescription = "Иллюстрация студии китайской каллиграфии",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xE81C1B18),
                                        Color(0x991C1B18),
                                        Color(0x221C1B18)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Словарь ${standard.shortLabel}",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        val levelsText = settings.enabledLevels
                            .sortedBy { it.number }
                            .joinToString(" · ") { it.label }
                        Text(
                            text = "$levelsText (${entries.size} иерогл. · ${words.size} слов)",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFEBE4D8)
                        )
                        if (!settings.includeHsk2 || !settings.includeHsk3 || !settings.includeHsk4) {
                            Text(
                                text = "Добавить HSK 2, 3, 4 →",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFFFFB5A7),
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable(onClick = onOpenSettings)
                                    .padding(vertical = 2.dp)
                                    .testTag("dictionary_add_levels_link")
                            )
                        }
                    }
                }
            }
        }

        // Standard switcher (HSK 3.0 vs HSK 2.0) + Mode switcher (Иероглифы vs Слова)
        item(span = { GridItemSpan(maxLineSpan) }) {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = !showWords,
                        onClick = { onToggleShowWords(false) },
                        label = { Text("Иероглифы (${entries.size})") },
                        modifier = Modifier.testTag("dict_tab_chars")
                    )
                }
                item {
                    FilterChip(
                        selected = showWords,
                        onClick = { onToggleShowWords(true) },
                        label = { Text("Слова ${standard.shortLabel} (${words.size})") },
                        modifier = Modifier.testTag("dict_tab_words")
                    )
                }
                item {
                    FilterChip(
                        selected = standard == HskStandard.HSK_3_0,
                        onClick = { onSelectHskStandard(HskStandard.HSK_3_0) },
                        label = { Text("HSK 3.0") },
                        modifier = Modifier.testTag("dict_std_hsk30")
                    )
                }
                item {
                    FilterChip(
                        selected = standard == HskStandard.HSK_2_0,
                        onClick = { onSelectHskStandard(HskStandard.HSK_2_0) },
                        label = { Text("HSK 2.0") },
                        modifier = Modifier.testTag("dict_std_hsk20")
                    )
                }
            }
        }

        // Search input
        item(span = { GridItemSpan(maxLineSpan) }) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dictionary_search_input"),
                placeholder = {
                    Text(
                        if (showWords) {
                            "Поиск по $totalWordsInStandard словам ${standard.shortLabel}: слово, пиньинь, № или перевод"
                        } else {
                            "Поиск (${standard.shortLabel}): иероглиф, пиньинь или перевод"
                        }
                    )
                },
                leadingIcon = {
                    Icon(imageVector = Icons.Filled.Search, contentDescription = "Поиск")
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(imageVector = Icons.Filled.Clear, contentDescription = "Очистить")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )
        }

        // Active HSK filter chips
        item(span = { GridItemSpan(maxLineSpan) }) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = selectedLevelFilter == null,
                        onClick = { onSelectLevelFilter(null) },
                        label = { Text("Все активные") },
                        modifier = Modifier.testTag("filter_chip_all")
                    )
                }
                items(settings.enabledLevels.sortedBy { it.number }) { level ->
                    FilterChip(
                        selected = selectedLevelFilter == level,
                        onClick = {
                            onSelectLevelFilter(if (selectedLevelFilter == level) null else level)
                        },
                        label = { Text(level.label) },
                        modifier = Modifier.testTag("filter_chip_hsk${level.number}")
                    )
                }
            }
        }

        // Recent characters
        if (!showWords && recentEntries.isNotEmpty() && query.isBlank()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Недавние",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(recentEntries, key = { "recent_${it.character}" }) { recent ->
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onSelectCharacter(recent) },
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(text = recent.character, fontSize = 20.sp)
                                    Text(
                                        text = recent.pinyin,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        val isEmpty = if (showWords) words.isEmpty() else entries.isEmpty()
        if (isEmpty) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.MenuBook,
                            contentDescription = null,
                            modifier = Modifier.size(36.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Ничего не найдено",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Попробуйте другой запрос или включите уровни HSK 2–4 в настройках.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else if (showWords) {
            items(words, key = { "word_${it.number}" }) { word ->
                DictionaryWordCard(
                    word = word,
                    onClick = { onSelectWord(word) }
                )
            }
        } else {
            items(entries, key = { it.character }) { entry ->
                DictionaryCharacterCard(
                    entry = entry,
                    onClick = { onSelectCharacter(entry) }
                )
            }
        }
    }
}

@Composable
private fun DictionaryCharacterCard(
    entry: HanziEntry,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag("dict_card_${entry.character}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                HskLevelBadge(level = entry.hskLevel)
                Text(
                    text = "${entry.strokeCount} черт",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(2.dp))

            Text(
                text = entry.character,
                fontSize = 42.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Text(
                text = entry.pinyin,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Text(
                text = entry.meaningRu,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DictionaryWordCard(
    word: HskWordEntry,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag("dict_word_${word.number}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                HskLevelBadge(level = word.hskLevel)
                Text(
                    text = if (word.pos.isNotBlank()) "№${word.number} · ${word.pos}" else "№${word.number}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(2.dp))

            Text(
                text = word.hanzi,
                fontSize = if (word.hanzi.length <= 2) 34.sp else 26.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Text(
                text = word.pinyin,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Text(
                text = word.meaningRu,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
