package com.rork.ghostdetectorspiritbox.domain

import kotlin.math.abs
import kotlin.random.Random

/** A word the local engine can surface, tagged with the theme used for record patterns. */
data class EngineWord(val word: String, val category: String)

/**
 * Local weighted word engine. There is no microphone and no radio: candidate words are
 * drawn from bundled pools, weighted by category, and the draw is nudged by how far the
 * magnetometer currently deviates from the session baseline.
 */
class WordEngine(private val random: Random = Random.Default) {

    private val pools: Map<String, List<String>> = mapOf(
        "NAMES" to listOf(
            "anna", "marta", "elias", "thomas", "sarah", "jacob",
            "ruth", "henry", "clara", "otto", "edith", "walter"
        ),
        "PLACES" to listOf(
            "outside", "door", "cellar", "upstairs", "hall", "corner",
            "window", "garden", "kitchen", "attic", "below", "behind"
        ),
        "COLD" to listOf("cold", "freeze", "ice", "chill", "winter", "damp"),
        "RESPONSES" to listOf(
            "yes", "no", "maybe", "wait", "listen", "again",
            "help", "please", "hello", "here", "gone", "true"
        ),
        "MOTION" to listOf("leave", "follow", "come", "run", "stay", "near", "closer", "back"),
        "TIME" to listOf("night", "seven", "midnight", "hour", "late", "soon", "years", "long")
    )

    private val baseWeights: Map<String, Double> = mapOf(
        "NAMES" to 1.0,
        "PLACES" to 1.4,
        "COLD" to 0.8,
        "RESPONSES" to 1.2,
        "MOTION" to 1.0,
        "TIME" to 0.9
    )

    private val recent = ArrayDeque<String>()

    /**
     * Draw the next response.
     *
     * @param answeringQuestion favours short answer words right after the user asks something.
     * @param fieldDeviation normalised distance of the live field from the session baseline;
     *        stronger deviation favours names and cold themes.
     */
    fun next(answeringQuestion: Boolean, fieldDeviation: Double): EngineWord {
        val deviation = fieldDeviation.coerceIn(0.0, 1.0)
        val weights = baseWeights.mapValues { (category, weight) ->
            var w = weight
            if (answeringQuestion && category == "RESPONSES") w *= 4.0
            if (category == "NAMES") w *= 1.0 + deviation * 1.6
            if (category == "COLD") w *= 1.0 + deviation * 1.2
            if (category == "TIME") w *= 1.0 - deviation * 0.3
            w
        }
        val category = weightedPick(weights)
        val pool = pools.getValue(category)
        var word = pool[random.nextInt(pool.size)]
        var attempts = 0
        while (recent.contains(word) && attempts < 4) {
            word = pool[random.nextInt(pool.size)]
            attempts++
        }
        recent.addLast(word)
        if (recent.size > 6) recent.removeFirst()
        return EngineWord(word, category)
    }

    private fun weightedPick(weights: Map<String, Double>): String {
        val total = weights.values.sum()
        var roll = random.nextDouble() * total
        for ((key, weight) in weights) {
            roll -= weight
            if (roll <= 0.0) return key
        }
        return weights.keys.first()
    }

    /** Gap before the next response, shorter when the user has just asked something. */
    fun nextDelayMillis(answeringQuestion: Boolean, fieldDeviation: Double): Long {
        if (answeringQuestion) return random.nextLong(1_800L, 4_200L)
        val deviation = abs(fieldDeviation).coerceIn(0.0, 1.0)
        val base = 13_000L - (deviation * 6_000L).toLong()
        return random.nextLong(base - 4_000L, base + 4_000L).coerceAtLeast(3_000L)
    }
}

