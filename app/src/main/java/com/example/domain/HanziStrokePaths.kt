package com.example.domain

import com.example.data.model.StrokeType
import com.example.data.repository.HanziCatalog
import java.util.concurrent.ConcurrentHashMap

object HanziStrokePaths {

    private val allSpecs: Map<String, String> by lazy {
        hsk1StrokePathSpecs + hsk2To4StrokePathSpecs
    }

    private val parsedCache = ConcurrentHashMap<String, List<List<StrokePoint>>>()

    fun hasManualStrokes(character: String): Boolean = allSpecs.containsKey(character)

    fun getNormalizedStrokes(character: String): List<List<StrokePoint>>? {
        parsedCache[character]?.let { return it }
        val spec = allSpecs[character]
        if (spec != null) {
            val parsed = spec.split("|").mapNotNull { strokeStr ->
                val pts = strokeStr.trim()
                    .split(" ")
                    .filter { it.isNotBlank() }
                    .mapNotNull { pair ->
                        val parts = pair.split(",")
                        if (parts.size == 2) {
                            val x = parts[0].toFloatOrNull()
                            val y = parts[1].toFloatOrNull()
                            if (x != null && y != null) StrokePoint(x, y) else null
                        } else null
                    }
                pts.takeIf { it.size >= 2 }
            }
            if (parsed.isNotEmpty()) {
                parsedCache[character] = parsed
                return parsed
            }
        }

        val entry = HanziCatalog.findByCharacter(character) ?: return null
        val synthesized = synthesizeStrokesFromTypes(entry.strokeTypes)
        if (synthesized.isNotEmpty()) {
            parsedCache[character] = synthesized
            return synthesized
        }
        return null
    }

    private fun synthesizeStrokesFromTypes(types: List<StrokeType>): List<List<StrokePoint>> {
        val total = types.size
        if (total == 0) return emptyList()
        val cols = when {
            total <= 3 -> 1
            total <= 8 -> 2
            else -> 3
        }
        val rows = ((total + cols - 1) / cols).coerceAtLeast(1)

        return types.mapIndexed { idx, type ->
            val r = idx / cols
            val c = idx % cols
            val cellLeft = 16f + c * (68f / cols)
            val cellRight = 16f + (c + 1) * (68f / cols)
            val cellTop = 16f + r * (68f / rows)
            val cellBottom = 16f + (r + 1) * (68f / rows)
            val cx = (cellLeft + cellRight) * 0.5f
            val cy = (cellTop + cellBottom) * 0.5f
            val hw = ((cellRight - cellLeft) * 0.36f).coerceIn(8f, 26f)
            val hh = ((cellBottom - cellTop) * 0.36f).coerceIn(8f, 26f)

            when (type) {
                StrokeType.HORIZONTAL -> listOf(
                    StrokePoint((cx - hw).coerceIn(12f, 88f), cy.coerceIn(12f, 88f)),
                    StrokePoint((cx + hw).coerceIn(12f, 88f), (cy - 1.2f).coerceIn(12f, 88f))
                )
                StrokeType.VERTICAL -> listOf(
                    StrokePoint(cx.coerceIn(12f, 88f), (cy - hh).coerceIn(12f, 88f)),
                    StrokePoint(cx.coerceIn(12f, 88f), (cy + hh).coerceIn(12f, 88f))
                )
                StrokeType.LEFT_FALLING -> listOf(
                    StrokePoint((cx + hw * 0.65f).coerceIn(12f, 88f), (cy - hh * 0.8f).coerceIn(12f, 88f)),
                    StrokePoint(cx.coerceIn(12f, 88f), cy.coerceIn(12f, 88f)),
                    StrokePoint((cx - hw * 0.75f).coerceIn(12f, 88f), (cy + hh * 0.85f).coerceIn(12f, 88f))
                )
                StrokeType.DOT_OR_RIGHT_FALLING -> listOf(
                    StrokePoint((cx - hw * 0.55f).coerceIn(12f, 88f), (cy - hh * 0.65f).coerceIn(12f, 88f)),
                    StrokePoint((cx + hw * 0.65f).coerceIn(12f, 88f), (cy + hh * 0.70f).coerceIn(12f, 88f))
                )
                StrokeType.TURNING_OR_HOOK -> listOf(
                    StrokePoint((cx - hw * 0.7f).coerceIn(12f, 88f), (cy - hh * 0.65f).coerceIn(12f, 88f)),
                    StrokePoint((cx + hw * 0.65f).coerceIn(12f, 88f), (cy - hh * 0.65f).coerceIn(12f, 88f)),
                    StrokePoint((cx + hw * 0.55f).coerceIn(12f, 88f), (cy + hh * 0.75f).coerceIn(12f, 88f))
                )
            }
        }
    }
}
