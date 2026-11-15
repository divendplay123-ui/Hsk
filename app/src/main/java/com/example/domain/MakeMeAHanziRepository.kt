package com.example.domain

import android.graphics.Path as AndroidPath
import android.graphics.PathMeasure
import java.io.BufferedReader
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

data class MakeMeAHanziStroke(
    val outlinePath: AndroidPath,
    val medianPath: AndroidPath,
    val medianLength: Float,
    val tipStart: StrokePoint,
    val tipEnd: StrokePoint,
    val durationWeight: Float
)

data class MakeMeAHanziGlyph(
    val character: String,
    val strokes: List<MakeMeAHanziStroke>,
    val totalWeight: Float
)

object MakeMeAHanziRepository {

    private val rawSpecs: Map<String, Pair<String, String>> by lazy {
        loadRawSpecs()
    }

    private val parsedCache = ConcurrentHashMap<String, MakeMeAHanziGlyph>()

    fun preload() {
        rawSpecs.size
    }

    fun hasGlyph(character: String): Boolean = rawSpecs.containsKey(character)

    fun getGlyph(character: String): MakeMeAHanziGlyph? {
        parsedCache[character]?.let { return it }
        val raw = rawSpecs[character] ?: return null
        val parsed = parseGlyph(character, raw.first, raw.second) ?: return null
        parsedCache[character] = parsed
        return parsed
    }

    private fun parseGlyph(
        character: String,
        strokesRaw: String,
        mediansRaw: String
    ): MakeMeAHanziGlyph? {
        val strokeSvgList = strokesRaw.split('|')
        val medianStrList = mediansRaw.split('|')
        val count = min(strokeSvgList.size, medianStrList.size)
        if (count == 0) return null

        val strokes = ArrayList<MakeMeAHanziStroke>(count)
        var totalWeight = 0f

        for (i in 0 until count) {
            val outline = parseSvgPathTo1024(strokeSvgList[i]) ?: continue
            val pts = parseMedianPointsTo1024(medianStrList[i])
            if (pts.size < 2) continue

            val rawLen = polylineLength(pts)
            val extendedPts = extendEnds(pts, rawLen)
            val medianPath = AndroidPath().apply {
                moveTo(extendedPts[0].x, extendedPts[0].y)
                for (k in 1 until extendedPts.size) {
                    lineTo(extendedPts[k].x, extendedPts[k].y)
                }
            }
            val pm = PathMeasure(medianPath, false)
            val medianLen = max(1f, pm.length)

            // Proportional duration weight: short dots ~0.68, medium strokes ~1.0, long bends ~1.45
            val weight = (0.45f + sqrt(rawLen / 1024f) * 0.95f).coerceIn(0.60f, 1.55f)
            totalWeight += weight

            strokes.add(
                MakeMeAHanziStroke(
                    outlinePath = outline,
                    medianPath = medianPath,
                    medianLength = medianLen,
                    tipStart = pts.first(),
                    tipEnd = pts.last(),
                    durationWeight = weight
                )
            )
        }

        if (strokes.isEmpty()) return null
        return MakeMeAHanziGlyph(
            character = character,
            strokes = strokes,
            totalWeight = max(0.01f, totalWeight)
        )
    }

    private fun parseMedianPointsTo1024(medianStr: String): List<StrokePoint> {
        val result = ArrayList<StrokePoint>()
        medianStr.trim().split(' ').forEach { token ->
            if (token.isBlank()) return@forEach
            val comma = token.indexOf(',')
            if (comma > 0) {
                val x = token.substring(0, comma).toFloatOrNull()
                val y = token.substring(comma + 1).toFloatOrNull()
                if (x != null && y != null) {
                    // MakeMeAHanzi coordinate system: X in [0..1024], Y up from [-124..900]
                    result.add(StrokePoint(x = x, y = 900f - y))
                }
            }
        }
        return result
    }

