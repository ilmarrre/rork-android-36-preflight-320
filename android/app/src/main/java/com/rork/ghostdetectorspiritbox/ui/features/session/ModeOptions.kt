package com.rork.ghostdetectorspiritbox.ui.features.session

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.rork.ghostdetectorspiritbox.R
import com.rork.ghostdetectorspiritbox.ui.instrument.ModeOption

/** The three views of one session, localized once and shared by idle and running screens. */
@Composable
fun rememberModeOptions(): List<ModeOption<SessionMode>> {
    val radar = stringResource(R.string.mode_radar)
    val box = stringResource(R.string.mode_box)
    val emf = stringResource(R.string.mode_emf)
    val radarSpoken = stringResource(R.string.a11y_mode_format, radar)
    val boxSpoken = stringResource(R.string.a11y_mode_format, box)
    val emfSpoken = stringResource(R.string.a11y_mode_format, emf)
    return remember(radar, box, emf) {
        listOf(
            ModeOption(SessionMode.RADAR, radar, radarSpoken),
            ModeOption(SessionMode.BOX, box, boxSpoken),
            ModeOption(SessionMode.EMF, emf, emfSpoken)
        )
    }
}
