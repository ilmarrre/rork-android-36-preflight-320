package com.rork.ghostdetectorspiritbox.ui.features.briefing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.rork.ghostdetectorspiritbox.R
import com.rork.ghostdetectorspiritbox.config.Space
import com.rork.ghostdetectorspiritbox.config.Tokens
import com.rork.ghostdetectorspiritbox.services.SensorStatus
import com.rork.ghostdetectorspiritbox.ui.instrument.HardwareButton
import com.rork.ghostdetectorspiritbox.ui.instrument.InstrumentPanel
import com.rork.ghostdetectorspiritbox.ui.instrument.InstrumentScreen
import com.rork.ghostdetectorspiritbox.ui.instrument.PhosphorDisplay
import com.rork.ghostdetectorspiritbox.ui.instrument.ReadoutRow
import com.rork.ghostdetectorspiritbox.ui.instrument.StatusLabel
import com.rork.ghostdetectorspiritbox.ui.theme.Type

/** First launch only: what the unit is, what it honestly does, then a sensor check. */
@Composable
fun BriefingScreen(
    sensorStatus: SensorStatus,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var step by remember { mutableIntStateOf(0) }

    InstrumentScreen(modifier = modifier) {
        StatusLabel(
            text = stringResource(R.string.unit_name),
            lit = true,
            pulsing = step == 1,
            modifier = Modifier.padding(top = Space.Control)
        )

        if (step == 0) {
            WelcomeStep(modifier = Modifier.weight(1f))
            HardwareButton(
                label = stringResource(R.string.briefing_continue),
                onClick = { step = 1 },
                dominant = true,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            SensorCheckStep(sensorStatus = sensorStatus, modifier = Modifier.weight(1f))
            HardwareButton(
                label = stringResource(R.string.briefing_enter),
                onClick = onComplete,
                dominant = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Text(
            text = stringResource(R.string.disclosure_footer),
            style = Type.labelSmall,
            color = Tokens.boneFaint,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = Space.Control)
        )
    }
}

@Composable
private fun WelcomeStep(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(Space.Control)
    ) {
        PhosphorDisplay(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(Space.Sm)) {
                Text(
                    text = stringResource(R.string.briefing_title),
                    style = Type.readoutLarge,
                    color = Tokens.phosphor
                )
                Text(
                    text = stringResource(R.string.briefing_subtitle),
                    style = Type.readout,
                    color = Tokens.phosphorMuted
                )
                Spacer(Modifier.height(Space.Xs))
                Text(
                    text = stringResource(R.string.briefing_tagline),
                    style = Type.monoSmall,
                    color = Tokens.phosphorMuted
                )
            }
        }

        InstrumentPanel(modifier = Modifier.fillMaxWidth(), screws = true) {
            Column(
                modifier = Modifier.padding(Space.Md),
                verticalArrangement = Arrangement.spacedBy(Space.Control)
            ) {
                Text(
                    text = stringResource(R.string.briefing_how_it_works),
                    style = Type.label,
                    color = Tokens.phosphor
                )
                TruthLine(
                    title = stringResource(R.string.briefing_emf_title),
                    body = stringResource(R.string.briefing_emf_body)
                )
                TruthLine(
                    title = stringResource(R.string.briefing_signal_title),
                    body = stringResource(R.string.briefing_signal_body)
                )
                TruthLine(
                    title = stringResource(R.string.briefing_box_title),
                    body = stringResource(R.string.briefing_box_body)
                )
                TruthLine(
                    title = stringResource(R.string.briefing_record_title),
                    body = stringResource(R.string.briefing_record_body)
                )
                Text(
                    text = stringResource(R.string.briefing_disclaimer),
                    style = Type.bodySmall,
                    color = Tokens.boneMute
                )
            }
        }
    }
}

@Composable
private fun TruthLine(title: String, body: String) {
    Column(
        modifier = Modifier.semantics(mergeDescendants = true) { },
        verticalArrangement = Arrangement.spacedBy(Space.Xs)
    ) {
        Text(text = title, style = Type.label, color = Tokens.bone)
        Text(text = body, style = Type.body, color = Tokens.boneMute)
    }
}

@Composable
private fun SensorCheckStep(sensorStatus: SensorStatus, modifier: Modifier = Modifier) {
    val online = sensorStatus == SensorStatus.ONLINE
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(Space.Control)
    ) {
        PhosphorDisplay(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Space.Sm)
            ) {
                Text(
                    text = stringResource(R.string.briefing_sensor_check),
                    style = Type.monoSmall,
                    color = Tokens.phosphorMuted
                )
                Text(
                    text = if (online) {
                        stringResource(R.string.status_ready)
                    } else {
                        stringResource(R.string.status_limited)
                    },
                    style = Type.hero,
                    color = if (online) Tokens.phosphor else Tokens.signalText
                )
            }
        }

        InstrumentPanel(modifier = Modifier.fillMaxWidth()) {
            Column {
                ReadoutRow(
                    label = stringResource(R.string.label_magnetometer),
                    value = if (online) {
                        stringResource(R.string.status_online)
                    } else {
                        stringResource(R.string.status_not_found)
                    },
                    valueColor = if (online) Tokens.phosphor else Tokens.signalText
                )
                ReadoutRow(
                    label = stringResource(R.string.label_signal_model),
                    value = stringResource(R.string.status_ready)
                )
                ReadoutRow(
                    label = stringResource(R.string.label_word_engine),
                    value = stringResource(R.string.status_local)
                )
                ReadoutRow(
                    label = stringResource(R.string.label_archive),
                    value = stringResource(R.string.status_on_device),
                    divider = false
                )
            }
        }

        Text(
            text = if (online) {
                stringResource(R.string.briefing_hold_still)
            } else {
                stringResource(R.string.briefing_no_sensor)
            },
            style = Type.body,
            color = Tokens.boneMute
        )
    }
}
