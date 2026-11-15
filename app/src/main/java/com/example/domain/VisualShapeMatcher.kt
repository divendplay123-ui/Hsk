package com.example.domain

import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

object VisualShapeMatcher {

    const val GRID_SIZE = 32
    private const val BLUR_SIZE = 16
    private const val CELL_GRID = 4
    private const val ORIENTATION_BINS = 4

    data class ShapeFeatures(
        val blurredSpatial16: FloatArray,
        val orientation4x4: FloatArray,
        val logAspect: Float
    )

    fun rasterizeStrokesTo32(strokes: List<DrawnStroke>): FloatArray? {
        val allPoints = strokes.flatMap { it.points }
        if (allPoints.size < 2) return null

        val minX = allPoints.minOf { it.x }
        val maxX = allPoints.maxOf { it.x }
        val minY = allPoints.minOf { it.y }
        val maxY = allPoints.maxOf { it.y }

        val w = max(1f, maxX - minX)
        val h = max(1f, maxY - minY)
        val scaleBase = max(w, h)
        if (scaleBase < 4f) return null

        val activeSpan = 24f
        val scale = activeSpan / scaleBase
        val centerX = (minX + maxX) * 0.5f
        val centerY = (minY + maxY) * 0.5f
        val gridCenter = GRID_SIZE * 0.5f

        val grid = FloatArray(GRID_SIZE * GRID_SIZE)
        val radius = 1.65f

        for (stroke in strokes) {
            val pts = stroke.points
            if (pts.isEmpty()) continue
            if (pts.size == 1) {
                val gx = (pts[0].x - centerX) * scale + gridCenter
                val gy = (pts[0].y - centerY) * scale + gridCenter
                drawCapsule(grid, gx, gy, gx, gy, radius)
                continue
            }
            for (i in 1 until pts.size) {
                val x1 = (pts[i - 1].x - centerX) * scale + gridCenter
                val y1 = (pts[i - 1].y - centerY) * scale + gridCenter
                val x2 = (pts[i].x - centerX) * scale + gridCenter
                val y2 = (pts[i].y - centerY) * scale + gridCenter
                drawCapsule(grid, x1, y1, x2, y2, radius)
            }
        }
        return grid
    }

    fun extractFeatures(grid32: FloatArray): ShapeFeatures {
        val spatial16 = downsample32To16(grid32)
        val blurred1 = blur16(spatial16)
        val blurred2 = blur16(blurred1)
        l2NormalizeInPlace(blurred2)

        val orientation = extractOrientation4x4(grid32)
        l2NormalizeInPlace(orientation)

        val aspect = computeAspectLogFromGrid(grid32)
        return ShapeFeatures(
            blurredSpatial16 = blurred2,
            orientation4x4 = orientation,
            logAspect = aspect
        )
    }

    fun computeVisualSimilarity(user: ShapeFeatures, template: ShapeFeatures): Float {
        val spatialSim = dotProduct(user.blurredSpatial16, template.blurredSpatial16).coerceIn(0f, 1f)
        val orientSim = dotProduct(user.orientation4x4, template.orientation4x4).coerceIn(0f, 1f)
        val aspectDiff = abs(user.logAspect - template.logAspect)
        val aspectSim = (1f - (aspectDiff / 2.2f)).coerceIn(0f, 1f)

        return (0.52f * spatialSim + 0.36f * orientSim + 0.12f * aspectSim).coerceIn(0f, 1f)
    }

    private fun drawCapsule(
        grid: FloatArray,
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        radius: Float
    ) {
        val minGx = max(0, (min(x1, x2) - radius - 1f).toInt())
        val maxGx = min(GRID_SIZE - 1, (max(x1, x2) + radius + 1f).toInt())
        val minGy = max(0, (min(y1, y2) - radius - 1f).toInt())
        val maxGy = min(GRID_SIZE - 1, (max(y1, y2) + radius + 1f).toInt())

        val dx = x2 - x1
        val dy = y2 - y1
        val lenSq = dx * dx + dy * dy

        for (py in minGy..maxGy) {
            for (px in minGx..maxGx) {
                val cx = px + 0.5f
                val cy = py + 0.5f
                val t = if (lenSq > 0.0001f) {
                    (((cx - x1) * dx + (cy - y1) * dy) / lenSq).coerceIn(0f, 1f)
                } else {
                    0f
                }
                val projX = x1 + t * dx
                val projY = y1 + t * dy
                val dist = hypot(cx - projX, cy - projY)
                if (dist <= radius) {
                    val alpha = (1f - (dist / (radius + 0.35f))).coerceIn(0.25f, 1f)
                    val idx = py * GRID_SIZE + px
                    if (alpha > grid[idx]) {
                        grid[idx] = alpha
                    }
                }
            }
        }
    }

