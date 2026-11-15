package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import android.graphics.Path as AndroidPath
import android.graphics.PathMeasure
import android.graphics.Rect
import android.graphics.Typeface
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.example.domain.HanziStrokePaths
import com.example.domain.MakeMeAHanziGlyph
import com.example.domain.MakeMeAHanziRepository
import com.example.domain.StrokePoint
import com.example.domain.VisualShapeMatcher
import kotlinx.coroutines.flow.distinctUntilChanged
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object AndroidGlyphRasterizer {
    private val cache = ConcurrentHashMap<String, VisualShapeMatcher.ShapeFeatures>()

    fun getFeaturesForCharacter(character: String): VisualShapeMatcher.ShapeFeatures {
        return cache.getOrPut(character) {
            val grid32 = rasterizeGlyphTo32(character)
            VisualShapeMatcher.extractFeatures(grid32)
        }
    }

    private fun rasterizeGlyphTo32(character: String): FloatArray {
        val size = 64
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = AndroidCanvas(bitmap)
        canvas.drawColor(android.graphics.Color.TRANSPARENT)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.BLACK
            textSize = 48f
            typeface = Typeface.SERIF
        }

        val bounds = Rect()
        paint.getTextBounds(character, 0, character.length, bounds)

        val w = max(1f, bounds.width().toFloat())
        val h = max(1f, bounds.height().toFloat())
        val scale = 48f / max(w, h)

        canvas.save()
        canvas.translate(size * 0.5f, size * 0.5f)
        canvas.scale(scale, scale)
        canvas.drawText(character, -bounds.exactCenterX(), -bounds.exactCenterY(), paint)
        canvas.restore()

        val pixels = IntArray(size * size)
        bitmap.getPixels(pixels, 0, size, 0, 0, size, size)
        bitmap.recycle()

        val grid32 = FloatArray(VisualShapeMatcher.GRID_SIZE * VisualShapeMatcher.GRID_SIZE)
        for (y in 0 until VisualShapeMatcher.GRID_SIZE) {
            for (x in 0 until VisualShapeMatcher.GRID_SIZE) {
                val sx = x * 2
                val sy = y * 2
                val a00 = (pixels[sy * size + sx] ushr 24) / 255f
                val a10 = (pixels[sy * size + sx + 1] ushr 24) / 255f
                val a01 = (pixels[(sy + 1) * size + sx] ushr 24) / 255f
                val a11 = (pixels[(sy + 1) * size + sx + 1] ushr 24) / 255f
                grid32[y * VisualShapeMatcher.GRID_SIZE + x] =
                    max(max(a00, a10), max(a01, a11))
            }
        }
        return grid32
    }
}

private class GlyphCanvasScratch {
    val ghostPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    val inkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    val brushPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    val nibGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    val nibCorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    val segmentPath = AndroidPath()
    val pathMeasure = PathMeasure()
    val tipPos = FloatArray(2)
}

