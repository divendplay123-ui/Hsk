package com.example.domain

import com.example.data.model.HskLevel
import com.example.data.model.HskStandard
import com.example.data.model.RecognizedCandidate
import com.example.data.model.StrokeType
import com.example.data.repository.HanziCatalog
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

data class StrokePoint(val x: Float, val y: Float)

data class DrawnStroke(
    val points: List<StrokePoint>,
    val classifiedType: StrokeType,
    val typeScores: Map<StrokeType, Float>
)

object StrokeRecognizer {

    fun classifyDrawnStroke(
        rawPoints: List<StrokePoint>,
        canvasSize: Float
    ): DrawnStroke {
        if (rawPoints.size < 2) {
            val dotScores = mapOf(
                StrokeType.DOT_OR_RIGHT_FALLING to 0.90f,
                StrokeType.HORIZONTAL to 0.25f,
                StrokeType.VERTICAL to 0.25f,
                StrokeType.LEFT_FALLING to 0.20f,
                StrokeType.TURNING_OR_HOOK to 0.10f
            )
            return DrawnStroke(rawPoints, StrokeType.DOT_OR_RIGHT_FALLING, dotScores)
        }

        val resampled = resamplePoints(rawPoints, targetCount = 20)
        val start = resampled.first()
        val end = resampled.last()
        val dx = end.x - start.x
        val dy = end.y - start.y
        val netDist = hypot(dx, dy)
        val pathLength = totalLength(resampled)

        val minX = resampled.minOf { it.x }
        val maxX = resampled.maxOf { it.x }
        val minY = resampled.minOf { it.y }
        val maxY = resampled.maxOf { it.y }
        val boxW = max(1f, maxX - minX)
        val boxH = max(1f, maxY - minY)

        val safeCanvas = max(100f, canvasSize)
        val normalizedLen = pathLength / safeCanvas

        val maxTurnDeg = computeMaxCornerAngleDeg(resampled)
        val totalTurnDeg = computeTotalTurnDeg(resampled)

        val scores = mutableMapOf(
            StrokeType.HORIZONTAL to 0.05f,
            StrokeType.VERTICAL to 0.05f,
            StrokeType.LEFT_FALLING to 0.05f,
            StrokeType.DOT_OR_RIGHT_FALLING to 0.05f,
            StrokeType.TURNING_OR_HOOK to 0.05f
        )

        if (normalizedLen < 0.14f && maxTurnDeg < 55f) {
            scores[StrokeType.DOT_OR_RIGHT_FALLING] = 0.92f
            scores[StrokeType.HORIZONTAL] = 0.35f
            scores[StrokeType.LEFT_FALLING] = 0.30f
            scores[StrokeType.VERTICAL] = 0.25f
        } else {
            val straightness = if (pathLength > 1f) (netDist / pathLength).coerceIn(0f, 1f) else 1f
            val angleDeg = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()

            val hasSharpTurn = maxTurnDeg >= 48f || straightness < 0.72f || totalTurnDeg >= 82f
            if (hasSharpTurn) {
                val turnConfidence = (
                    0.50f +
                        ((maxTurnDeg - 35f).coerceIn(0f, 90f) / 180f) +
                        ((1f - straightness) * 0.45f)
                    ).coerceIn(0.45f, 0.98f)
                scores[StrokeType.TURNING_OR_HOOK] = turnConfidence
            }

            if (dy > 0 && boxH > boxW * 1.4f && abs(dx) < boxH * 0.55f) {
                scores[StrokeType.VERTICAL] = max(scores[StrokeType.VERTICAL]!!, 0.85f)
                if (maxTurnDeg >= 38f) {
                    scores[StrokeType.TURNING_OR_HOOK] = max(scores[StrokeType.TURNING_OR_HOOK]!!, 0.76f)
                }
            }

            if (abs(dx) > abs(dy) * 1.65f && maxTurnDeg < 52f) {
                val horizScore = if (dx >= 0) 0.94f else 0.65f
                scores[StrokeType.HORIZONTAL] = max(scores[StrokeType.HORIZONTAL]!!, horizScore)
                if (angleDeg in -40f..-10f) {
                    scores[StrokeType.DOT_OR_RIGHT_FALLING] = max(scores[StrokeType.DOT_OR_RIGHT_FALLING]!!, 0.55f)
                }
            }

            if (dy > 0 && abs(dy) > abs(dx) * 1.75f && maxTurnDeg < 52f) {
                scores[StrokeType.VERTICAL] = max(scores[StrokeType.VERTICAL]!!, 0.94f)
            }

            if (dy > 0 && dx < -boxH * 0.18f && maxTurnDeg < 60f) {
                val pieScore = (0.78f + (abs(dx) / (abs(dx) + dy + 1f)) * 0.20f).coerceAtMost(0.96f)
                scores[StrokeType.LEFT_FALLING] = max(scores[StrokeType.LEFT_FALLING]!!, pieScore)
                if (abs(dx) < dy * 0.35f) {
                    scores[StrokeType.VERTICAL] = max(scores[StrokeType.VERTICAL]!!, 0.52f)
                }
            }

            if (dy > 0 && dx > boxH * 0.22f && maxTurnDeg < 60f) {
                val naScore = (0.80f + (dx / (dx + dy + 1f)) * 0.18f).coerceAtMost(0.96f)
                scores[StrokeType.DOT_OR_RIGHT_FALLING] = max(scores[StrokeType.DOT_OR_RIGHT_FALLING]!!, naScore)
            }
        }

        val bestType = scores.maxByOrNull { it.value }?.key ?: StrokeType.HORIZONTAL
        return DrawnStroke(
            points = rawPoints,
            classifiedType = bestType,
            typeScores = scores
        )
    }