    private fun downsample32To16(grid32: FloatArray): FloatArray {
        val out = FloatArray(BLUR_SIZE * BLUR_SIZE)
        for (y in 0 until BLUR_SIZE) {
            for (x in 0 until BLUR_SIZE) {
                val sx = x * 2
                val sy = y * 2
                val sum = grid32[sy * GRID_SIZE + sx] +
                    grid32[sy * GRID_SIZE + sx + 1] +
                    grid32[(sy + 1) * GRID_SIZE + sx] +
                    grid32[(sy + 1) * GRID_SIZE + sx + 1]
                out[y * BLUR_SIZE + x] = sum * 0.25f
            }
        }
        return out
    }

    private fun blur16(src: FloatArray): FloatArray {
        val dst = FloatArray(BLUR_SIZE * BLUR_SIZE)
        for (y in 0 until BLUR_SIZE) {
            for (x in 0 until BLUR_SIZE) {
                var sum = 0f
                var weightSum = 0f
                for (ky in -1..1) {
                    val ny = y + ky
                    if (ny !in 0 until BLUR_SIZE) continue
                    for (kx in -1..1) {
                        val nx = x + kx
                        if (nx !in 0 until BLUR_SIZE) continue
                        val w = when {
                            kx == 0 && ky == 0 -> 4f
                            kx == 0 || ky == 0 -> 2f
                            else -> 1f
                        }
                        sum += src[ny * BLUR_SIZE + nx] * w
                        weightSum += w
                    }
                }
                dst[y * BLUR_SIZE + x] = if (weightSum > 0f) sum / weightSum else 0f
            }
        }
        return dst
    }

    private fun extractOrientation4x4(grid32: FloatArray): FloatArray {
        val bins = FloatArray(CELL_GRID * CELL_GRID * ORIENTATION_BINS)
        val cellPx = GRID_SIZE / CELL_GRID

        for (y in 1 until GRID_SIZE - 1) {
            val cellY = y / cellPx
            for (x in 1 until GRID_SIZE - 1) {
                val cellX = x / cellPx

                val tl = grid32[(y - 1) * GRID_SIZE + (x - 1)]
                val tc = grid32[(y - 1) * GRID_SIZE + x]
                val tr = grid32[(y - 1) * GRID_SIZE + (x + 1)]
                val ml = grid32[y * GRID_SIZE + (x - 1)]
                val mr = grid32[y * GRID_SIZE + (x + 1)]
                val bl = grid32[(y + 1) * GRID_SIZE + (x - 1)]
                val bc = grid32[(y + 1) * GRID_SIZE + x]
                val br = grid32[(y + 1) * GRID_SIZE + (x + 1)]

                val gx = (tr + 2f * mr + br) - (tl + 2f * ml + bl)
                val gy = (bl + 2f * bc + br) - (tl + 2f * tc + tr)
                val mag = hypot(gx, gy)
                if (mag < 0.08f) continue

                val absX = abs(gx)
                val absY = abs(gy)
                val baseOffset = (cellY * CELL_GRID + cellX) * ORIENTATION_BINS

                // Remember: stroke tangent is perpendicular to gradient (gx, gy).
                // Horizontal stroke => large |gy|, small |gx|
                // Vertical stroke => large |gx|, small |gy|
                if (absY > absX * 1.6f) {
                    bins[baseOffset + 0] += mag // Horizontal
                } else if (absX > absY * 1.6f) {
                    bins[baseOffset + 1] += mag // Vertical
                } else if (gx * gy > 0f) {
                    bins[baseOffset + 2] += mag // Slash /
                } else {
                    bins[baseOffset + 3] += mag // Backslash \
                }
            }
        }
        return bins
    }

    private fun computeAspectLogFromGrid(grid32: FloatArray): Float {
        var minX = GRID_SIZE
        var maxX = -1
        var minY = GRID_SIZE
        var maxY = -1
        for (y in 0 until GRID_SIZE) {
            for (x in 0 until GRID_SIZE) {
                if (grid32[y * GRID_SIZE + x] > 0.15f) {
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                }
            }
        }
        if (maxX < minX || maxY < minY) return 0f
        val w = max(2f, (maxX - minX + 1).toFloat())
        val h = max(2f, (maxY - minY + 1).toFloat())
        return ln((w / h).toDouble()).toFloat()
    }

    private fun l2NormalizeInPlace(vec: FloatArray) {
        var sumSq = 0f
        for (v in vec) sumSq += v * v
        val norm = sqrt(sumSq)
        if (norm > 1e-5f) {
            for (i in vec.indices) vec[i] /= norm
        }
    }

    private fun dotProduct(a: FloatArray, b: FloatArray): Float {
        var sum = 0f
        val n = min(a.size, b.size)
        for (i in 0 until n) sum += a[i] * b[i]
        return sum
    }
}
