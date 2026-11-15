package com.example.domain

import com.example.data.model.HskWordEntry

sealed interface RecallChallenge {
    val target: HskWordEntry

    /**
     * Given the character + meaning, recall the exact pinyin with tones
     * among 4 tone-minimal-pair candidates.
     */
    data class ToneChallenge(
        override val target: HskWordEntry,
        val options: List<String>,
        val correctOption: String
    ) : RecallChallenge

    /**
     * Given meaning + pinyin, actively assemble the Chinese word from character tiles.
     */
    data class AssembleChallenge(
        override val target: HskWordEntry,
        val tileBank: List<String>
    ) : RecallChallenge
}

data class MatchPairItem(
    val word: HskWordEntry
)

object StudyTechniquesEngine {

    private val toneGroups: List<List<Char>> = listOf(
        listOf('a', 'ā', 'á', 'ǎ', 'à'),
        listOf('o', 'ō', 'ó', 'ǒ', 'ò'),
        listOf('e', 'ē', 'é', 'ě', 'è'),
        listOf('i', 'ī', 'í', 'ǐ', 'ì'),
        listOf('u', 'ū', 'ú', 'ǔ', 'ù'),
        listOf('ü', 'ǖ', 'ǘ', 'ǚ', 'ǜ')
    )

    private val charToToneGroup: Map<Char, Pair<List<Char>, Int>> = buildMap {
        toneGroups.forEach { group ->
            group.forEachIndexed { toneIdx, ch ->
                put(ch, group to toneIdx)
            }
        }
    }

    /**
     * Generates a mixed Active Recall round (Tone discrimination + Hanzi assembly).
     */
    fun buildRecallRound(
        pool: List<HskWordEntry>,
        srsRecords: Map<Int, SrsCardState> = emptyMap(),
        roundSize: Int = 8,
        nowMillis: Long = System.currentTimeMillis()
    ): List<RecallChallenge> {
        if (pool.size < 4) return emptyList()

        // Prioritize due & unmastered words, then mix remaining
        val dueOrWeak = pool.filter {
            val st = srsRecords[it.number]
            st == null || SrsEngine.isDue(st, nowMillis) || SrsEngine.isWeakOrHard(st)
        }
        val candidates = ((dueOrWeak.shuffled() + pool.shuffled()).distinctBy { it.number })
            .filter { !it.pinyin.contains("/") && it.hanzi.isNotBlank() }
            .ifEmpty { pool.shuffled() }

        val result = ArrayList<RecallChallenge>(roundSize)
        val allPoolChars = pool
            .flatMap { word -> word.hanzi.map { it.toString() } }
            .filter { it.isNotBlank() && it[0].code >= 0x4E00 }
            .distinct()

        candidates.forEach { word ->
            if (result.size >= roundSize) return@forEach

            val preferAssemble = (result.size % 2 == 1) && word.hanzi.length in 1..3 && allPoolChars.size >= 8
            val toneChallenge = if (!preferAssemble) buildToneChallenge(word) else null
            if (toneChallenge != null) {
                result.add(toneChallenge)
            } else {
                val assembleChallenge = buildAssembleChallenge(word, allPoolChars)
                if (assembleChallenge != null) {
                    result.add(assembleChallenge)
                } else {
                    buildToneChallenge(word)?.let { result.add(it) }
                }
            }
        }
        return result
    }

    fun buildToneChallenge(word: HskWordEntry): RecallChallenge.ToneChallenge? {
        val basePinyin = word.pinyin.trim().lowercase()
        val variants = generateToneVariants(basePinyin)
        if (variants.size < 3) return null

        val distractors = variants.shuffled().take(3)
        val options = (listOf(basePinyin) + distractors).shuffled()
        return RecallChallenge.ToneChallenge(
            target = word,
            options = options,
            correctOption = basePinyin
        )
    }

    fun buildAssembleChallenge(
        word: HskWordEntry,
        allPoolChars: List<String>
    ): RecallChallenge.AssembleChallenge? {
        val targetChars = word.hanzi.map { it.toString() }
        if (targetChars.isEmpty() || targetChars.size > 4) return null

        val neededDistractors = (8 - targetChars.size).coerceAtLeast(3)
        val distractors = allPoolChars
            .filter { it !in targetChars }
            .shuffled()
            .take(neededDistractors)
        if (distractors.size < 3) return null

        val bank = (targetChars + distractors).shuffled()
        return RecallChallenge.AssembleChallenge(
            target = word,
            tileBank = bank
        )
    }

    fun generateToneVariants(pinyin: String): List<String> {
        val tonedPositions = pinyin.indices.mapNotNull { idx ->
            val ch = pinyin[idx]
            val entry = charToToneGroup[ch]
            if (entry != null && entry.second > 0) idx to entry else null
        }

        val variants = LinkedHashSet<String>()
        // Mutate each toned vowel to other tones
        for ((idx, pair) in tonedPositions) {
            val (group, currentTone) = pair
            for (altTone in 1..4) {
                if (altTone == currentTone) continue
                val mutated = pinyin.substring(0, idx) + group[altTone] + pinyin.substring(idx + 1)
                if (mutated != pinyin) {
                    variants.add(mutated)
                }
            }
            // Also neutral tone variant if single syllable or second syllable
            val neutral = pinyin.substring(0, idx) + group[0] + pinyin.substring(idx + 1)
            if (neutral != pinyin) {
                variants.add(neutral)
            }
        }
        return variants.toList()
    }

    /**
     * Picks distinct-meaning words for a 5-pair visual association matching grid.
     */
    fun buildMatchPairs(
        pool: List<HskWordEntry>,
        srsRecords: Map<Int, SrsCardState> = emptyMap(),
        pairCount: Int = 5,
        nowMillis: Long = System.currentTimeMillis()
    ): List<HskWordEntry> {
        val prioritized = pool.sortedBy { word ->
            val st = srsRecords[word.number]
            when {
                st == null -> 1
                SrsEngine.isDue(st, nowMillis) -> 0
                SrsEngine.isWeakOrHard(st) -> 1
                else -> 2
            }
        }
        val topSlice = prioritized.take(40).shuffled() + prioritized.drop(40).shuffled()
        return topSlice
            .distinctBy { it.hanzi }
            .distinctBy { it.meaningRu.lowercase() }
            .take(pairCount)
    }
}
