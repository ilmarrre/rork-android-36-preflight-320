package com.rork.ghostdetectorspiritbox.domain

import kotlinx.serialization.Serializable
import java.util.Locale

/** Every kind of entry that can appear in a session log. */
@Serializable
enum class EventKind {
    SESSION_START,
    BASELINE,
    WORD,
    QUESTION,
    EMF_SPIKE,
    RADAR_TRACE,
    RADAR_CONTACT,
    RADAR_LOST,
    MARKED,
    PAUSE,
    SESSION_END
}

/**
 * A single timestamped entry. [atMillis] is the offset from the session start,
 * so records stay readable regardless of wall-clock time.
 */
@Serializable
data class SessionEvent(
    val kind: EventKind,
    val atMillis: Long,
    val text: String = "",
    val value: Double? = null,
    val category: String? = null,
    val marked: Boolean = false
)

/** A finished investigation session, stored as an evidence record. */
@Serializable
data class SessionRecord(
    val id: String,
    val number: Int,
    val startedAtEpochMillis: Long,
    val durationMillis: Long,
    val baselineMicroTesla: Double,
    val peakMicroTesla: Double,
    val thresholdMicroTesla: Double,
    val events: List<SessionEvent>
)

/** Number of engine responses logged in this record. */
val SessionRecord.responseCount: Int
    get() = events.count { it.kind == EventKind.WORD }

/** Number of radar contacts logged in this record. */
val SessionRecord.contactCount: Int
    get() = events.count { it.kind == EventKind.RADAR_CONTACT }

/** Number of manually marked moments, including starred responses. */
val SessionRecord.markedCount: Int
    get() = events.count { it.kind == EventKind.MARKED || it.marked }

/** Entries a reader cares about, in chronological order. */
val SessionRecord.timeline: List<SessionEvent>
    get() = events
        .filter { it.kind != EventKind.SESSION_START && it.kind != EventKind.BASELINE }
        .sortedBy { it.atMillis }

private val NOTABLE_KINDS = setOf(
    EventKind.WORD,
    EventKind.QUESTION,
    EventKind.EMF_SPIKE,
    EventKind.RADAR_CONTACT,
    EventKind.MARKED
)

/**
 * The 30-second window holding the densest activity, formatted as MM:SS–MM:SS.
 * Returns null when the record has too little activity to name an interval.
 */
fun SessionRecord.strongestInterval(): String? {
    val notable = events.filter { it.kind in NOTABLE_KINDS }
    if (notable.size < 2) return null
    val window = 30_000L
    var bestStart = 0L
    var bestCount = 0
    var index = 0
    while (index < notable.size) {
        val start = notable[index].atMillis
        val count = notable.count { it.atMillis >= start && it.atMillis < start + window }
        if (count > bestCount) {
            bestCount = count
            bestStart = start
        }
        index++
    }
    if (bestCount < 2) return null
    val alignedStart = (bestStart / 10_000L) * 10_000L
    val end = (alignedStart + window).coerceAtMost(durationMillis)
    return "${formatClock(alignedStart)}–${formatClock(end)}"
}

/** Recurring themes across engine responses, e.g. "NAMES (3) · COLD (2)". */
fun SessionRecord.recurringThemes(): String? {
    val counts = events
        .filter { it.kind == EventKind.WORD && it.category != null }
        .groupingBy { it.category!! }
        .eachCount()
        .filter { it.value >= 2 }
        .entries
        .sortedByDescending { it.value }
        .take(3)
    if (counts.isEmpty()) return null
    return counts.joinToString(" · ") { "${it.key} (${it.value})" }
}

/** MM:SS for a duration in milliseconds. */
fun formatClock(millis: Long): String {
    val total = (millis / 1000L).coerceAtLeast(0L)
    return String.format(Locale.US, "%02d:%02d", total / 60, total % 60)
}

/** One decimal magnetic field value. */
fun formatMicroTesla(value: Double): String = String.format(Locale.US, "%.1f", value)
