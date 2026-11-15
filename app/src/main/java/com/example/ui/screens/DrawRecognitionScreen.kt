package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppSettings
import com.example.data.model.HanziEntry
import com.example.data.model.HskLevel
import com.example.data.model.HskStandard
import com.example.data.model.RecognizedCandidate
import com.example.data.model.StrokeType
import com.example.domain.DrawnStroke
import com.example.domain.StrokePoint
import com.example.ui.components.HskLevelBadge
import com.example.ui.components.drawTianzigeGrid
import kotlin.math.roundToInt

@Composable
fun DrawRecognitionScreen(
    settings: AppSettings,
    drawnStrokes: List<DrawnStroke>,
    localCandidates: List<RecognizedCandidate>,
    onStrokeCompleted: (List<StrokePoint>, Float) -> Unit,
    onManualStrokeTap: (StrokeType) -> Unit,
    onUndo: () -> Unit,
    onClear: () -> Unit,
    onSelectCharacter: (HanziEntry) -> Unit,
    onSelectHskStandard: (HskStandard) -> Unit = {},
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activePathPoints = remember { mutableStateListOf<StrokePoint>() }
    var canvasDimensionPx by remember { mutableFloatStateOf(600f) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val isWide = maxWidth >= 720.dp

        if (isWide) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    HeaderLevelStrip(
                        settings = settings,
                        onSelectHskStandard = onSelectHskStandard,
                        onOpenSettings = onOpenSettings
                    )
                    CalligraphyCanvasCard(
                        drawnStrokes = drawnStrokes,
                        activePathPoints = activePathPoints,
                        showGridGuide = settings.showGridGuide,
                        onCanvasMeasured = { canvasDimensionPx = it },
                        onStrokeCompleted = { pts -> onStrokeCompleted(pts, canvasDimensionPx) }
                    )
                    CanvasActionBar(
                        hasStrokes = drawnStrokes.isNotEmpty(),
                        onUndo = onUndo,
                        onClear = onClear
                    )
                    QuickStrokeButtonsBar(onManualStrokeTap = onManualStrokeTap)
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    DetectedStrokeSequenceCard(drawnStrokes = drawnStrokes)
                    CandidatesSection(
                        candidates = localCandidates,
                        hasStrokes = drawnStrokes.isNotEmpty(),
                        onSelectCharacter = onSelectCharacter
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HeaderLevelStrip(
                    settings = settings,
                    onSelectHskStandard = onSelectHskStandard,
                    onOpenSettings = onOpenSettings
                )

                CandidatesSection(
                    candidates = localCandidates,
                    hasStrokes = drawnStrokes.isNotEmpty(),
                    onSelectCharacter = onSelectCharacter
                )

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CalligraphyCanvasCard(
                        drawnStrokes = drawnStrokes,
                        activePathPoints = activePathPoints,
                        showGridGuide = settings.showGridGuide,
                        onCanvasMeasured = { canvasDimensionPx = it },
                        onStrokeCompleted = { pts -> onStrokeCompleted(pts, canvasDimensionPx) },
                        modifier = Modifier.widthIn(max = 420.dp)
                    )
                }

                DetectedStrokeSequenceCard(drawnStrokes = drawnStrokes)

                CanvasActionBar(
                    hasStrokes = drawnStrokes.isNotEmpty(),
                    onUndo = onUndo,
                    onClear = onClear
                )

                QuickStrokeButtonsBar(onManualStrokeTap = onManualStrokeTap)
            }
        }
    }
}

@Composable
private fun HeaderLevelStrip(
    settings: AppSettings,
    onSelectHskStandard: (HskStandard) -> Unit,
    onOpenSettings: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Поиск по чертам",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            val activeLabels = settings.enabledLevels
                .sortedBy { it.number }
                .joinToString(" + ") { it.label }
            Text(
                text = "Активно: ${settings.hskStandard.shortLabel} · $activeLabels",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        val nextStandard = if (settings.hskStandard == HskStandard.HSK_3_0) {
            HskStandard.HSK_2_0
        } else {
            HskStandard.HSK_3_0
        }
        Surface(
            modifier = Modifier
                .heightIn(min = 36.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable { onSelectHskStandard(nextStandard) }
                .testTag("draw_standard_toggle"),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
        ) {
            Text(
                text = settings.hskStandard.shortLabel,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
            )
        }

        Surface(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onOpenSettings)
                .testTag("header_levels_shortcut"),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                HskLevel.entries.forEach { level ->
                    val enabled = level in settings.enabledLevels
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (enabled) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline
                            )
                    )
                }
                Icon(
                    imageVector = Icons.Filled.Tune,
                    contentDescription = "Настроить уровни HSK",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CandidatesSection(
    candidates: List<RecognizedCandidate>,
    hasStrokes: Boolean,
    onSelectCharacter: (HanziEntry) -> Unit
) {
    if (!hasStrokes) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Brush,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Column {
                    Text(
                        text = "Нарисуйте иероглиф так, как помните",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Распознаём локально по форме рисунка — можно рисовать слитно (например, 口 одним квадратом) или по чертам.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        return
    }

    if (candidates.isEmpty()) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Text(
                text = "Точных совпадений не найдено — попробуйте отключить «Строгий счёт черт» или расширьте уровни HSK в Настройках.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(14.dp)
            )
        }
        return
    }

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("candidates_row")
    ) {
        items(
            items = candidates,
            key = { it.entry.character }
        ) { candidate ->
            CandidateTile(
                candidate = candidate,
                onClick = { onSelectCharacter(candidate.entry) }
            )
        }
    }
}

