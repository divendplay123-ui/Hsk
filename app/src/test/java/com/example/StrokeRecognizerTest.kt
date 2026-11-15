package com.example

import com.example.data.model.HskLevel
import com.example.data.model.HskStandard
import com.example.data.model.StrokeType
import com.example.data.repository.HanziCatalog
import com.example.domain.DrawnStroke
import com.example.domain.HanziStrokePaths
import com.example.domain.MakeMeAHanziRepository
import com.example.domain.StrokePoint
import com.example.domain.StrokeRecognizer
import com.example.domain.VisualShapeMatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StrokeRecognizerTest {

    @Test
    fun catalogEntriesAreWellFormed() {
        // Official HSK 3.0 word counts from the 4 PDFs + UNI block
        assertEquals(300, HanziCatalog.wordCountForLevel(HskLevel.HSK1))
        assertEquals(200, HanziCatalog.wordCountForLevel(HskLevel.HSK2))
        assertEquals(500, HanziCatalog.wordCountForLevel(HskLevel.HSK3))
        assertEquals(1000, HanziCatalog.wordCountForLevel(HskLevel.HSK4))
        assertEquals(49, HanziCatalog.wordCountForLevel(HskLevel.UNI))
        assertEquals(2049, HanziCatalog.allWords.size)

        // Official HSK 3.0 unique character counts + UNI block
        assertEquals(248, HanziCatalog.countForLevel(HskLevel.HSK1))
        assertEquals(123, HanziCatalog.countForLevel(HskLevel.HSK2))
        assertEquals(284, HanziCatalog.countForLevel(HskLevel.HSK3))
        assertEquals(441, HanziCatalog.countForLevel(HskLevel.HSK4))
        assertEquals(5, HanziCatalog.countForLevel(HskLevel.UNI))
        assertEquals(1101, HanziCatalog.allEntries.size)

        HanziCatalog.allEntries.forEach { entry ->
            assertTrue("Empty character", entry.character.isNotBlank())
            assertTrue("Empty pinyin for ${entry.character}", entry.pinyin.isNotBlank())
            assertTrue("Empty meaningRu for ${entry.character}", entry.meaningRu.isNotBlank())
            assertTrue(
                "Invalid strokeCode '${entry.strokeCode}' for ${entry.character}",
                entry.strokeCode.isNotEmpty() && entry.strokeCode.all { it in '1'..'5' }
            )
        }
    }

    @Test
    fun hsk20CatalogEntriesAreWellFormed() {
        assertEquals(150, HanziCatalog.wordCountForLevel(HskLevel.HSK1, HskStandard.HSK_2_0))
        assertEquals(150, HanziCatalog.wordCountForLevel(HskLevel.HSK2, HskStandard.HSK_2_0))
        assertEquals(300, HanziCatalog.wordCountForLevel(HskLevel.HSK3, HskStandard.HSK_2_0))
        assertEquals(600, HanziCatalog.wordCountForLevel(HskLevel.HSK4, HskStandard.HSK_2_0))
        assertEquals(56, HanziCatalog.wordCountForLevel(HskLevel.UNI, HskStandard.HSK_2_0))
        assertEquals(1256, HanziCatalog.allWordsFor(HskStandard.HSK_2_0).size)

        assertEquals(174, HanziCatalog.countForLevel(HskLevel.HSK1, HskStandard.HSK_2_0))
        assertEquals(174, HanziCatalog.countForLevel(HskLevel.HSK2, HskStandard.HSK_2_0))
        assertEquals(270, HanziCatalog.countForLevel(HskLevel.HSK3, HskStandard.HSK_2_0))
        assertEquals(446, HanziCatalog.countForLevel(HskLevel.HSK4, HskStandard.HSK_2_0))
        assertEquals(4, HanziCatalog.countForLevel(HskLevel.UNI, HskStandard.HSK_2_0))
        assertEquals(1068, HanziCatalog.allEntriesFor(HskStandard.HSK_2_0).size)

        HanziCatalog.allEntriesFor(HskStandard.HSK_2_0).forEach { entry ->
            assertTrue("Empty HSK 2.0 character", entry.character.isNotBlank())
            assertTrue("Empty HSK 2.0 pinyin for ${entry.character}", entry.pinyin.isNotBlank())
            assertTrue("Empty HSK 2.0 meaningRu for ${entry.character}", entry.meaningRu.isNotBlank())
            assertTrue(
                "Invalid HSK 2.0 strokeCode '${entry.strokeCode}' for ${entry.character}",
                entry.strokeCode.isNotEmpty() && entry.strokeCode.all { it in '1'..'5' }
            )
        }

        // Verify examples preserve semicolon meanings and UNI entries exist in both standards
        val aiHsk30 = HanziCatalog.findByCharacter("爱", HskStandard.HSK_3_0)
        assertTrue(aiHsk30 != null && aiHsk30.examples.isNotEmpty())
        val yiHsk20 = HanziCatalog.findByCharacter("一", HskStandard.HSK_2_0)
        assertTrue(yiHsk20 != null && yiHsk20.examples.isNotEmpty())

        listOf(HskStandard.HSK_3_0, HskStandard.HSK_2_0).forEach { std ->
            val uniWords = HanziCatalog.wordsForLevels(setOf(HskLevel.UNI), std)
            assertTrue("Missing 俄语 in $std UNI", uniWords.any { it.hanzi == "俄语" })
            assertTrue("Missing 德语 in $std UNI", uniWords.any { it.hanzi == "德语" })
            assertFalse("Unexpected radical in $std UNI", uniWords.any { it.pos == "rad." })
            val uniChars = HanziCatalog.entriesForLevels(setOf(HskLevel.UNI), std).map { it.character }.toSet()
            assertTrue("Missing 王 in $std UNI", "王" in uniChars)
            assertTrue("Missing 俄 in $std UNI", "俄" in uniChars)
            assertFalse("Unexpected key 氵 in $std UNI", "氵" in uniChars)
        }

        // Verify continuous pinyin queries match spaced HSK 2.0 pinyin (e.g. 'cidi' -> 'cí diǎn')
        val cidiWords20 = HanziCatalog.searchWords("cidi", setOf(HskLevel.HSK3), HskStandard.HSK_2_0)
        assertTrue("Expected 词典 for 'cidi' in HSK 2.0", cidiWords20.any { it.hanzi == "词典" })
        val cidiChars20 = HanziCatalog.search("cidian", setOf(HskLevel.HSK3), HskStandard.HSK_2_0)
        assertTrue("Expected 词/典 example match for 'cidian' in HSK 2.0", cidiChars20.any { it.character == "词" })

        // Verify resolveWordEntries returns all characters of multi-character words for full-word animation
        val cidianWord = cidiWords20.first { it.hanzi == "词典" }
        val cidianEntries = HanziCatalog.resolveWordEntries(cidianWord, HskStandard.HSK_2_0)
        assertEquals(listOf("词", "典"), cidianEntries.map { it.character })

        val bukeqiWord = HanziCatalog.findWordByHanzi("不客气", HskStandard.HSK_2_0)!!
        val bukeqiEntries = HanziCatalog.resolveWordEntries(bukeqiWord, HskStandard.HSK_2_0)
        assertEquals(listOf("不", "客", "气"), bukeqiEntries.map { it.character })
    }

    @Test
    fun geometricClassifierDetectsBasicStrokes() {
        val horizontal = StrokeRecognizer.classifyDrawnStroke(
            listOf(StrokePoint(80f, 300f), StrokePoint(300f, 302f), StrokePoint(520f, 298f)),
            canvasSize = 600f
        )
        assertEquals(StrokeType.HORIZONTAL, horizontal.classifiedType)

        val vertical = StrokeRecognizer.classifyDrawnStroke(
            listOf(StrokePoint(300f, 80f), StrokePoint(302f, 300f), StrokePoint(298f, 520f)),
            canvasSize = 600f
        )
        assertEquals(StrokeType.VERTICAL, vertical.classifiedType)

        val leftFalling = StrokeRecognizer.classifyDrawnStroke(
            listOf(StrokePoint(380f, 100f), StrokePoint(280f, 280f), StrokePoint(140f, 460f)),
            canvasSize = 600f
        )
        assertEquals(StrokeType.LEFT_FALLING, leftFalling.classifiedType)

        val turning = StrokeRecognizer.classifyDrawnStroke(
            listOf(
                StrokePoint(150f, 150f),
                StrokePoint(420f, 150f),
                StrokePoint(420f, 440f)
            ),
            canvasSize = 600f
        )
        assertEquals(StrokeType.TURNING_OR_HOOK, turning.classifiedType)
    }

    @Test
    fun singleSquareStrokeMatchesKouByVisualShape() {
        // User draws '口' as a single continuous square stroke (1 stroke instead of 3)
        val singleSquare = StrokeRecognizer.classifyDrawnStroke(
            listOf(
                StrokePoint(120f, 120f),
                StrokePoint(480f, 120f),
                StrokePoint(480f, 480f),
                StrokePoint(120f, 480f),
                StrokePoint(120f, 120f)
            ),
            canvasSize = 600f
        )

        // Synthetic reference templates simulating rendered glyphs for 口, 日, 一, 人
        val templates = mapOf(
            "口" to buildTemplate(
                listOf(
                    listOf(StrokePoint(120f, 120f), StrokePoint(120f, 480f)),
                    listOf(StrokePoint(120f, 120f), StrokePoint(480f, 120f), StrokePoint(480f, 480f)),
                    listOf(StrokePoint(120f, 480f), StrokePoint(480f, 480f))
                )
            ),
            "日" to buildTemplate(
                listOf(
                    listOf(StrokePoint(120f, 120f), StrokePoint(120f, 480f)),
                    listOf(StrokePoint(120f, 120f), StrokePoint(480f, 120f), StrokePoint(480f, 480f)),
                    listOf(StrokePoint(120f, 300f), StrokePoint(480f, 300f)),
                    listOf(StrokePoint(120f, 480f), StrokePoint(480f, 480f))
                )
            ),
            "一" to buildTemplate(
                listOf(listOf(StrokePoint(80f, 300f), StrokePoint(520f, 300f)))
            ),
            "人" to buildTemplate(
                listOf(
                    listOf(StrokePoint(300f, 100f), StrokePoint(120f, 480f)),
                    listOf(StrokePoint(280f, 220f), StrokePoint(480f, 480f))
                )
            )
        )
        val fallbackTemplate = templates.getValue("一")

        val candidates = StrokeRecognizer.matchCandidates(
            drawnStrokes = listOf(singleSquare),
            enabledLevels = setOf(HskLevel.HSK1),
            strictStrokeCount = false,
            templateFeaturesProvider = { ch -> templates[ch] ?: fallbackTemplate },
            limit = 5
        )

        assertTrue(candidates.isNotEmpty())
        assertEquals("口", candidates.first().entry.character)
    }

    @Test
    fun matchingRespectsEnabledHskLevels() {
        // '门' (strokeCode "425") is in HSK 2 in official HSK 3.0
        val strokes = listOf(
            StrokeRecognizer.fromManualStroke(StrokeType.DOT_OR_RIGHT_FALLING),
            StrokeRecognizer.fromManualStroke(StrokeType.VERTICAL),
            StrokeRecognizer.fromManualStroke(StrokeType.TURNING_OR_HOOK)
        )

        val hsk1Only = StrokeRecognizer.matchCandidates(
            drawnStrokes = strokes,
            enabledLevels = setOf(HskLevel.HSK1),
            strictStrokeCount = true,
            limit = 30
        )
        assertTrue(hsk1Only.all { it.entry.hskLevel == HskLevel.HSK1 })
        assertFalse(hsk1Only.any { it.entry.character == "门" })

        val withHsk2 = StrokeRecognizer.matchCandidates(
            drawnStrokes = strokes,
            enabledLevels = setOf(HskLevel.HSK1, HskLevel.HSK2),
            strictStrokeCount = true,
            limit = 30
        )
        assertTrue(withHsk2.any { it.entry.character == "门" })
    }

    @Test
    fun matchingRespectsHskStandard() {
        // '占' (strokeCode "21251", 5 strokes) exists only in HSK 2.0 (HSK 4), not in HSK 3.0 (1-4)
        val strokes = listOf(
            StrokeRecognizer.fromManualStroke(StrokeType.VERTICAL),
            StrokeRecognizer.fromManualStroke(StrokeType.HORIZONTAL),
            StrokeRecognizer.fromManualStroke(StrokeType.VERTICAL),
            StrokeRecognizer.fromManualStroke(StrokeType.TURNING_OR_HOOK),
            StrokeRecognizer.fromManualStroke(StrokeType.HORIZONTAL)
        )

        val hsk30Matches = StrokeRecognizer.matchCandidates(
            drawnStrokes = strokes,
            enabledLevels = HskLevel.entries.toSet(),
            strictStrokeCount = true,
            hskStandard = HskStandard.HSK_3_0,
            limit = 50
        )
        assertFalse(hsk30Matches.any { it.entry.character == "占" })

        val hsk20Matches = StrokeRecognizer.matchCandidates(
            drawnStrokes = strokes,
            enabledLevels = HskLevel.entries.toSet(),
            strictStrokeCount = true,
            hskStandard = HskStandard.HSK_2_0,
            limit = 50
        )
        assertTrue(hsk20Matches.any { it.entry.character == "占" })
    }

    @Test
    fun handwrittenStrokePathsMatchCatalogStrokeCounts() {
        (HanziCatalog.allEntries + HanziCatalog.allEntriesFor(HskStandard.HSK_2_0)).forEach { entry ->
            assertTrue("Missing MakeMeAHanzi SVG data for '${entry.character}'", MakeMeAHanziRepository.hasGlyph(entry.character))
            val strokes = HanziStrokePaths.getNormalizedStrokes(entry.character)
            assertTrue("Missing handwritten strokes for '${entry.character}'", strokes != null)
            assertEquals(
                "Stroke count mismatch for '${entry.character}'",
                entry.strokeCount,
                strokes!!.size
            )
        }
    }

    private fun buildTemplate(strokeLines: List<List<StrokePoint>>): VisualShapeMatcher.ShapeFeatures {
        val strokes = strokeLines.map { DrawnStroke(it, StrokeType.HORIZONTAL, emptyMap()) }
        val grid = VisualShapeMatcher.rasterizeStrokesTo32(strokes)!!
        return VisualShapeMatcher.extractFeatures(grid)
    }
}
