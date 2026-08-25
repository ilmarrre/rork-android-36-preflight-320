package com.rork.ghostdetectorspiritbox.ui.features.investigate

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.rork.ghostdetectorspiritbox.R
import com.rork.ghostdetectorspiritbox.config.Limits
import com.rork.ghostdetectorspiritbox.config.Space
import com.rork.ghostdetectorspiritbox.config.Tokens
import com.rork.ghostdetectorspiritbox.domain.formatClock
import com.rork.ghostdetectorspiritbox.services.SensorStatus
import com.rork.ghostdetectorspiritbox.ui.features.session.SessionMode
import com.rork.ghostdetectorspiritbox.ui.features.session.SessionUiState
import com.rork.ghostdetectorspiritbox.ui.features.session.rememberModeOptions
import com.rork.ghostdetectorspiritbox.ui.instrument.HardwareButton
import com.rork.ghostdetectorspiritbox.ui.instrument.InstrumentPanel
import com.rork.ghostdetectorspiritbox.ui.instrument.InstrumentScreen
import com.rork.ghostdetectorspiritbox.ui.instrument.ModeSelector
import com.rork.ghostdetectorspiritbox.ui.instrument.PhosphorDisplay
import com.rork.ghostdetectorspiritbox.ui.instrument.ReadoutRow
import com.rork.ghostdetectorspiritbox.ui.instrument.StaticRadarGraticule
import com.rork.ghostdetectorspiritbox.ui.instrument.StatusLamp
import com.rork.ghostdetectorspiritbox.ui.instrument.TopStatusBar
import com.rork.ghostdetectorspiritbox.ui.theme.Type

/**
 * Idle instrument panel. Nothing here animates or reads out before the user starts a
 * session, and no baseline is shown because it is measured on start.
 */
@Composable
fun InvestigateScreen(
    state: SessionUiState,
    onModeChange: (SessionMode) -> Unit,
    onStart: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sensorOnline = state.sensorStatus == SensorStatus.ONLINE
    val startLabel = when (state.mode) {
        SessionMode.RADAR -> stringResource(R.string.start_radar)
        SessionMode.BOX -> stringResource(R.string.start_box)
        SessionMode.EMF -> stringResource(R.string.start_emf)
    }

    InstrumentScreen(modifier = modifier, applySafeArea = false) {
        TopStatusBar {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Space.Xs)
            ) {
                Text(
                    text = stringResource(R.string.unit_name),
                    style = Type.heading,
                    color = Tokens.bone
                )
                Text(
                    text = stringResource(
                        R.string.idle_status_format,
                        if (sensorOnline) {
                            stringResource(R.string.status_ready)
                        } else {
                            stringResource(R.string.status_limited)
                        }
                    ),
                    style = Type.label,
                    color = if (sensorOnline) Tokens.boneMute else Tokens.signalText
                )
                Text(
                    text = stringResource(R.string.idle_session_not_running),
                    style = Type.label,
                    color = Tokens.boneMute
                )
            }
            StatusLamp(lit = sensorOnline, size = Space.Md)
        }

        PhosphorDisplay(modifier = Modifier.weight(1f)) {
            Box(modifier = Modifier.fillMaxSize()) {
                StaticRadarGraticule()
                Text(
                    text = stringResource(R.string.label_signal_model),
                    style = Type.monoSmall,
                    color = Tokens.phosphorMuted,
                    modifier = Modifier.align(Alignment.TopStart)
                )
                Text(
                    text = stringResource(R.string.status_standby),
                    style = Type.monoSmall,
                    color = Tokens.phosphorMuted,
                    modifier = Modifier.align(Alignment.TopEnd)
                )
            }
        }

        InstrumentPanel(modifier = Modifier.fillMaxWidth()) {
            Column {
                ReadoutRow(
                    label = stringResource(R.string.label_emf_sensor),
                    value = if (sensorOnline) {
                        stringResource(R.string.status_online)
                    } else {
                        stringResource(R.string.status_unavailable)
                    },
                    valueColor = if (sensorOnline) Tokens.phosphor else Tokens.signalText
                )
                ReadoutRow(
                    label = stringResource(R.string.label_free_session),
                    value = formatClock(Limits.FREE_SESSION_MILLIS)
                )
                ReadoutRow(
                    label = stringResource(R.string.label_archive),
                    value = stringResource(
                        R.string.archive_capacity_format,
                        state.archiveCount,
                        state.archiveCapacity
                    ),
                    valueColor = if (state.archiveFull) Tokens.signalText else Tokens.phosphor,
                    divider = false
                )
            }
        }

        ModeSelector(
            options = rememberModeOptions(),
            selected = state.mode,
            onSelect = onModeChange,
            modifier = Modifier.fillMaxWidth()
        )

        HardwareButton(
            label = startLabel,
            onClick = onStart,
            dominant = true,
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = if (state.archiveFull) {
                stringResource(R.string.idle_archive_full_hint, state.archiveCapacity)
            } else {
                stringResource(R.string.idle_baseline_hint)
            },
            style = Type.bodySmall,
            color = if (state.archiveFull) Tokens.signalText else Tokens.boneMute,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = Space.Control)
        )
    }
}