    private fun extendEnds(pts: List<StrokePoint>, rawLen: Float): List<StrokePoint> {
        if (pts.size < 2 || rawLen < 1f) return pts
        val ext = min(38f, rawLen * 0.22f)
        val out = ArrayList<StrokePoint>(pts.size + 2)

        val s0 = pts[0]
        val s1 = pts[1]
        val sDx = s1.x - s0.x
        val sDy = s1.y - s0.y
        val sLen = hypot(sDx, sDy)
        if (sLen > 0.5f) {
            out.add(StrokePoint(s0.x - (sDx / sLen) * ext, s0.y - (sDy / sLen) * ext))
        }
        out.addAll(pts)

        val e0 = pts[pts.size - 2]
        val e1 = pts[pts.size - 1]
        val eDx = e1.x - e0.x
        val eDy = e1.y - e0.y
        val eLen = hypot(eDx, eDy)
        if (eLen > 0.5f) {
            out.add(StrokePoint(e1.x + (eDx / eLen) * ext, e1.y + (eDy / eLen) * ext))
        }
        return out
    }

    private fun polylineLength(pts: List<StrokePoint>): Float {
        var sum = 0f
        for (i in 1 until pts.size) {
            sum += hypot(pts[i].x - pts[i - 1].x, pts[i].y - pts[i - 1].y)
        }
        return sum
    }

    private fun parseSvgPathTo1024(d: String): AndroidPath? {
        val tokens = d.trim().split(Regex("\\s+"))
        if (tokens.isEmpty()) return null

        val path = AndroidPath()
        var idx = 0
        var cmd = ' '
        var hasContent = false

        fun nextFloat(): Float? {
            if (idx >= tokens.size) return null
            val v = tokens[idx].toFloatOrNull() ?: return null
            idx++
            return v
        }

        while (idx < tokens.size) {
            val t = tokens[idx]
            if (t.length == 1 && t[0] in charArrayOf('M', 'L', 'Q', 'C', 'Z')) {
                cmd = t[0]
                idx++
                if (cmd == 'Z') {
                    path.close()
                }
                continue
            }

            when (cmd) {
                'M' -> {
                    val x = nextFloat() ?: break
                    val y = nextFloat() ?: break
                    path.moveTo(x, 900f - y)
                    hasContent = true
                    cmd = 'L'
                }
                'L' -> {
                    val x = nextFloat() ?: break
                    val y = nextFloat() ?: break
                    path.lineTo(x, 900f - y)
                    hasContent = true
                }
                'Q' -> {
                    val cx = nextFloat() ?: break
                    val cy = nextFloat() ?: break
                    val x = nextFloat() ?: break
                    val y = nextFloat() ?: break
                    path.quadTo(cx, 900f - cy, x, 900f - y)
                    hasContent = true
                }
                'C' -> {
                    val c1x = nextFloat() ?: break
                    val c1y = nextFloat() ?: break
                    val c2x = nextFloat() ?: break
                    val c2y = nextFloat() ?: break
                    val x = nextFloat() ?: break
                    val y = nextFloat() ?: break
                    path.cubicTo(c1x, 900f - c1y, c2x, 900f - c2y, x, 900f - y)
                    hasContent = true
                }
                else -> idx++
            }
        }

        return if (hasContent) path else null
    }

    private fun loadRawSpecs(): Map<String, Pair<String, String>> {
        val map = HashMap<String, Pair<String, String>>(1200)
        openResourceReader("hsk/hanzi_strokes_svg.tsv")?.useLines { lines ->
            lines.forEach { rawLine ->
                val line = rawLine.trimEnd('\n', '\r')
                if (line.isEmpty()) return@forEach
                val parts = line.split('\t')
                if (parts.size >= 3) {
                    map[parts[0]] = parts[1] to parts[2]
                }
            }
        }
        return map
    }

    private fun openResourceReader(resourcePath: String): BufferedReader? {
        val stream: InputStream? = MakeMeAHanziRepository::class.java.classLoader?.getResourceAsStream(resourcePath)
            ?: File("app/src/main/resources/$resourcePath").takeIf { it.exists() }?.inputStream()
            ?: File("src/main/resources/$resourcePath").takeIf { it.exists() }?.inputStream()
        return stream?.let { BufferedReader(InputStreamReader(it, StandardCharsets.UTF_8)) }
    }
}
