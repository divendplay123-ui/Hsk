package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.GridOn
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AppSettings
import com.example.data.model.HskLevel
import com.example.data.model.HskStandard
import com.example.data.repository.HanziCatalog
import com.example.ui.components.HskLevelBadge

@Composable
fun SettingsScreen(
    settings: AppSettings,
    onSelectHskStandard: (HskStandard) -> Unit,
    onToggleHsk2: (Boolean) -> Unit,
    onToggleHsk3: (Boolean) -> Unit,
    onToggleHsk4: (Boolean) -> Unit,
    onToggleUni: (Boolean) -> Unit,
    onToggleGridGuide: (Boolean) -> Unit,
    onToggleStrictStrokeCount: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val standard = settings.hskStandard
    val activeCount = HanziCatalog.entriesForLevels(settings.enabledLevels, standard).size
    val activeWordCount = HanziCatalog.wordsForLevels(settings.enabledLevels, standard).size

    val w1 = HanziCatalog.wordCountForLevel(HskLevel.HSK1, standard)
    val w2 = HanziCatalog.wordCountForLevel(HskLevel.HSK2, standard)
    val w3 = HanziCatalog.wordCountForLevel(HskLevel.HSK3, standard)
    val w4 = HanziCatalog.wordCountForLevel(HskLevel.HSK4, standard)
    val w5 = HanziCatalog.wordCountForLevel(HskLevel.UNI, standard)

    val c1 = HanziCatalog.countForLevel(HskLevel.HSK1, standard)
    val c2 = HanziCatalog.countForLevel(HskLevel.HSK2, standard)
    val c3 = HanziCatalog.countForLevel(HskLevel.HSK3, standard)
    val c4 = HanziCatalog.countForLevel(HskLevel.HSK4, standard)
    val c5 = HanziCatalog.countForLevel(HskLevel.UNI, standard)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.widthIn(max = 640.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Настройки",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            // Active Vocabulary Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Layers,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "База ${standard.shortLabel}: $activeCount иероглифов · $activeWordCount слов",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        val levelsSummary = settings.enabledLevels
                            .sortedBy { it.number }
                            .joinToString(", ") { it.label }
                        Text(
                            text = "Включённые уровни: $levelsSummary",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // HSK Standard Switcher (HSK 3.0 vs HSK 2.0)
            Text(
                text = "Версия стандарта HSK",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HskStandard.entries.forEach { item ->
                    val selected = item == standard
                    val tag = if (item == HskStandard.HSK_3_0) {
                        "standard_hsk30_button"
                    } else {
                        "standard_hsk20_button"
                    }
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 72.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onSelectHskStandard(item) }
                            .testTag(tag),
                        shape = RoundedCornerShape(16.dp),
                        color = if (selected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                        border = BorderStroke(
                            width = if (selected) 1.5.dp else 1.dp,
                            color = if (selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outlineVariant
                            }
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item.label,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                )
                                if (selected) {
                                    Icon(
                                        imageVector = Icons.Outlined.CheckCircle,
                                        contentDescription = "Выбрано",
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Text(
                                text = item.descriptionRu,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (selected) {
                                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                }
            }

            // HSK Levels Toggles Section
            Text(
                text = "Уровни словаря ${standard.shortLabel}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // HSK 1 (Always active)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                HskLevelBadge(level = HskLevel.HSK1)
                                Text(
                                    text = "$w1 слов · $c1 иероглифов",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Базовый уровень ${standard.shortLabel} (№ 1–$w1) включён по умолчанию",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CheckCircle,
                                    contentDescription = "HSK 1 активен",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = "Базовый",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    HskToggleRow(
                        level = HskLevel.HSK2,
                        title = "Добавить HSK 2",
                        subtitle = "Элементарный уровень (№ ${w1 + 1}–${w1 + w2}: +$w2 слов · $c2 иерогл.)",
                        checked = settings.includeHsk2,
                        onCheckedChange = onToggleHsk2,
                        switchTestTag = "hsk2_switch"
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    HskToggleRow(
                        level = HskLevel.HSK3,
                        title = "Добавить HSK 3",
                        subtitle = "Пороговый уровень (№ ${w1 + w2 + 1}–${w1 + w2 + w3}: +$w3 слов · $c3 иерогл.)",
                        checked = settings.includeHsk3,
                        onCheckedChange = onToggleHsk3,
                        switchTestTag = "hsk3_switch"
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    HskToggleRow(
                        level = HskLevel.HSK4,
                        title = "Добавить HSK 4",
                        subtitle = "Средний уровень (№ ${w1 + w2 + w3 + 1}–${w1 + w2 + w3 + w4}: +$w4 слов · $c4 иерогл.)",
                        checked = settings.includeHsk4,
                        onCheckedChange = onToggleHsk4,
                        switchTestTag = "hsk4_switch"
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    HskToggleRow(
                        level = HskLevel.UNI,
                        title = "Добавить программу вуза",
                        subtitle = "Лексика и иероглифы из уроков 1–13, не вошедшие в HSK 1–4 (+$c5 иерогл. · +$w5 слов)",
                        checked = settings.includeUni,
                        onCheckedChange = onToggleUni,
                        switchTestTag = "uni_switch"
                    )
                }
            }

            // Canvas & Recognition Options
            Text(
                text = "Холст и распознавание",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.GridOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Сетка каллиграфии (田字格)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Направляющие линии для пропорционального письма",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.showGridGuide,
                            onCheckedChange = onToggleGridGuide,
                            modifier = Modifier.testTag("grid_guide_switch")
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Строгий счёт черт",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Искать только с точным числом касаний (отключите, чтобы рисовать слитно)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.strictStrokeCount,
                            onCheckedChange = onToggleStrictStrokeCount,
                            modifier = Modifier.testTag("strict_stroke_switch")
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HskToggleRow(
    level: HskLevel,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    switchTestTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HskLevelBadge(level = level)
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(switchTestTag)
        )
    }
}
