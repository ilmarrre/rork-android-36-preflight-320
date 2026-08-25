package com.rork.ghostdetectorspiritbox.domain

import kotlin.random.Random

/** A signal-model blip. Position is a drawing coordinate, never a physical distance. */
data class RadarBlip(
    val id: Long,
    val angleDeg: Float,
    val radiusFraction: Float,
    val state: BlipState,
    val strength: Float
)

enum class BlipState { TRACE, CONTACT }

/**
 * The radar is a visualization model of session activity, not a ranging device.
 * It never reports metres, bearings or ranges — only TRACE, CONTACT and LOST.
 */
class RadarModel(private val random: Random = Random.Default) {

    private data class Track(
        val id: Long,
        val angleDeg: Float,
        val radiusFraction: Float,
        var state: BlipState,
        val bornAt: Long,
        val contactAt: Long,
        val diesAt: Long,
        var contactLogged: Boolean
    )

    private val tracks = mutableListOf<Track>()
    private var nextId = 1L
    private var lastSpawnAt = 0L

    val blips: List<RadarBlip>
        get() = tracks.map {
            RadarBlip(
                id = it.id,
                angleDeg = it.angleDeg,
                radiusFraction = it.radiusFraction,
                state = it.state,
                strength = if (it.state == BlipState.CONTACT) 1f else 0.45f
            )
        }

    /**
     * Advance the model.
     *
     * @param elapsedMillis session elapsed time.
     * @param deviation normalised deviation of the live field from the session baseline.
     * @return events to append to the session log.
     */
    fun tick(elapsedMillis: Long, deviation: Double): List<SessionEvent> {
        val events = mutableListOf<SessionEvent>()
        val dev = deviation.coerceIn(0.0, 1.0)

        val iterator = tracks.iterator()
        while (iterator.hasNext()) {
            val track = iterator.next()
            if (track.state == BlipState.TRACE && elapsedMillis >= track.contactAt) {
                track.state = BlipState.CONTACT
                if (!track.contactLogged) {
                    track.contactLogged = true
                    events += SessionEvent(EventKind.RADAR_CONTACT, elapsedMillis)
                }
            }
            if (elapsedMillis >= track.diesAt) {
                iterator.remove()
                if (track.contactLogged) {
                    events += SessionEvent(EventKind.RADAR_LOST, elapsedMillis)
                }
            }
        }

        val sinceSpawn = elapsedMillis - lastSpawnAt
        val minimumGap = 9_000L - (dev * 5_000L).toLong()
        if (tracks.size < 3 && sinceSpawn > minimumGap) {
            val chance = 0.05 + dev * 0.25
            if (random.nextDouble() < chance) {
                lastSpawnAt = elapsedMillis
                val contactDelay = random.nextLong(1_400L, 3_200L)
                val life = random.nextLong(9_000L, 22_000L)
                tracks += Track(
                    id = nextId++,
                    angleDeg = random.nextFloat() * 360f,
                    radiusFraction = 0.28f + random.nextFloat() * 0.6f,
                    state = BlipState.TRACE,
                    bornAt = elapsedMillis,
                    contactAt = elapsedMillis + contactDelay,
                    diesAt = elapsedMillis + contactDelay + life,
                    contactLogged = false
                )
                events += SessionEvent(EventKind.RADAR_TRACE, elapsedMillis)
            }
        }
        return events
    }

    /** Drop all tracks, e.g. when a session ends. */
    fun clear() {
        tracks.clear()
        lastSpawnAt = 0L
    }
}
