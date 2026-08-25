package com.rork.ghostdetectorspiritbox.ui.features.session

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rork.ghostdetectorspiritbox.R
import com.rork.ghostdetectorspiritbox.config.Motion
import com.rork.ghostdetectorspiritbox.config.Radius
import com.rork.ghostdetectorspiritbox.config.Sizes
import com.rork.ghostdetectorspiritbox.config.Space
import com.rork.ghostdetectorspiritbox.config.Tokens
import com.rork.ghostdetectorspiritbox.domain.BlipState
import com.rork.ghostdetectorspiritbox.domain.EventKind
import com.rork.ghostdetectorspiritbox.domain.SessionEvent
import com.rork.ghostdetectorspiritbox.domain.formatClock
import com.rork.ghostdetectorspiritbox.domain.formatMicroTesla
import com.rork.ghostdetectorspiritbox.ui.instrument.ControlRow
import com.rork.ghostdetectorspiritbox.ui.instrument.HardwareButton
import com.rork.ghostdetectorspiritbox.ui.instrument.HardwareHoldButton
import com.rork.ghostdetectorspiritbox.ui.instrument.InlineInstrumentError
import com.rork.ghostdetectorspiritbox.ui.instrument.InstrumentDivider
import com.rork.ghostdetectorspiritbox.ui.instrument.InstrumentPanel
import com.rork.ghostdetectorspiritbox.ui.instrument.InstrumentScreen
import com.rork.ghostdetectorspiritbox.ui.instrument.ModeSelector
import com.rork.ghostdetectorspiritbox.ui.instrument.PhosphorDisplay
import com.rork.ghostdetectorspiritbox.ui.instrument.RadarDisplay
import com.rork.ghostdetectorspiritbox.ui.instrument.SegmentValue
import com.rork.ghostdetectorspiritbox.ui.instrument.StatusLamp
import com.rork.ghostdetectorspiritbox.ui.instrument.TraceGraph
import com.rork.ghostdetectorspiritbox.ui.text.describe
import com.rork.ghostdetectorspiritbox.ui.text.isEmphasised
import com.rork.ghostdetectorspiritbox.ui.theme.LocalReducedMotion
import com.rork.ghostdetectorspiritbox.ui.theme.Type

private val LIVE_LOG_KINDS = setOf(
    EventKind.WORD,
    EventKind.QUESTION,
    EventKind.EMF_SPIKE,
    EventKind.MARKED,
    EventKind.RADAR_CONTACT
)