    fun fromManualStroke(type: StrokeType): DrawnStroke {
        val scores = StrokeType.entries.associateWith { if (it == type) 1.0f else 0.08f }
        return DrawnStroke(
            points = emptyList(),
            classifiedType = type,
            typeScores = scores
        )
    }

    fun matchCandidates(
        drawnStrokes: List<DrawnStroke>,
        enabledLevels: Set<HskLevel>,
        strictStrokeCount: Boolean,
        hskStandard: HskStandard = HskStandard.HSK_3_0,
        templateFeaturesProvider: ((String) -> VisualShapeMatcher.ShapeFeatures)? = null,
        limit: Int = 12
    ): List<RecognizedCandidate> {
        if (drawnStrokes.isEmpty()) return emptyList()

        val pool = HanziCatalog.entriesForLevels(enabledLevels, hskStandard)
        val inputLen = drawnStrokes.size

        // Rasterize drawn strokes if visual points exist (allows matching merged strokes like '口' in 1 square)
        val userGrid32 = VisualShapeMatcher.rasterizeStrokesTo32(drawnStrokes)
        val userVisualFeatures = if (userGrid32 != null && templateFeaturesProvider != null) {
            VisualShapeMatcher.extractFeatures(userGrid32)
        } else {
            null
        }

        val scored = pool.mapNotNull { entry ->
            val targetTypes = entry.strokeTypes
            val targetLen = targetTypes.size

            if (strictStrokeCount && targetLen != inputLen) {
                return@mapNotNull null
            }

            val fullDistance = softEditDistance(drawnStrokes, targetTypes)
            val maxLen = max(inputLen, targetLen).toFloat()
            val fullSimilarity = (1f - (fullDistance / maxLen)).coerceIn(0f, 1f)

            val prefixSimilarity = if (inputLen <= targetLen) {
                val prefixTarget = targetTypes.subList(0, inputLen)
                val prefixDist = softEditDistance(drawnStrokes, prefixTarget)
                (1f - (prefixDist / inputLen.toFloat())).coerceIn(0f, 1f)
            } else {
                0f
            }

            val countDiff = abs(targetLen - inputLen)
            val strokeSeqScore = if (userVisualFeatures != null) {
                // Gentle stroke hint when drawing freehand so merged/split strokes never fail
                val gentleCountScore = when {
                    countDiff == 0 -> 0.22f
                    countDiff == 1 -> 0.12f
                    countDiff == 2 -> 0.05f
                    else -> 0f
                }
                (fullSimilarity * 0.78f + gentleCountScore).coerceIn(0f, 1f)
            } else {
                // Pure manual stroke button mode
                val countBonus = when {
                    countDiff == 0 -> 0.24f
                    countDiff == 1 -> 0.06f
                    inputLen < targetLen -> 0.02f - (targetLen - inputLen) * 0.025f
                    else -> -0.18f * countDiff
                }
                if (inputLen == targetLen) {
                    fullSimilarity * 0.80f + countBonus
                } else if (inputLen < targetLen) {
                    max(fullSimilarity * 0.72f, prefixSimilarity * 0.70f) + countBonus
                } else {
                    fullSimilarity * 0.75f + countBonus
                }
            }

            val combinedScore = if (userVisualFeatures != null && templateFeaturesProvider != null) {
                val templateFeatures = templateFeaturesProvider(entry.character)
                val visualScore = VisualShapeMatcher.computeVisualSimilarity(
                    userVisualFeatures,
                    templateFeatures
                )
                // Visual drawing shape dominates (82%) so drawing '口' as 1 square matches '口' #1!
                (0.82f * visualScore + 0.18f * strokeSeqScore).coerceIn(0.01f, 0.99f)
            } else {
                strokeSeqScore.coerceIn(0.01f, 0.99f)
            }

            val threshold = if (userVisualFeatures != null) 0.34f else 0.22f
            if (combinedScore < threshold) return@mapNotNull null

            RecognizedCandidate(
                entry = entry,
                confidence = combinedScore
            )
        }

        return scored
            .sortedWith(
                compareByDescending<RecognizedCandidate> { it.confidence }
                    .thenBy { abs(it.entry.strokeCount - inputLen) }
                    .thenBy { it.entry.hskLevel.number }
            )
            .take(limit)
    }

