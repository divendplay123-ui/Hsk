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
import com.example.domain.StrokePoint
import com.example.domain.VisualShapeMatcher
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.max

object AndroidGlyphRasterizer {
    private val cache = ConcurrentHashMap<String, VisualShapeMatcher.ShapeFeatures>()
    private val skeletonCache = ConcurrentHashMap<String, List<List<StrokePoint>>>()

    fun getFeaturesForCharacter(character: String): VisualShapeMatcher.ShapeFeatures {
        return cache.getOrPut(character) {
            val grid32 = rasterizeGlyphTo32(character)
            VisualShapeMatcher.extractFeatures(grid32)
        }
    }

    fun getHandwrittenStrokes(character: String, targetStrokeCount: Int): List<List<StrokePoint>>? {
        if (HanziStrokePaths.hasManualStrokes(character)) {
            return HanziStrokePaths.getNormalizedStrokes(character)
        }
        skeletonCache[character]?.let { return it }
        val extracted = extractSkeletonStrokes(character, targetStrokeCount.coerceAtLeast(1))
        if (extracted != null && extracted.isNotEmpty()) {
            skeletonCache[character] = extracted
            return extracted
        }
        return HanziStrokePaths.getNormalizedStrokes(character)
    }

    private fun extractSkeletonStrokes(character: String, targetCount: Int): List<List<StrokePoint>>? {
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
        if (bounds.width() <= 0 || bounds.height() <= 0) {
            bitmap.recycle()
            return null
        }

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

        val binary = BooleanArray(size * size)
        var fgCount = 0
        for (i in pixels.indices) {
            if ((pixels[i] ushr 24) >= 96) {
                binary[i] = true
                fgCount++
            }
        }
        if (fgCount < 12) return null

        // Zhang-Suen thinning to 1-pixel centerline
        zhangSuenThin(binary, size)

        // Trace connected chains of skeleton pixels in top-to-bottom, left-to-right order
        val visited = BooleanArray(size * size)
        val rawChains = ArrayList<List<StrokePoint>>()
        val dirs = arrayOf(
            intArrayOf(1, 0), intArrayOf(1, 1), intArrayOf(0, 1), intArrayOf(-1, 1),
            intArrayOf(-1, 0), intArrayOf(-1, -1), intArrayOf(0, -1), intArrayOf(1, -1)
        )

        for (y in 1 until size - 1) {
            for (x in 1 until size - 1) {
                val idx = y * size + x
                if (!binary[idx] || visited[idx]) continue

                val chain = ArrayList<StrokePoint>()
                var cx = x
                var cy = y
                while (true) {
                    val cIdx = cy * size + cx
                    visited[cIdx] = true
                    chain.add(
                        StrokePoint(
                            x = 12f + (cx / (size - 1f)) * 76f,
                            y = 12f + (cy / (size - 1f)) * 76f
                        )
                    )
                    var nextX = -1
                    var nextY = -1
                    for (d in dirs) {
                        val nx = cx + d[0]
                        val ny = cy + d[1]
                        if (nx in 1 until size - 1 && ny in 1 until size - 1) {
                            val nIdx = ny * size + nx
                            if (binary[nIdx] && !visited[nIdx]) {
                                nextX = nx
                                nextY = ny
                                break
                            }
                        }
                    }
                    if (nextX == -1) break
                    cx = nextX
                    cy = nextY
                }
                if (chain.size >= 3) {
                    val simplified = simplifyChain(chain)
                    if (simplified.size >= 2) {
                        rawChains.add(simplified)
                    }
                }
            }
        }

        if (rawChains.isEmpty()) return null

        // Normalize to match targetCount so stroke order badges stay synchronized
        val ordered = rawChains.sortedWith(
            compareBy<List<StrokePoint>> { (it.first().y / 14f).toInt() }.thenBy { it.first().x }
        ).toMutableList()

        while (ordered.size > targetCount && ordered.size >= 2) {
            // Merge the shortest chain into its nearest neighbor
            var minIdx = 0
            var minSize = ordered[0].size
            for (i in 1 until ordered.size) {
                if (ordered[i].size < minSize) {
                    minSize = ordered[i].size
                    minIdx = i
                }
            }
            val removed = ordered.removeAt(minIdx)
            val mergeTarget = if (minIdx < ordered.size) minIdx else ordered.size - 1
            ordered[mergeTarget] = ordered[mergeTarget] + removed
        }

        return ordered
    }

    private fun simplifyChain(pts: List<StrokePoint>): List<StrokePoint> {
        if (pts.size <= 4) return listOf(pts.first(), pts.last())
        val step = max(2, pts.size / 5)
        val result = ArrayList<StrokePoint>()
        var i = 0
        while (i < pts.size - 1) {
            result.add(pts[i])
            i += step
        }
        result.add(pts.last())
        return result
    }