@Composable
fun AnimatedHanziBox(
    character: String,
    strokeCount: Int,
    replayTrigger: Int,
    onActiveStrokeIndex: (Int) -> Unit,
    modifier: Modifier = Modifier,
    externalProgressProvider: (() -> Float)? = null,
    isHighlighted: Boolean = false
) {
    val svgGlyph = remember(character) {
        MakeMeAHanziRepository.getGlyph(character)
    }
    val fallbackStrokes = remember(character, svgGlyph == null) {
        if (svgGlyph == null) HanziStrokePaths.getNormalizedStrokes(character) else null
    }

    val internalProgress = remember(character, replayTrigger) { Animatable(0f) }

    LaunchedEffect(character, replayTrigger, externalProgressProvider == null) {
        if (externalProgressProvider == null) {
            internalProgress.snapTo(0f)
            val durationMs = if (svgGlyph != null) {
                (svgGlyph.totalWeight * 490f).roundToInt().coerceIn(1100, 9500)
            } else {
                (strokeCount.coerceAtLeast(1) * 520).coerceIn(1100, 7500)
            }
            internalProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = durationMs, easing = LinearEasing)
            )
            onActiveStrokeIndex(-1)
        }
    }

    // Emit stroke index updates ONLY when the integer stroke index actually changes,
    // avoiding 60fps recompositions of the parent sheet/badges.
    LaunchedEffect(character, strokeCount, svgGlyph, externalProgressProvider) {
        val badgeCount = strokeCount.coerceAtLeast(1)
        snapshotFlow {
            val p = (externalProgressProvider?.invoke() ?: internalProgress.value).coerceIn(0f, 1f)
            if (p <= 0.002f || p >= 0.996f) {
                -1
            } else if (svgGlyph != null) {
                val rawIdx = activeStrokeIndexForProgress(svgGlyph, p).first
                rawIdx.coerceIn(0, badgeCount - 1)
            } else {
                (p * badgeCount).toInt().coerceIn(0, badgeCount - 1)
            }
        }
            .distinctUntilChanged()
            .collect { idx ->
                onActiveStrokeIndex(idx)
            }
    }

    val scratch = remember { GlyphCanvasScratch() }
    val inkColor = MaterialTheme.colorScheme.onSurface
    val accentColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outline

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(
            width = if (isHighlighted) 2.2.dp else 1.5.dp,
            color = if (isHighlighted) accentColor else accentColor.copy(alpha = 0.45f)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawTianzigeGrid(gridColor)

                val p = (externalProgressProvider?.invoke() ?: internalProgress.value).coerceIn(0f, 1f)
                val boxSize = size.minDimension

                if (svgGlyph != null) {
                    drawMakeMeAHanziSequence(
                        glyph = svgGlyph,
                        progress = p,
                        boxSize = boxSize,
                        inkColor = inkColor,
                        nibColor = accentColor,
                        scratch = scratch
                    )
                } else if (fallbackStrokes != null) {
                    drawFallbackStrokes(
                        strokes = fallbackStrokes,
                        progress = p,
                        boxSize = boxSize,
                        inkColor = inkColor,
                        nibColor = accentColor
                    )
                } else {
                    drawSerifProgressiveReveal(
                        character = character,
                        progress = p,
                        boxSize = boxSize,
                        inkColor = inkColor
                    )
                }
            }
        }
    }
}

private fun activeStrokeIndexForProgress(
    glyph: MakeMeAHanziGlyph,
    progress: Float
): Pair<Int, Float> {
    val strokes = glyph.strokes
    if (strokes.isEmpty()) return -1 to 1f
    val targetWeight = progress * glyph.totalWeight
    var acc = 0f
    for (i in strokes.indices) {
        val w = strokes[i].durationWeight
        if (targetWeight < acc + w || i == strokes.lastIndex) {
            val local = ((targetWeight - acc) / w).coerceIn(0f, 1f)
            return i to local
        }
        acc += w
    }
    return strokes.lastIndex to 1f
}