@Composable
private fun CandidateTile(
    candidate: RecognizedCandidate,
    onClick: () -> Unit
) {
    val entry = candidate.entry

    Card(
        modifier = Modifier
            .width(126.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("candidate_card_${entry.character}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                HskLevelBadge(level = entry.hskLevel)
                Text(
                    text = "${(candidate.confidence * 100).roundToInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = entry.character,
                fontSize = 36.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Text(
                text = entry.pinyin,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Text(
                text = entry.meaningRu,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
private fun CalligraphyCanvasCard(
    drawnStrokes: List<DrawnStroke>,
    activePathPoints: MutableList<StrokePoint>,
    showGridGuide: Boolean,
    onCanvasMeasured: (Float) -> Unit,
    onStrokeCompleted: (List<StrokePoint>) -> Unit,
    modifier: Modifier = Modifier
) {
    val inkColor = MaterialTheme.colorScheme.onSurface
    val gridColor = MaterialTheme.colorScheme.outline
    val surfaceColor = MaterialTheme.colorScheme.surface
    val warmTint = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
        tonalElevation = 2.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(surfaceColor, warmTint)
                    )
                )
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("draw_canvas")
                    .onSizeChanged { size ->
                        if (size.width > 0) {
                            onCanvasMeasured(size.width.toFloat())
                        }
                    }
                    .pointerInput(Unit) {
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
                                    onStrokeCompleted(activePathPoints.toList())
                                    activePathPoints.clear()
                                }
                            },
                            onDragCancel = {
                                activePathPoints.clear()
                            }
                        )
                    }
            ) {
                if (showGridGuide) {
                    drawTianzigeGrid(gridColor)
                }

                val strokeStyle = Stroke(
                    width = size.minDimension * 0.036f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )

                fun drawPointsList(pts: List<StrokePoint>, color: Color) {
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

                drawnStrokes.forEach { stroke ->
                    drawPointsList(stroke.points, inkColor)
                }
                if (activePathPoints.isNotEmpty()) {
                    drawPointsList(activePathPoints, inkColor.copy(alpha = 0.85f))
                }
            }

            if (drawnStrokes.isEmpty() && activePathPoints.isEmpty()) {
                Text(
                    text = "Поле для письма (田字格)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun DetectedStrokeSequenceCard(drawnStrokes: List<DrawnStroke>) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = "Черт: ${drawnStrokes.size}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            if (drawnStrokes.isEmpty()) {
                Text(
                    text = "Последовательность черт появится здесь",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(drawnStrokes.size) { index ->
                        val st = drawnStrokes[index]
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = st.classifiedType.symbol,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CanvasActionBar(
    hasStrokes: Boolean,
    onUndo: () -> Unit,
    onClear: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(
            onClick = onUndo,
            enabled = hasStrokes,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
                .testTag("undo_stroke_button"),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Undo,
                contentDescription = "Отменить черту",
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text("Назад")
        }

        OutlinedButton(
            onClick = onClear,
            enabled = hasStrokes,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
                .testTag("clear_canvas_button"),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.DeleteOutline,
                contentDescription = "Очистить холст",
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text("Сброс")
        }
    }
}

@Composable
private fun QuickStrokeButtonsBar(
    onManualStrokeTap: (StrokeType) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "Или выберите тип черты вручную:",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            StrokeType.entries.forEach { strokeType ->
                FilledTonalButton(
                    onClick = { onManualStrokeTap(strokeType) },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("manual_stroke_${strokeType.code}"),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = strokeType.symbol,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = strokeType.nameRu.substringBefore(" "),
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
