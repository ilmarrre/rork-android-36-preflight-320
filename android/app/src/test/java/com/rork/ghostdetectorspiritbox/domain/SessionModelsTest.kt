package com.rork.ghostdetectorspiritbox.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure record maths: no Android, no UI, no sensors. */
class SessionModelsTest {

    private fun record(events: List<SessionEvent>, durationMillis: Long = 120_000L) = SessionRecord(
        id = "test",
        number = 1,
        startedAtEpochMillis = 0L,
        durationMillis = durationMillis,
        baselineMicroTesla = 40.0,
        peakMicroTesla = 88.5,
        thresholdMicroTesla = 80.0,
        events = events
    )

    @Test
    fun `formatClock renders minutes and seconds`() {
        assertEquals("00:00", formatClock(0L))
        assertEquals("00:09", formatClock(9_400L))
        assertEquals("15:00", formatClock(900_000L))
        assertEquals("00:00", formatClock(-5_000L))
    }

    @Test
    fun `formatMicroTesla keeps exactly one decimal`() {
        assertEquals("40.0", formatMicroTesla(40.0))
        assertEquals("88.5", formatMicroTesla(88.46))
    }

    @Test
    fun `counts only include their own kind`() {
        val subject = record(
            listOf(
                SessionEvent(EventKind.WORD, 1_000L, "cold"),
                SessionEvent(EventKind.WORD, 2_000L, "door"),
                SessionEvent(EventKind.RADAR_CONTACT, 3_000L),
                SessionEvent(EventKind.MARKED, 4_000L)
            )
        )
        assertEquals(2, subject.responseCount)
        assertEquals(1, subject.contactCount)
        assertEquals(1, subject.markedCount)
    }

    @Test
    fun `marked responses count as marked moments`() {
        val subject = record(
            listOf(SessionEvent(EventKind.WORD, 1_000L, "cold", marked = true))
        )
        assertEquals(1, subject.markedCount)
    }

    @Test
    fun `timeline hides setup entries and stays chronological`() {
        val subject = record(
            listOf(
                SessionEvent(EventKind.SESSION_START, 0L),
                SessionEvent(EventKind.BASELINE, 3_500L, value = 40.0),
                SessionEvent(EventKind.WORD, 9_000L, "hello"),
                SessionEvent(EventKind.QUESTION, 6_000L, "Are you here?")
            )
        )
        val kinds = subject.timeline.map { it.kind }
        assertEquals(listOf(EventKind.QUESTION, EventKind.WORD), kinds)
    }

    @Test
    fun `strongest interval needs at least two notable events`() {
        assertNull(record(listOf(SessionEvent(EventKind.WORD, 1_000L, "cold"))).strongestInterval())
    }

    @Test
    fun `strongest interval names the densest window`() {
        val subject = record(
            listOf(
                SessionEvent(EventKind.WORD, 61_000L, "cold"),
                SessionEvent(EventKind.WORD, 64_000L, "door"),
                SessionEvent(EventKind.EMF_SPIKE, 66_000L, value = 91.0),
                SessionEvent(EventKind.WORD, 110_000L, "gone")
            )
        )
        assertEquals("01:00–01:30", subject.strongestInterval())
    }

    @Test
    fun `recurring themes only report categories seen twice`() {
        val subject = record(
            listOf(
                SessionEvent(EventKind.WORD, 1_000L, "cold", category = "COLD"),
                SessionEvent(EventKind.WORD, 2_000L, "ice", category = "COLD"),
                SessionEvent(EventKind.WORD, 3_000L, "anna", category = "NAMES")
            )
        )
        assertEquals("COLD (2)", subject.recurringThemes())
    }

    @Test
    fun `recurring themes are null without repetition`() {
        val subject = record(
            listOf(SessionEvent(EventKind.WORD, 1_000L, "anna", category = "NAMES"))
        )
        assertNull(subject.recurringThemes())
    }

    @Test
    fun `strongest interval never runs past the session`() {
        val subject = record(
            events = listOf(
                SessionEvent(EventKind.WORD, 10_000L, "cold"),
                SessionEvent(EventKind.WORD, 12_000L, "door")
            ),
            durationMillis = 20_000L
        )
        assertTrue(subject.strongestInterval()!!.endsWith("00:20"))
    }
}