    private fun softEditDistance(
        drawn: List<DrawnStroke>,
        target: List<StrokeType>
    ): Float {
        val n = drawn.size
        val m = target.size
        val dp = Array(n + 1) { FloatArray(m + 1) }

        for (i in 0..n) dp[i][0] = i * 0.85f
        for (j in 0..m) dp[0][j] = j * 0.85f

        for (i in 1..n) {
            val drawnStroke = drawn[i - 1]
            for (j in 1..m) {
                val targetType = target[j - 1]
                val matchProb = drawnStroke.typeScores[targetType] ?: 0.05f
                val subCost = (1f - matchProb).coerceIn(0f, 1f)
                dp[i][j] = min(
                    dp[i - 1][j - 1] + subCost,
                    min(
                        dp[i - 1][j] + 0.85f,
                        dp[i][j - 1] + 0.85f
                    )
                )
            }
        }
        return dp[n][m]
    }

    private fun resamplePoints(points: List<StrokePoint>, targetCount: Int): List<StrokePoint> {
        if (points.size <= 2) return points
        val totalLen = totalLength(points)
        if (totalLen < 1f) return points

        val interval = totalLen / (targetCount - 1)
        val result = mutableListOf(points.first())
        var accumulated = 0f
        val mutable = points.toMutableList()
        var i = 1
        while (i < mutable.size) {
            val prev = mutable[i - 1]
            val curr = mutable[i]
            val dist = hypot(curr.x - prev.x, curr.y - prev.y)
            if (accumulated + dist >= interval && dist > 0.0001f) {
                val ratio = (interval - accumulated) / dist
                val nx = prev.x + ratio * (curr.x - prev.x)
                val ny = prev.y + ratio * (curr.y - prev.y)
                val interpolated = StrokePoint(nx, ny)
                result.add(interpolated)
                mutable.add(i, interpolated)
                accumulated = 0f
            } else {
                accumulated += dist
            }
            i++
        }
        while (result.size < targetCount) {
            result.add(points.last())
        }
        return result.take(targetCount)
    }

    private fun totalLength(points: List<StrokePoint>): Float {
        var len = 0f
        for (i in 1 until points.size) {
            len += hypot(points[i].x - points[i - 1].x, points[i].y - points[i - 1].y)
        }
        return len
    }

    private fun computeMaxCornerAngleDeg(points: List<StrokePoint>): Float {
        if (points.size < 7) return 0f
        var maxAngle = 0f
        val stride = 3
        for (i in stride until points.size - stride) {
            val v1x = points[i].x - points[i - stride].x
            val v1y = points[i].y - points[i - stride].y
            val v2x = points[i + stride].x - points[i].x
            val v2y = points[i + stride].y - points[i].y
            val m1 = hypot(v1x, v1y)
            val m2 = hypot(v2x, v2y)
            if (m1 > 2f && m2 > 2f) {
                val cosTheta = ((v1x * v2x + v1y * v2y) / (m1 * m2)).coerceIn(-1f, 1f)
                val angle = Math.toDegrees(acos(cosTheta.toDouble())).toFloat()
                if (angle > maxAngle) maxAngle = angle
            }
        }
        return maxAngle
    }

    private fun computeTotalTurnDeg(points: List<StrokePoint>): Float {
        if (points.size < 7) return 0f
        var sum = 0f
        val stride = 3
        var i = stride
        while (i < points.size - stride) {
            val v1x = points[i].x - points[i - stride].x
            val v1y = points[i].y - points[i - stride].y
            val v2x = points[i + stride].x - points[i].x
            val v2y = points[i + stride].y - points[i].y
            val m1 = hypot(v1x, v1y)
            val m2 = hypot(v2x, v2y)
            if (m1 > 2f && m2 > 2f) {
                val cosTheta = ((v1x * v2x + v1y * v2y) / (m1 * m2)).coerceIn(-1f, 1f)
                val angle = Math.toDegrees(acos(cosTheta.toDouble())).toFloat()
                if (angle > 18f) sum += angle
            }
            i += stride
        }
        return sum
    }
}