private fun DrawScope.drawMakeMeAHanziSequence(
    glyph: MakeMeAHanziGlyph,
    progress: Float,
    boxSize: Float,
    inkColor: Color,
    nibColor: Color,
    scratch: GlyphCanvasScratch
) {
    val pad = boxSize * 0.06f
    val glyphSpan = max(1f, boxSize - pad * 2f)
    val scaleFactor = glyphSpan / 1024f

    val nativeCanvas = drawContext.canvas.nativeCanvas
    nativeCanvas.save()
    nativeCanvas.translate(pad, pad)
    nativeCanvas.scale(scaleFactor, scaleFactor)

    // 1. Crisp authentic KaiTi ghost guide underneath (exact strokes at subtle opacity)
    scratch.ghostPaint.color = inkColor.copy(alpha = 0.13f).toArgb()
    for (s in glyph.strokes) {
        nativeCanvas.drawPath(s.outlinePath, scratch.ghostPaint)
    }

    if (progress <= 0.001f) {
        nativeCanvas.restore()
        return
    }

    if (progress >= 0.996f) {
        // Completed character uses the exact same stroke vector outlines — zero pop or font swap
        scratch.inkPaint.color = inkColor.toArgb()
        for (s in glyph.strokes) {
            nativeCanvas.drawPath(s.outlinePath, scratch.inkPaint)
        }
        nativeCanvas.restore()
        return
    }

    val (activeIdx, rawLocal) = activeStrokeIndexForProgress(glyph, progress)
    if (activeIdx < 0) {
        nativeCanvas.restore()
        return
    }

    // 2. Draw all completed strokes cleanly
    scratch.inkPaint.color = inkColor.toArgb()
    for (i in 0 until activeIdx) {
        nativeCanvas.drawPath(glyph.strokes[i].outlinePath, scratch.inkPaint)
    }

    // 3. Draw ONLY the active stroke clipped strictly to its own outlinePath (zero overlap onto other strokes)
    val activeStroke = glyph.strokes[activeIdx]
    // Smooth cosine ease-in-out for natural human calligraphic brush acceleration
    val eased = (0.5f - 0.5f * cos(rawLocal * PI.toFloat())).coerceIn(0f, 1f)
    val touchAlpha = (rawLocal / 0.07f).coerceIn(0f, 1f)

    scratch.pathMeasure.setPath(activeStroke.medianPath, false)
    val totalLen = scratch.pathMeasure.length
    val drawLen = (totalLen * eased).coerceAtLeast(1f)

    scratch.segmentPath.rewind()
    val hasSegment = scratch.pathMeasure.getSegment(0f, drawLen, scratch.segmentPath, true)

    nativeCanvas.save()
    nativeCanvas.clipPath(activeStroke.outlinePath)

    if (hasSegment) {
        val baseBrushWidth = (activeStroke.medianLength * 1.1f).coerceIn(138f, 224f)
        val endFlareBoost = if (eased > 0.78f) {
            130f * ((eased - 0.78f) / 0.22f)
        } else {
            0f
        }
        scratch.brushPaint.strokeWidth = baseBrushWidth + endFlareBoost
        scratch.brushPaint.color = inkColor.copy(alpha = touchAlpha).toArgb()
        nativeCanvas.drawPath(scratch.segmentPath, scratch.brushPaint)
    }

    // Gentle final settle inside this stroke's own clip so sharp hooks complete seamlessly
    if (eased > 0.86f) {
        val settleAlpha = ((eased - 0.86f) / 0.14f).coerceIn(0f, 1f)
        scratch.inkPaint.color = inkColor.copy(alpha = settleAlpha).toArgb()
        nativeCanvas.drawPath(activeStroke.outlinePath, scratch.inkPaint)
    }

    nativeCanvas.restore()

    // 4. Subtle brush tip tracker along the active median while writing
    if (eased in 0.03f..0.95f) {
        scratch.pathMeasure.getPosTan(drawLen, scratch.tipPos, null)
        val tipX = scratch.tipPos[0]
        val tipY = scratch.tipPos[1]
        scratch.nibGlowPaint.color = nibColor.copy(alpha = 0.26f).toArgb()
        scratch.nibCorePaint.color = nibColor.copy(alpha = 0.90f).toArgb()
        nativeCanvas.drawCircle(tipX, tipY, 52f, scratch.nibGlowPaint)
        nativeCanvas.drawCircle(tipX, tipY, 24f, scratch.nibCorePaint)
    }

    nativeCanvas.restore()
}