    private fun zhangSuenThin(grid: BooleanArray, size: Int) {
        val toClear = IntArray(size * size)
        var iter = 0
        while (iter < 12) {
            iter++
            var changed = false
            for (step in 0..1) {
                var clearCount = 0
                for (y in 1 until size - 1) {
                    for (x in 1 until size - 1) {
                        val idx = y * size + x
                        if (!grid[idx]) continue
                        val p2 = if (grid[(y - 1) * size + x]) 1 else 0
                        val p3 = if (grid[(y - 1) * size + (x + 1)]) 1 else 0
                        val p4 = if (grid[y * size + (x + 1)]) 1 else 0
                        val p5 = if (grid[(y + 1) * size + (x + 1)]) 1 else 0
                        val p6 = if (grid[(y + 1) * size + x]) 1 else 0
                        val p7 = if (grid[(y + 1) * size + (x - 1)]) 1 else 0
                        val p8 = if (grid[y * size + (x - 1)]) 1 else 0
                        val p9 = if (grid[(y - 1) * size + (x - 1)]) 1 else 0

                        val b = p2 + p3 + p4 + p5 + p6 + p7 + p8 + p9
                        if (b < 2 || b > 6) continue

                        var a = 0
                        if (p2 == 0 && p3 == 1) a++
                        if (p3 == 0 && p4 == 1) a++
                        if (p4 == 0 && p5 == 1) a++
                        if (p5 == 0 && p6 == 1) a++
                        if (p6 == 0 && p7 == 1) a++
                        if (p7 == 0 && p8 == 1) a++
                        if (p8 == 0 && p9 == 1) a++
                        if (p9 == 0 && p2 == 1) a++
                        if (a != 1) continue

                        val cond = if (step == 0) {
                            (p2 * p4 * p6 == 0) && (p4 * p6 * p8 == 0)
                        } else {
                            (p2 * p4 * p8 == 0) && (p2 * p6 * p8 == 0)
                        }
                        if (cond) {
                            toClear[clearCount++] = idx
                        }
                    }
                }
                if (clearCount > 0) {
                    changed = true
                    for (i in 0 until clearCount) {
                        grid[toClear[i]] = false
                    }
                }
            }
            if (!changed) break
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

@Composable
fun AnimatedHanziBox(
    character: String,
    strokeCount: Int,
    replayTrigger: Int,
    onActiveStrokeIndex: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val handwrittenStrokes = remember(character, strokeCount) {
        AndroidGlyphRasterizer.getHandwrittenStrokes(character, strokeCount)
    }
    val effectiveStrokeCount = handwrittenStrokes?.size ?: strokeCount.coerceAtLeast(1)

    val progress = remember(character, replayTrigger) { Animatable(0f) }

    LaunchedEffect(character, replayTrigger) {
        progress.snapTo(0f)
        // Calm, slower instructional pace (~950 ms per stroke)
        val durationMs = (effectiveStrokeCount * 950).coerceIn(1900, 11000)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = durationMs, easing = LinearEasing)
        )
        onActiveStrokeIndex(-1)
    }

    val p = progress.value
    LaunchedEffect(p, effectiveStrokeCount, strokeCount) {
        val badgeCount = strokeCount.coerceAtLeast(1)
        if (p < 0.995f && badgeCount > 0) {
            val idx = (p * badgeCount).toInt().coerceIn(0, badgeCount - 1)
            onActiveStrokeIndex(idx)
        } else {
            onActiveStrokeIndex(-1)
        }
    }

    val inkColor = MaterialTheme.colorScheme.onSurface
    val accentColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outline

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.5.dp, accentColor.copy(alpha = 0.45f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawTianzigeGrid(gridColor)

                val boxSize = size.minDimension
                val baseStrokeWidth = boxSize * 0.056f

                if (handwrittenStrokes != null) {
                    drawHandwrittenSequence(
                        strokes = handwrittenStrokes,
                        progress = p,
                        boxSize = boxSize,
                        baseWidth = baseStrokeWidth,
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

private fun DrawScope.drawHandwrittenSequence(
    strokes: List<List<StrokePoint>>,
    progress: Float,
    boxSize: Float,
    baseWidth: Float,
    inkColor: Color,
    nibColor: Color
) {
    val scale = boxSize / 100f
    val ghostColor = inkColor.copy(alpha = 0.13f)

    // 1. Faint handwritten guide underneath (all strokes at full normal line thickness)
    for (stroke in strokes) {
        drawCalligraphicStroke(
            pts = stroke,
            fraction = 1f,
            scale = scale,
            baseWidth = baseWidth,
            color = ghostColor
        )
    }

    // 2. Draw completed strokes & active stroke directly as normal solid lines (no fill-in)
    val totalStrokes = strokes.size
    val slotSize = 1f / totalStrokes
    var activeTip: Offset? = null

    for (i in 0 until totalStrokes) {
        val slotStart = i * slotSize
        if (progress <= slotStart) break

        // First 84% of each stroke slot draws the stroke; remaining 16% is a gentle pen-lift pause
        val localRaw = ((progress - slotStart) / (slotSize * 0.84f)).coerceIn(0f, 1f)
        val tip = drawCalligraphicStroke(
            pts = strokes[i],
            fraction = localRaw,
            scale = scale,
            baseWidth = baseWidth,
            color = inkColor
        )
        if (localRaw < 1f && tip != null) {
            activeTip = tip
        }
    }

    // 3. Pen/brush nib marker only while actively writing
    if (activeTip != null && progress < 0.995f) {
        drawCircle(
            color = nibColor.copy(alpha = 0.25f),
            radius = baseWidth * 1.45f,
            center = activeTip
        )
        drawCircle(
            color = nibColor,
            radius = baseWidth * 0.62f,
            center = activeTip
        )
    }
}

private fun DrawScope.drawCalligraphicStroke(
    pts: List<StrokePoint>,
    fraction: Float,
    scale: Float,
    baseWidth: Float,
    color: Color
): Offset? {
    if (pts.size < 2 || fraction <= 0f) return null

    // Build smooth quadratic path in Android Path so PathMeasure samples evenly
    val androidPath = AndroidPath().apply {
        moveTo(pts[0].x * scale, pts[0].y * scale)
        if (pts.size == 2) {
            lineTo(pts[1].x * scale, pts[1].y * scale)
        } else {
            for (i in 1 until pts.size - 1) {
                val currX = pts[i].x * scale
                val currY = pts[i].y * scale
                val nextX = pts[i + 1].x * scale
                val nextY = pts[i + 1].y * scale
                quadTo(currX, currY, (currX + nextX) * 0.5f, (currY + nextY) * 0.5f)
            }
            lineTo(pts.last().x * scale, pts.last().y * scale)
        }
    }

    val pm = PathMeasure(androidPath, false)
    val totalLen = pm.length
    if (totalLen <= 0.5f) return null

    val drawLen = totalLen * fraction.coerceIn(0f, 1f)
    val step = max(2f, totalLen / 28f)

    val posPrev = FloatArray(2)
    val posCurr = FloatArray(2)
    pm.getPosTan(0f, posPrev, null)

    var dist = step
    var lastOffset = Offset(posPrev[0], posPrev[1])

    while (dist < drawLen) {
        pm.getPosTan(dist, posCurr, null)
        val t = (dist / totalLen).coerceIn(0f, 1f)
        val width = calligraphicWidthAt(t, baseWidth)
        val startPt = Offset(posPrev[0], posPrev[1])
        val endPt = Offset(posCurr[0], posCurr[1])
        drawLine(
            color = color,
            start = startPt,
            end = endPt,
            strokeWidth = width,
            cap = StrokeCap.Round
        )
        posPrev[0] = posCurr[0]
        posPrev[1] = posCurr[1]
        lastOffset = endPt
        dist += step
    }

    if (drawLen > 0f) {
        pm.getPosTan(drawLen, posCurr, null)
        val t = (drawLen / totalLen).coerceIn(0f, 1f)
        val width = calligraphicWidthAt(t, baseWidth)
        val endPt = Offset(posCurr[0], posCurr[1])
        drawLine(
            color = color,
            start = Offset(posPrev[0], posPrev[1]),
            end = endPt,
            strokeWidth = width,
            cap = StrokeCap.Round
        )
        lastOffset = endPt
    }

    return lastOffset
}

private fun calligraphicWidthAt(t: Float, baseWidth: Float): Float {
    return when {
        t < 0.14f -> baseWidth * (1.10f - 0.12f * (t / 0.14f))
        t > 0.78f -> baseWidth * (0.98f - 0.34f * ((t - 0.78f) / 0.22f))
        else -> baseWidth * 0.98f
    }
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

    // Faint handwritten-serif guide
    paint.color = inkColor.copy(alpha = 0.13f).toArgb()
    nativeCanvas.drawText(character, x, y, paint)

    // Progressive top-to-bottom solid reveal (no hollow outline, no fade fill)
    val revealHeight = (size.height * progress).coerceIn(0f, size.height)
    clipRect(left = 0f, top = 0f, right = size.width, bottom = revealHeight) {
        paint.color = inkColor.toArgb()
        nativeCanvas.drawText(character, x, y, paint)
    }
}