/**
 * The running session. There is no close control and no tab bar here: the session ends
 * only through HOLD STOP, and Android Back asks for confirmation first.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionScreen(
    state: SessionUiState,
    onModeChange: (SessionMode) -> Unit,
    onAsk: (String) -> Unit,
    onMark: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    var askVisible by remember { mutableStateOf(false) }
    var confirmBack by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val view = LocalView.current

    LaunchedEffect(state.isRunning) {
        view.keepScreenOn = state.isRunning
    }

    BackHandler(enabled = state.isRunning) { confirmBack = true }

    InstrumentScreen(modifier = modifier) {
        SessionHeader(state = state, onMark = onMark)

        Box(modifier = Modifier.weight(1f)) {
            when (state.mode) {
                SessionMode.RADAR -> RadarMode(state = state)
                SessionMode.BOX -> BoxMode(state = state)
                SessionMode.EMF -> EmfMode(state = state)
            }
        }

        ModeSelector(
            options = rememberModeOptions(),
            selected = state.mode,
            onSelect = onModeChange,
            modifier = Modifier.fillMaxWidth()
        )

        ControlRow(modifier = Modifier.padding(bottom = Space.Control)) {
            HardwareButton(
                label = stringResource(R.string.session_ask),
                accessibilityLabel = stringResource(R.string.a11y_ask),
                onClick = { askVisible = true },
                dominant = true,
                enabled = state.phase == SessionPhase.RUNNING,
                modifier = Modifier.weight(1f)
            )
            HardwareHoldButton(
                label = stringResource(R.string.session_hold_stop),
                holdCaption = stringResource(R.string.session_hold),
                completingCaption = stringResource(R.string.session_ending),
                accessibilityLabel = stringResource(R.string.a11y_hold_stop),
                onComplete = onStop,
                modifier = Modifier.weight(1f)
            )
        }
    }

    if (askVisible) {
        ModalBottomSheet(
            onDismissRequest = { askVisible = false },
            sheetState = sheetState,
            containerColor = Tokens.caseEdge,
            contentColor = Tokens.bone,
            scrimColor = Tokens.ink.copy(alpha = 0.8f)
        ) {
            AskSheetContent(
                onAsk = { question ->
                    onAsk(question)
                    askVisible = false
                }
            )
        }
    }

    if (confirmBack) {
        AlertDialog(
            onDismissRequest = { confirmBack = false },
            containerColor = Tokens.caseEdge,
            titleContentColor = Tokens.bone,
            textContentColor = Tokens.boneMute,
            title = {
                Text(
                    text = stringResource(R.string.confirm_leave_title),
                    style = Type.labelLarge,
                    color = Tokens.bone
                )
            },
            text = {
                Text(text = stringResource(R.string.confirm_leave_body), style = Type.body)
            },
            confirmButton = {
                TextButton(onClick = { confirmBack = false }) {
                    Text(
                        text = stringResource(R.string.confirm_keep_running),
                        style = Type.label,
                        color = Tokens.phosphor
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    confirmBack = false
                    onStop()
                }) {
                    Text(
                        text = stringResource(R.string.confirm_end_session),
                        style = Type.label,
                        color = Tokens.signalText
                    )
                }
            }
        )
    }
}

@Composable
private fun SessionHeader(state: SessionUiState, onMark: () -> Unit) {
    val elapsed = formatClock(state.elapsedMillis)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Space.Control),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatusLamp(lit = true, pulsing = true)
        Spacer(Modifier.width(Space.Sm))
        Text(
            text = if (state.phase == SessionPhase.CALIBRATING) {
                stringResource(R.string.session_baseline)
            } else {
                stringResource(R.string.session_logging)
            },
            style = Type.label,
            color = Tokens.bone
        )
        Spacer(Modifier.width(Space.Sm))
        SegmentValue(
            value = elapsed,
            placeholder = "88:88",
            contentDescription = stringResource(R.string.a11y_elapsed_format, elapsed)
        )
        Spacer(Modifier.weight(1f))
        state.liveMicroTesla?.let { live ->
            val value = formatMicroTesla(live)
            val spoken = stringResource(R.string.a11y_emf_value_format, value)
            Text(
                text = stringResource(
                    R.string.session_value_format,
                    value,
                    stringResource(R.string.unit_microtesla)
                ),
                style = Type.readout,
                color = Tokens.phosphor,
                modifier = Modifier.semantics { contentDescription = spoken }
            )
        }
        Spacer(Modifier.width(Space.Sm))
        MarkKey(onClick = onMark, enabled = state.phase == SessionPhase.RUNNING)
    }
}

@Composable
private fun MarkKey(onClick: () -> Unit, enabled: Boolean) {
    val label = stringResource(R.string.a11y_mark)
    Box(
        modifier = Modifier
            .size(Sizes.Touch)
            .clip(Radius.Control)
            .background(Tokens.caseEdge)
            .border(Space.Hairline, Tokens.caseLift.copy(alpha = 0.7f), Radius.Control)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.session_mark),
            style = Type.readout,
            color = if (enabled) Tokens.phosphor else Tokens.boneFaint
        )
    }
}

@Composable
private fun RadarMode(state: SessionUiState) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(Space.Control)
    ) {
        PhosphorDisplay(modifier = Modifier.weight(1f)) {
            Box(modifier = Modifier.fillMaxSize()) {
                RadarDisplay(
                    active = state.phase == SessionPhase.RUNNING,
                    blips = state.blips
                )
                Text(
                    text = stringResource(R.string.label_signal_model),
                    style = Type.monoSmall,
                    color = Tokens.phosphorMuted,
                    modifier = Modifier.align(Alignment.TopStart)
                )
                Text(
                    text = if (state.phase == SessionPhase.CALIBRATING) {
                        stringResource(R.string.session_calibrating)
                    } else {
                        stringResource(R.string.session_sweeping)
                    },
                    style = Type.monoSmall,
                    color = Tokens.phosphor,
                    modifier = Modifier.align(Alignment.TopEnd)
                )
                Text(
                    text = stringResource(
                        R.string.session_contacts_format,
                        state.blips.count { it.state == BlipState.CONTACT }
                    ),
                    style = Type.monoSmall,
                    color = Tokens.phosphorMuted,
                    modifier = Modifier.align(Alignment.BottomStart)
                )
            }
        }
        InstrumentPanel(modifier = Modifier.heightIn(min = 140.dp)) {
            LiveLog(events = state.log)
        }
    }
}

@Composable
private fun BoxMode(state: SessionUiState) {
    val reducedMotion = LocalReducedMotion.current
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(Space.Control)
    ) {
        PhosphorDisplay(modifier = Modifier.heightIn(min = 180.dp)) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Space.Sm)
                ) {
                    val waiting = stringResource(R.string.session_waiting)
                    AnimatedContent(
                        targetState = state.currentWord,
                        transitionSpec = {
                            fadeIn(Motion.enter(reducedMotion)) togetherWith
                                fadeOut(Motion.exit(reducedMotion))
                        },
                        label = "word"
                    ) { word ->
                        Text(
                            text = (word ?: waiting).uppercase(),
                            style = if ((word ?: waiting).length > 7) Type.readoutLarge else Type.hero,
                            color = if (word == null) Tokens.phosphorMuted else Tokens.phosphor,
                            textAlign = TextAlign.Center
                        )
                    }
                    Text(
                        text = if (state.phase == SessionPhase.CALIBRATING) {
                            stringResource(R.string.session_measuring)
                        } else {
                            stringResource(R.string.session_listening)
                        },
                        style = Type.monoSmall,
                        color = Tokens.phosphorMuted
                    )
                }
            }
        }
        InstrumentPanel(modifier = Modifier.weight(1f)) {
            LiveLog(events = state.log)
        }
    }
}

@Composable
private fun EmfMode(state: SessionUiState) {
    val threshold = state.thresholdMicroTesla
    val placeholder = stringResource(R.string.value_placeholder)
    val liveText = state.liveMicroTesla?.let { formatMicroTesla(it) } ?: placeholder

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(Space.Control)
    ) {
        PhosphorDisplay(modifier = Modifier.heightIn(min = 140.dp)) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        SegmentValue(
                            value = liveText,
                            placeholder = "888.8",
                            style = Type.display,
                            color = if (state.spikeActive) Tokens.signalText else Tokens.phosphor,
                            contentDescription = stringResource(
                                R.string.a11y_emf_value_format,
                                liveText
                            )
                        )
                        Spacer(Modifier.width(Space.Sm))
                        Text(
                            text = stringResource(R.string.unit_microtesla),
                            style = Type.readout,
                            color = Tokens.phosphorMuted,
                            modifier = Modifier.padding(bottom = Space.Sm)
                        )
                    }
                    Text(
                        text = stringResource(R.string.session_live_field),
                        style = Type.monoSmall,
                        color = Tokens.phosphorMuted
                    )
                }
            }
        }

        PhosphorDisplay(modifier = Modifier.weight(1f)) {
            Column(modifier = Modifier.fillMaxSize()) {
                Text(
                    text = stringResource(R.string.session_last_60),
                    style = Type.monoSmall,
                    color = Tokens.phosphorMuted
                )
                TraceGraph(
                    samples = state.history,
                    threshold = threshold?.toFloat(),
                    contentDescription = stringResource(R.string.a11y_trace_format, liveText),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(top = Space.Xs)
                )
            }
        }

        InstrumentPanel(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Space.Sm),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MetricInline(
                    label = stringResource(R.string.session_base),
                    value = state.baselineMicroTesla?.let { formatMicroTesla(it) } ?: placeholder
                )
                MetricInline(
                    label = stringResource(R.string.session_peak),
                    value = formatMicroTesla(state.peakMicroTesla)
                )
                MetricInline(
                    label = stringResource(R.string.session_threshold),
                    value = threshold?.let { formatMicroTesla(it) } ?: placeholder
                )
            }
        }

        Text(
            text = state.lastSpikeAtMillis?.let {
                stringResource(R.string.session_last_spike_format, state.spikeCount, formatClock(it))
            } ?: stringResource(R.string.session_spikes_format, state.spikeCount),
            style = Type.bodySmall,
            color = Tokens.boneMute,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        if (state.spikeActive) {
            InlineInstrumentError(text = stringResource(R.string.session_spike_active))
        }
    }
}

@Composable
private fun MetricInline(label: String, value: String) {
    Column(
        modifier = Modifier.semantics(mergeDescendants = true) { },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = label, style = Type.labelSmall, color = Tokens.boneMute)
        Text(text = value, style = Type.readout, color = Tokens.phosphor)
    }
}

@Composable
private fun LiveLog(events: List<SessionEvent>) {
    val context = LocalContext.current
    val visible = remember(events) {
        events.filter { it.kind in LIVE_LOG_KINDS }.sortedByDescending { it.atMillis }
    }
    if (visible.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(R.string.session_no_entries),
                style = Type.label,
                color = Tokens.boneMute
            )
        }
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(items = visible, key = { "${it.kind}-${it.atMillis}-${it.text}" }) { event ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics(mergeDescendants = true) { }
                    .padding(horizontal = Space.Md, vertical = Space.Control),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatClock(event.atMillis),
                    style = Type.monoSmall,
                    color = Tokens.boneMute
                )
                Spacer(Modifier.width(Space.Control))
                Text(
                    text = event.describe(context),
                    style = Type.mono,
                    color = if (event.isEmphasised()) Tokens.phosphor else Tokens.bone,
                    modifier = Modifier.weight(1f)
                )
                if (event.marked) {
                    Text(
                        text = stringResource(R.string.session_mark),
                        style = Type.mono,
                        color = Tokens.phosphor
                    )
                }
            }
            InstrumentDivider()
        }
    }
}

@Composable
private fun AskSheetContent(onAsk: (String) -> Unit) {
    var custom by remember { mutableStateOf("") }
    val prepared = stringArrayResource(R.array.prepared_questions)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Space.Screen)
            .padding(bottom = Space.Xl),
        verticalArrangement = Arrangement.spacedBy(Space.Control)
    ) {
        Text(
            text = stringResource(R.string.ask_title),
            style = Type.labelLarge,
            color = Tokens.bone
        )
        Text(
            text = stringResource(R.string.ask_body),
            style = Type.bodySmall,
            color = Tokens.boneMute
        )
        prepared.forEach { question ->
            HardwareButton(
                label = question.uppercase(),
                accessibilityLabel = question,
                onClick = { onAsk(question) },
                modifier = Modifier.fillMaxWidth()
            )
        }
        OutlinedTextField(
            value = custom,
            onValueChange = { custom = it },
            label = {
                Text(text = stringResource(R.string.ask_own_question), style = Type.labelSmall)
            },
            singleLine = true,
            textStyle = Type.mono.copy(color = Tokens.bone),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Tokens.phosphor,
                unfocusedBorderColor = Tokens.caseLift,
                focusedLabelColor = Tokens.phosphor,
                unfocusedLabelColor = Tokens.boneMute,
                cursorColor = Tokens.phosphor,
                focusedTextColor = Tokens.bone,
                unfocusedTextColor = Tokens.bone
            ),
            modifier = Modifier.fillMaxWidth()
        )
        HardwareButton(
            label = stringResource(R.string.ask_send),
            onClick = { onAsk(custom) },
            dominant = true,
            enabled = custom.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
