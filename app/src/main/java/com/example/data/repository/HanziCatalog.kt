package com.example.data.repository

import com.example.data.model.ExampleWord
import com.example.data.model.HanziEntry
import com.example.data.model.HskLevel
import com.example.data.model.HskStandard
import com.example.data.model.HskWordEntry
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

object HanziCatalog {

    val allEntries: List<HanziEntry> by lazy {
        loadCharactersFromResource("hsk/hsk_chars.tsv")
    }

    val allWords: List<HskWordEntry> by lazy {
        loadWordsFromResource("hsk/hsk_words.tsv")
    }

    val allEntriesHsk20: List<HanziEntry> by lazy {
        loadCharactersFromResource("hsk/hsk20_chars.tsv")
    }

    val allWordsHsk20: List<HskWordEntry> by lazy {
        loadWordsFromResource("hsk/hsk20_words.tsv")
    }

    private val byCharacterHsk30: Map<String, HanziEntry> by lazy {
        allEntries.associateBy { it.character }
    }

    private val byCharacterHsk20: Map<String, HanziEntry> by lazy {
        allEntriesHsk20.associateBy { it.character }
    }

    private val charCountByLevelHsk30: Map<HskLevel, Int> by lazy {
        HskLevel.entries.associateWith { level ->
            allEntries.count { it.hskLevel == level }
        }
    }

    private val charCountByLevelHsk20: Map<HskLevel, Int> by lazy {
        HskLevel.entries.associateWith { level ->
            allEntriesHsk20.count { it.hskLevel == level }
        }
    }

    private val wordCountByLevelHsk30: Map<HskLevel, Int> by lazy {
        HskLevel.entries.associateWith { level ->
            allWords.count { it.hskLevel == level }
        }
    }

    private val wordCountByLevelHsk20: Map<HskLevel, Int> by lazy {
        HskLevel.entries.associateWith { level ->
            allWordsHsk20.count { it.hskLevel == level }
        }
    }

    fun allEntriesFor(standard: HskStandard = HskStandard.HSK_3_0): List<HanziEntry> =
        if (standard == HskStandard.HSK_2_0) allEntriesHsk20 else allEntries

    fun allWordsFor(standard: HskStandard = HskStandard.HSK_3_0): List<HskWordEntry> =
        if (standard == HskStandard.HSK_2_0) allWordsHsk20 else allWords

    fun countForLevel(level: HskLevel, standard: HskStandard = HskStandard.HSK_3_0): Int =
        if (standard == HskStandard.HSK_2_0) {
            charCountByLevelHsk20[level] ?: 0
        } else {
            charCountByLevelHsk30[level] ?: 0
        }

    fun wordCountForLevel(level: HskLevel, standard: HskStandard = HskStandard.HSK_3_0): Int =
        if (standard == HskStandard.HSK_2_0) {
            wordCountByLevelHsk20[level] ?: 0
        } else {
            wordCountByLevelHsk30[level] ?: 0
        }

    fun entriesForLevels(
        enabledLevels: Set<HskLevel>,
        standard: HskStandard = HskStandard.HSK_3_0
    ): List<HanziEntry> =
        allEntriesFor(standard).filter { it.hskLevel in enabledLevels }

    fun wordsForLevels(
        enabledLevels: Set<HskLevel>,
        standard: HskStandard = HskStandard.HSK_3_0
    ): List<HskWordEntry> =
        allWordsFor(standard).filter { it.hskLevel in enabledLevels }

    fun findByCharacter(
        char: String,
        standard: HskStandard = HskStandard.HSK_3_0
    ): HanziEntry? = if (standard == HskStandard.HSK_2_0) {
        byCharacterHsk20[char] ?: byCharacterHsk30[char]
    } else {
        byCharacterHsk30[char] ?: byCharacterHsk20[char]
    }

    fun findWordByHanzi(
        hanzi: String,
        standard: HskStandard = HskStandard.HSK_3_0
    ): HskWordEntry? {
        val trimmed = hanzi.trim()
        val primary = allWordsFor(standard).firstOrNull { it.hanzi == trimmed }
        if (primary != null) return primary
        val fallbackStd = if (standard == HskStandard.HSK_2_0) HskStandard.HSK_3_0 else HskStandard.HSK_2_0
        return allWordsFor(fallbackStd).firstOrNull { it.hanzi == trimmed }
    }

