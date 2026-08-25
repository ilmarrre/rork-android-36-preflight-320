package com.rork.ghostdetectorspiritbox.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * The signal model may only ever emit TRACE, CONTACT and LOST, and its blips must stay
 * inside the drawable graticule — they are coordinates, never distances.
 */
class RadarModelTest {

    private val allowed = setOf(
        EventKind.RADAR_TRACE,
        EventKind.RADAR_CONTACT,
        EventKind.RADAR_LOST
    )

    @Test
    fun `only radar events are produced`() {
        val model = RadarModel(Random(9))
        var elapsed = 0L
        repeat(600) {
            elapsed += 300L
            model.tick(elapsed, 0.8).forEach { event ->
                assertTrue("unexpected ${event.kind}", event.kind in allowed)
            }
        }
    }

    @Test
    fun `contacts are always preceded by a trace`() {
        val model = RadarModel(Random(21))
        var elapsed = 0L
        var traces = 0
        var contacts = 0
        repeat(800) {
            elapsed += 300L
            model.tick(elapsed, 1.0).forEach { event ->
                when (event.kind) {
                    EventKind.RADAR_TRACE -> traces++
                    EventKind.RADAR_CONTACT -> contacts++
                    else -> Unit
                }
            }
            assertTrue(contacts <= traces)
        }
        assertTrue("model produced no activity at all", traces > 0)
    }

    @Test
    fun `blips stay inside the graticule`() {
        val model = RadarModel(Random(4))
        var elapsed = 0L
        repeat(400) {
            elapsed += 300L
            model.tick(elapsed, 0.9)
            model.blips.forEach { blip ->
                assertTrue(blip.radiusFraction in 0f..1f)
                assertTrue(blip.angleDeg in 0f..360f)
                assertTrue(blip.strength in 0f..1f)
            }
        }
    }

    @Test
    fun `clearing drops every track`() {
        val model = RadarModel(Random(4))
        var elapsed = 0L
        repeat(400) {
            elapsed += 300L
            model.tick(elapsed, 1.0)
        }
        model.clear()
        assertEquals(emptyList<RadarBlip>(), model.blips)
    }
}