private fun DrawScope.drawFallbackStrokes(
    strokes: List<List<StrokePoint>>,
    progress: Float,
    boxSize: Float,
    inkColor: Color,
    nibColor: Color
) {
    val scale = boxSize / 100f
    val baseWidth = boxSize * 0.056f
    val ghostColor = inkColor.copy(alpha = 0.13f)

    for (stroke in strokes) {
        drawSimpleStroke(stroke, 1f, scale, baseWidth, ghostColor)
    }

    val totalStrokes = strokes.size.coerceAtLeast(1)
    val slotSize = 1f / totalStrokes
    var activeTip: Offset? = null

    for (i in 0 until totalStrokes) {
        val slotStart = i * slotSize
        if (progress <= slotStart) break
        val localRaw = ((progress - slotStart) / slotSize).coerceIn(0f, 1f)
        val eased = (0.5f - 0.5f * cos(localRaw * PI.toFloat())).coerceIn(0f, 1f)
        val tip = drawSimpleStroke(strokes[i], eased, scale, baseWidth, inkColor)
        if (localRaw < 1f && tip != null) {
            activeTip = tip
        }
    }

    if (activeTip != null && progress < 0.995f) {
        drawCircle(color = nibColor.copy(alpha = 0.24f), radius = baseWidth * 1.4f, center = activeTip)
        drawCircle(color = nibColor, radius = baseWidth * 0.6f, center = activeTip)
    }
}

private fun DrawScope.drawSimpleStroke(
    pts: List<StrokePoint>,
    fraction: Float,
    scale: Float,
    baseWidth: Float,
    color: Color
): Offset? {
    if (pts.size < 2 || fraction <= 0f) return null
    val androidPath = AndroidPath().apply {
        moveTo(pts[0].x * scale, pts[0].y * scale)
        for (i in 1 until pts.size) {
            lineTo(pts[i].x * scale, pts[i].y * scale)
        }
    }
    val pm = PathMeasure(androidPath, false)
    val totalLen = pm.length
    if (totalLen <= 0.5f) return null

    val drawLen = totalLen * fraction.coerceIn(0f, 1f)
    val step = max(1.8f, totalLen / 24f)
    val posPrev = FloatArray(2)
    val posCurr = FloatArray(2)
    pm.getPosTan(0f, posPrev, null)
    var dist = step
    var lastOffset = Offset(posPrev[0], posPrev[1])

    while (dist < drawLen) {
        pm.getPosTan(dist, posCurr, null)
        val startPt = Offset(posPrev[0], posPrev[1])
        val endPt = Offset(posCurr[0], posCurr[1])
        drawLine(color = color, start = startPt, end = endPt, strokeWidth = baseWidth, cap = StrokeCap.Round)
        posPrev[0] = posCurr[0]
        posPrev[1] = posCurr[1]
        lastOffset = endPt
        dist += step
    }

    if (drawLen > 0f) {
        pm.getPosTan(drawLen, posCurr, null)
        val endPt = Offset(posCurr[0], posCurr[1])
        drawLine(color = color, start = Offset(posPrev[0], posPrev[1]), end = endPt, strokeWidth = baseWidth, cap = StrokeCap.Round)
        lastOffset = endPt
    }
    return lastOffset
}

private fun DrawScope.drawSerifProgressiveReveal(
    character: String,
    progress: Float,
    boxSize: Float,
    inkColor: Color
) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = boxSize * 0.74f
        typeface = Typeface.SERIF
    }

    val bounds = Rect()
    paint.getTextBounds(character, 0, character.length, bounds)
    val x = (boxSize - bounds.width()) * 0.5f - bounds.left
    val y = (boxSize + bounds.height()) * 0.5f - bounds.bottom

    val nativeCanvas = drawContext.canvas.nativeCanvas
    paint.color = inkColor.copy(alpha = 0.14f).toArgb()
    nativeCanvas.drawText(character, x, y, paint)

    val revealHeight = (size.height * progress).coerceIn(0f, size.height)
    clipRect(left = 0f, top = 0f, right = size.width, bottom = revealHeight) {
        paint.color = inkColor.toArgb()
        nativeCanvas.drawText(character, x, y, paint)
    }
}
