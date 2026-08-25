package com.rork.ghostdetectorspiritbox.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Small persisted flags. Nothing here leaves the device. */
class AppPreferences private constructor(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("gd7_prefs", Context.MODE_PRIVATE)

    private val _briefingComplete = MutableStateFlow(prefs.getBoolean(KEY_BRIEFING, false))
    val briefingComplete: StateFlow<Boolean> = _briefingComplete.asStateFlow()

    fun completeBriefing() {
        prefs.edit().putBoolean(KEY_BRIEFING, true).apply()
        _briefingComplete.value = true
    }

    companion object {
        private const val KEY_BRIEFING = "briefing_complete"

        @Volatile
        private var instance: AppPreferences? = null

        fun get(context: Context): AppPreferences =
            instance ?: synchronized(this) {
                instance ?: AppPreferences(context).also { instance = it }
            }
    }
}
