package com.rork.ghostdetectorspiritbox.ui.text

import android.content.Context
import com.rork.ghostdetectorspiritbox.R
import com.rork.ghostdetectorspiritbox.domain.EventKind
import com.rork.ghostdetectorspiritbox.domain.SessionEvent
import com.rork.ghostdetectorspiritbox.domain.formatClock
import com.rork.ghostdetectorspiritbox.domain.formatMicroTesla

/**
 * One localized line for a log or timeline entry. Word entries are engine output and
 * are shown verbatim; everything else is a localized instrument phrase.
 */
fun SessionEvent.describe(context: Context): String = when (kind) {
    EventKind.WORD -> text
    EventKind.QUESTION -> context.getString(R.string.event_question_format, text)
    EventKind.EMF_SPIKE ->
        context.getString(R.string.event_emf_spike_format, formatMicroTesla(value ?: 0.0))
    EventKind.RADAR_TRACE -> context.getString(R.string.event_radar_trace)
    EventKind.RADAR_CONTACT -> context.getString(R.string.event_radar_contact)
    EventKind.RADAR_LOST -> context.getString(R.string.event_radar_lost)
    EventKind.MARKED -> context.getString(R.string.event_marked)
    EventKind.PAUSE ->
        context.getString(R.string.event_pause_format, formatClock((value ?: 0.0).toLong()))
    EventKind.BASELINE ->
        context.getString(R.string.event_baseline_format, formatMicroTesla(value ?: 0.0))
    EventKind.SESSION_START -> context.getString(R.string.event_session_start)
    EventKind.SESSION_END -> context.getString(R.string.event_session_end)
}

/** True for entries that deserve amber emphasis. */
fun SessionEvent.isEmphasised(): Boolean =
    kind == EventKind.EMF_SPIKE || kind == EventKind.MARKED || marked