    fun resolveWordEntries(
        word: HskWordEntry,
        standard: HskStandard = HskStandard.HSK_3_0
    ): List<HanziEntry> = resolveWordEntries(
        hanzi = word.hanzi,
        pinyin = word.pinyin,
        meaningRu = word.meaningRu,
        hskLevel = word.hskLevel,
        standard = standard
    )

    fun resolveWordEntries(
        hanzi: String,
        pinyin: String,
        meaningRu: String,
        hskLevel: HskLevel = HskLevel.HSK1,
        standard: HskStandard = HskStandard.HSK_3_0
    ): List<HanziEntry> {
        val chars = hanzi.map { it.toString() }
            .filter { ch -> ch.isNotBlank() && ch[0].code >= 0x3400 }
        val syllables = pinyin.trim()
            .split(Regex("[\\s'’·]+"))
            .filter { it.isNotBlank() }

        return chars.mapIndexed { idx, ch ->
            findByCharacter(ch, standard) ?: HanziEntry(
                character = ch,
                pinyin = syllables.getOrNull(idx) ?: pinyin,
                meaningRu = meaningRu,
                hskLevel = hskLevel,
                strokeCode = "1234",
                radical = ch,
                examples = emptyList()
            )
        }
    }

    fun search(
        query: String,
        enabledLevels: Set<HskLevel>,
        standard: HskStandard = HskStandard.HSK_3_0
    ): List<HanziEntry> {
        val trimmed = query.trim()
        val pool = entriesForLevels(enabledLevels, standard)
        if (trimmed.isEmpty()) return pool

        val normalizedQuery = normalizePinyin(trimmed)
        val compactQuery = compactPinyin(normalizedQuery)
        return pool.filter { entry ->
            entry.character.contains(trimmed) ||
                entry.meaningRu.contains(trimmed, ignoreCase = true) ||
                pinyinMatches(entry.pinyin, normalizedQuery, compactQuery) ||
                entry.examples.any {
                    it.hanzi.contains(trimmed) ||
                        it.meaningRu.contains(trimmed, ignoreCase = true) ||
                        pinyinMatches(it.pinyin, normalizedQuery, compactQuery)
                }
        }
    }

    fun searchWords(
        query: String,
        enabledLevels: Set<HskLevel>,
        standard: HskStandard = HskStandard.HSK_3_0
    ): List<HskWordEntry> {
        val trimmed = query.trim()
        val pool = wordsForLevels(enabledLevels, standard)
        if (trimmed.isEmpty()) return pool

        val normalizedQuery = normalizePinyin(trimmed)
        val compactQuery = compactPinyin(normalizedQuery)
        return pool.filter { word ->
            word.hanzi.contains(trimmed) ||
                word.meaningRu.contains(trimmed, ignoreCase = true) ||
                word.meaningEn.contains(trimmed, ignoreCase = true) ||
                pinyinMatches(word.pinyin, normalizedQuery, compactQuery) ||
                word.number.toString() == trimmed
        }
    }

    private fun loadCharactersFromResource(resourcePath: String): List<HanziEntry> {
        val list = ArrayList<HanziEntry>(1150)
        openResourceReader(resourcePath).useLines { lines ->
            lines.forEach { rawLine ->
                val line = rawLine.trim()
                if (line.isEmpty()) return@forEach
                val parts = line.split('\t')
                if (parts.size < 6) return@forEach
                val character = parts[0].trim()
                val levelNum = parts[1].trim().toIntOrNull() ?: 1
                val pinyin = parts[2].trim()
                val meaningRu = parts[3].trim()
                val strokeCode = parts[4].trim()
                val radical = parts[5].trim()
                val examplesRaw = if (parts.size >= 7) parts[6].trim() else ""
                val examples = parseExamples(examplesRaw)
                list.add(
                    HanziEntry(
                        character = character,
                        pinyin = pinyin,
                        meaningRu = meaningRu,
                        hskLevel = HskLevel.fromNumber(levelNum),
                        strokeCode = strokeCode,
                        radical = radical,
                        examples = examples
                    )
                )
            }
        }
        return list
    }

