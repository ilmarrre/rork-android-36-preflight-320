package com.rork.ghostdetectorspiritbox.config

/** Free-tier limits, session tuning and thresholds. Single source of truth. */
object Limits {

    /** Free tier session length. */
    const val FREE_SESSION_MILLIS: Long = 15 * 60 * 1000L

    /** Free tier archive capacity. */
    const val FREE_ARCHIVE_CAPACITY: Int = 3

    /** How long a session survives in the background before it is closed. */
    const val GRACE_PERIOD_MILLIS: Long = 5 * 60 * 1000L

    /** Shortest background gap worth logging as a pause. */
    const val MIN_PAUSE_MILLIS: Long = 4_000L

    /** Baseline measurement window at the start of every session. */
    const val CALIBRATION_MILLIS: Long = 3_500L

    /** Spike threshold as a multiple of the measured baseline. */
    const val THRESHOLD_FACTOR: Double = 2.0

    /** Field must fall this far below the threshold before a spike is released. */
    const val SPIKE_RELEASE_MARGIN: Double = 4.0

    /** Session loop tick. */
    const val TICK_MILLIS: Long = 90L

    /** History sampling for the 60 s trace. */
    const val HISTORY_INTERVAL_MILLIS: Long = 250L
    const val HISTORY_POINTS: Int = 240

    /** Signal-model tick. */
    const val RADAR_INTERVAL_MILLIS: Long = 300L

    /** How long a drawn word stays on the display. */
    const val WORD_HOLD_MILLIS: Long = 5_000L

    /** A star attaches to the latest response if it is no older than this. */
    const val MARK_ATTACH_WINDOW_MILLIS: Long = 12_000L

    /** Deliberate hold required to stop a session. */
    const val HOLD_STOP_MILLIS: Int = 1_100
}

/** Build-time feature flags. Everything beyond the MVP boundary stays off. */
object FeatureFlags {
    const val PURCHASES_ENABLED: Boolean = false
    const val ANALYTICS_ENABLED: Boolean = false
    const val CLOUD_SYNC_ENABLED: Boolean = false
}
