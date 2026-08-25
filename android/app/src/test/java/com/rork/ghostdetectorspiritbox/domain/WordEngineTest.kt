package com.rork.ghostdetectorspiritbox.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** The word engine is local, deterministic under a seeded source, and never empty. */
class WordEngineTest {

    @Test
    fun `every draw returns a non-blank word and a known category`() {
        val engine = WordEngine(Random(7))
        val categories = setOf("NAMES", "PLACES", "COLD", "RESPONSES", "MOTION", "TIME")
        repeat(200) {
            val drawn = engine.next(answeringQuestion = false, fieldDeviation = 0.5)
            assertTrue(drawn.word.isNotBlank())
            assertTrue(drawn.category in categories)
        }
    }

    @Test
    fun `the same seed produces the same sequence`() {
        val first = WordEngine(Random(42))
        val second = WordEngine(Random(42))
        repeat(25) {
            assertEquals(
                first.next(answeringQuestion = false, fieldDeviation = 0.2),
                second.next(answeringQuestion = false, fieldDeviation = 0.2)
            )
        }
    }

    @Test
    fun `answering a question favours short response words`() {
        val prompted = WordEngine(Random(11))
        val counts = (1..400)
            .map { prompted.next(answeringQuestion = true, fieldDeviation = 0.0).category }
            .groupingBy { it }
            .eachCount()
        val responses = counts["RESPONSES"] ?: 0
        val runnerUp = counts.filterKeys { it != "RESPONSES" }.values.maxOrNull() ?: 0
        assertTrue(
            "RESPONSES ($responses) should lead the next category ($runnerUp)",
            responses > runnerUp * 2
        )

        val idle = WordEngine(Random(11))
        val idleResponses = (1..400).count {
            idle.next(answeringQuestion = false, fieldDeviation = 0.0).category == "RESPONSES"
        }
        assertTrue(
            "a question should raise RESPONSES above the idle rate ($idleResponses)",
            responses > idleResponses
        )
    }

    @Test
    fun `a question shortens the wait for the next response`() {
        val engine = WordEngine(Random(3))
        repeat(50) {
            val prompt = engine.nextDelayMillis(answeringQuestion = true, fieldDeviation = 0.0)
            assertTrue(prompt in 1_800L..4_200L)
        }
    }

    @Test
    fun `idle delays never drop below the floor`() {
        val engine = WordEngine(Random(5))
        repeat(200) {
            val delay = engine.nextDelayMillis(answeringQuestion = false, fieldDeviation = 1.0)
            assertTrue("delay was $delay", delay >= 3_000L)
        }
    }
}