    private fun parseExamples(examplesRaw: String): List<ExampleWord> {
        if (examplesRaw.isEmpty()) return emptyList()
        val result = mutableListOf<ExampleWord>()
        for (segment in examplesRaw.split(';')) {
            val trimmed = segment.trim()
            if (trimmed.isEmpty()) continue
            val delimiter = when {
                trimmed.count { it == '|' } >= 2 -> '|'
                trimmed.count { it == '~' } >= 2 -> '~'
                else -> null
            }
            if (delimiter != null) {
                val exParts = trimmed.split(delimiter, limit = 3)
                if (exParts.size >= 3) {
                    result.add(
                        ExampleWord(
                            hanzi = exParts[0].trim(),
                            pinyin = exParts[1].trim(),
                            meaningRu = exParts[2].trim()
                        )
                    )
                }
            } else if (result.isNotEmpty()) {
                val prev = result.last()
                result[result.lastIndex] = prev.copy(
                    meaningRu = "${prev.meaningRu}; $trimmed"
                )
            }
        }
        return result
    }

    private fun loadWordsFromResource(resourcePath: String): List<HskWordEntry> {
        val list = ArrayList<HskWordEntry>(2050)
        openResourceReader(resourcePath).useLines { lines ->
            lines.forEach { rawLine ->
                val line = rawLine.trim()
                if (line.isEmpty()) return@forEach
                val parts = line.split('\t')
                if (parts.size < 6) return@forEach
                val number = parts[0].trim().toIntOrNull() ?: return@forEach
                val levelNum = parts[1].trim().toIntOrNull() ?: 1
                val hanzi = parts[2].trim()
                val pinyin = parts[3].trim()
                val pos = parts[4].trim()
                val meaningRu = parts[5].trim()
                val meaningEn = if (parts.size >= 7) parts[6].trim() else ""
                list.add(
                    HskWordEntry(
                        number = number,
                        hskLevel = HskLevel.fromNumber(levelNum),
                        hanzi = hanzi,
                        pinyin = pinyin,
                        pos = pos,
                        meaningRu = meaningRu,
                        meaningEn = meaningEn
                    )
                )
            }
        }
        return list
    }

    private fun openResourceReader(resourcePath: String): BufferedReader {
        val stream = HanziCatalog::class.java.classLoader?.getResourceAsStream(resourcePath)
            ?: HanziCatalog::class.java.getResourceAsStream("/$resourcePath")
        if (stream != null) {
            return BufferedReader(InputStreamReader(stream, StandardCharsets.UTF_8))
        }
        val candidates = listOf(
            File("src/main/resources/$resourcePath"),
            File("app/src/main/resources/$resourcePath")
        )
        val file = candidates.firstOrNull { it.exists() }
            ?: error("Could not locate HSK resource: $resourcePath")
        return file.bufferedReader(StandardCharsets.UTF_8)
    }

    private fun pinyinMatches(
        pinyin: String,
        normalizedQuery: String,
        compactQuery: String
    ): Boolean {
        val normalizedPinyin = normalizePinyin(pinyin)
        if (normalizedPinyin.contains(normalizedQuery)) return true
        if (compactQuery.isEmpty()) return false
        val compact = compactPinyin(normalizedPinyin)
        if (compact.contains(compactQuery)) return true
        return compact.replace('v', 'u').contains(compactQuery.replace('v', 'u'))
    }

    private fun compactPinyin(normalized: String): String = buildString(normalized.length) {
        for (ch in normalized) {
            if (ch != ' ' && ch != '\'' && ch != '’' && ch != '-') {
                append(ch)
            }
        }
    }

    private fun normalizePinyin(input: String): String {
        val map = mapOf(
            'ā' to 'a', 'á' to 'a', 'ǎ' to 'a', 'à' to 'a',
            'ē' to 'e', 'é' to 'e', 'ě' to 'e', 'è' to 'e',
            'ī' to 'i', 'í' to 'i', 'ǐ' to 'i', 'ì' to 'i',
            'ō' to 'o', 'ó' to 'o', 'ǒ' to 'o', 'ò' to 'o',
            'ū' to 'u', 'ú' to 'u', 'ǔ' to 'u', 'ù' to 'u',
            'ǖ' to 'v', 'ǘ' to 'v', 'ǚ' to 'v', 'ǜ' to 'v', 'ü' to 'v'
        )
        return buildString(input.length) {
            for (ch in input.lowercase()) {
                append(map[ch] ?: ch)
            }
        }
    }
}
