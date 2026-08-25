package com.rork.ghostdetectorspiritbox.ui.features.record

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.rork.ghostdetectorspiritbox.R
import com.rork.ghostdetectorspiritbox.config.Space
import com.rork.ghostdetectorspiritbox.config.Tokens
import com.rork.ghostdetectorspiritbox.domain.SessionRecord
import com.rork.ghostdetectorspiritbox.domain.contactCount
import com.rork.ghostdetectorspiritbox.domain.formatClock
import com.rork.ghostdetectorspiritbox.domain.formatMicroTesla
import com.rork.ghostdetectorspiritbox.domain.markedCount
import com.rork.ghostdetectorspiritbox.domain.recurringThemes
import com.rork.ghostdetectorspiritbox.domain.responseCount
import com.rork.ghostdetectorspiritbox.domain.strongestInterval
import com.rork.ghostdetectorspiritbox.domain.timeline
import com.rork.ghostdetectorspiritbox.ui.instrument.CellDivider
import com.rork.ghostdetectorspiritbox.ui.instrument.ControlRow
import com.rork.ghostdetectorspiritbox.ui.instrument.EmptyInstrumentState
import com.rork.ghostdetectorspiritbox.ui.instrument.HardwareButton
import com.rork.ghostdetectorspiritbox.ui.instrument.InstrumentDivider
import com.rork.ghostdetectorspiritbox.ui.instrument.InstrumentPanel
import com.rork.ghostdetectorspiritbox.ui.instrument.InstrumentScreen
import com.rork.ghostdetectorspiritbox.ui.instrument.InstrumentSurface
import com.rork.ghostdetectorspiritbox.ui.instrument.MetricCell
import com.rork.ghostdetectorspiritbox.ui.text.describe
import com.rork.ghostdetectorspiritbox.ui.text.formatRecordDate
import com.rork.ghostdetectorspiritbox.ui.text.isEmphasised
import com.rork.ghostdetectorspiritbox.ui.theme.Type

/**
 * A finished session as a printed field report: masthead, summary, one chronological
 * timeline and the session pattern. Deletion lives only in the overflow menu.
 */
@Composable
fun RecordScreen(
    record: SessionRecord,
    justFinished: Boolean,
    endedByTimeLimit: Boolean,
    onBack: () -> Unit,
    onNewSession: () -> Unit,
    onOpenArchive: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var menuOpen by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val recordTitle = stringResource(R.string.record_number_format, record.number)

    InstrumentSurface(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Space.Sm, vertical = Space.Xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.a11y_back),
                        tint = Tokens.bone
                    )
                }
                Text(text = recordTitle, style = Type.heading, color = Tokens.bone)
                Spacer(Modifier.weight(1f))
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.a11y_record_options),
                            tint = Tokens.bone
                        )
                    }
                    DropdownMenu(
                        expanded = menuOpen,
                        onDismissRequest = { menuOpen = false },
                        containerColor = Tokens.caseEdge
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = stringResource(R.string.record_delete),
                                    style = Type.label,
                                    color = Tokens.signalText
                                )
                            },
                            onClick = {
                                menuOpen = false
                                confirmDelete = true
                            }
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(
                    horizontal = Space.Screen,
                    vertical = Space.Sm
                ),
                verticalArrangement = Arrangement.spacedBy(Space.Control)
            ) {
                item {
                    InstrumentPanel(modifier = Modifier.fillMaxWidth(), screws = true) {
                        Column(
                            modifier = Modifier
                                .padding(Space.Md)
                                .semantics(mergeDescendants = true) { },
                            verticalArrangement = Arrangement.spacedBy(Space.Xs)
                        ) {
                            Text(text = recordTitle, style = Type.heading, color = Tokens.bone)
                            Text(
                                text = formatRecordDate(record.startedAtEpochMillis),
                                style = Type.monoSmall,
                                color = Tokens.boneMute
                            )
                            Text(
                                text = stringResource(
                                    R.string.record_duration_format,
                                    formatClock(record.durationMillis)
                                ),
                                style = Type.monoSmall,
                                color = Tokens.boneMute
                            )
                            if (justFinished && endedByTimeLimit) {
                                Text(
                                    text = stringResource(R.string.record_limit_reached),
                                    style = Type.labelSmall,
                                    color = Tokens.signalText
                                )
                            }
                        }
                    }
                }

                item {
                    InstrumentPanel(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MetricCell(
                                label = stringResource(R.string.record_responses),
                                value = record.responseCount.toString(),
                                modifier = Modifier.weight(1f)
                            )
                            CellDivider()
                            MetricCell(
                                label = stringResource(R.string.record_contacts),
                                value = record.contactCount.toString(),
                                modifier = Modifier.weight(1f)
                            )
                            CellDivider()
                            MetricCell(
                                label = stringResource(R.string.record_peak_emf),
                                value = formatMicroTesla(record.peakMicroTesla),
                                valueColor = Tokens.phosphor,
                                modifier = Modifier.weight(1f)
                            )
                            CellDivider()
                            MetricCell(
                                label = stringResource(R.string.record_strongest),
                                value = record.strongestInterval()
                                    ?: stringResource(R.string.value_none),
                                modifier = Modifier.weight(1.2f)
                            )
                        }
                    }
                }

                item {
                    Text(
                        text = stringResource(R.string.record_timeline),
                        style = Type.label,
                        color = Tokens.boneMute,
                        modifier = Modifier.padding(start = Space.Xs, top = Space.Xs)
                    )
                }

                items(
                    items = record.timeline,
                    key = { "${it.kind}-${it.atMillis}-${it.text}" }
                ) { event ->
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics(mergeDescendants = true) { }
                                .padding(vertical = Space.Sm, horizontal = Space.Xs),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = formatClock(event.atMillis),
                                style = Type.monoSmall,
                                color = Tokens.boneMute
                            )
                            Spacer(Modifier.width(Space.Control))
                            Text(
                                text = event.describe(context) +
                                    if (event.marked) "  ★" else "",
                                style = Type.mono,
                                color = if (event.isEmphasised()) Tokens.phosphor else Tokens.bone,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        InstrumentDivider()
                    }
                }

                item {
                    InstrumentPanel(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Space.Md)
                                .semantics(mergeDescendants = true) { },
                            verticalArrangement = Arrangement.spacedBy(Space.Xs)
                        ) {
                            Text(
                                text = stringResource(
                                    R.string.record_recurring_format,
                                    record.recurringThemes()
                                        ?: stringResource(R.string.record_recurring_none)
                                ),
                                style = Type.label,
                                color = Tokens.bone
                            )
                            Text(
                                text = stringResource(
                                    R.string.record_summary_format,
                                    record.markedCount,
                                    formatMicroTesla(record.baselineMicroTesla),
                                    formatMicroTesla(record.thresholdMicroTesla)
                                ),
                                style = Type.labelSmall,
                                color = Tokens.boneMute
                            )
                        }
                    }
                }

                item { Spacer(Modifier.height(Space.Xs)) }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Space.Screen)
                    .padding(bottom = Space.Control),
                verticalArrangement = Arrangement.spacedBy(Space.Control)
            ) {
                HardwareButton(
                    label = stringResource(R.string.record_share),
                    onClick = { shareRecord(context, record) },
                    dominant = true,
                    modifier = Modifier.fillMaxWidth()
                )
                ControlRow {
                    HardwareButton(
                        label = stringResource(R.string.record_new_session),
                        onClick = onNewSession,
                        modifier = Modifier.weight(1f)
                    )
                    HardwareButton(
                        label = stringResource(R.string.record_open_archive),
                        onClick = onOpenArchive,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = Tokens.caseEdge,
            title = {
                Text(
                    text = stringResource(R.string.record_delete_title_format, recordTitle),
                    style = Type.labelLarge,
                    color = Tokens.bone
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.record_delete_body),
                    style = Type.body,
                    color = Tokens.boneMute
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete()
                }) {
                    Text(
                        text = stringResource(R.string.record_delete_confirm),
                        style = Type.label,
                        color = Tokens.signalText
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text(
                        text = stringResource(R.string.record_delete_cancel),
                        style = Type.label,
                        color = Tokens.phosphor
                    )
                }
            }
        )
    }
}

/** Shown if a record id can no longer be found. */
@Composable
fun MissingRecordScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    InstrumentScreen(modifier = modifier) {
        EmptyInstrumentState(
            title = stringResource(R.string.record_missing_title),
            body = stringResource(R.string.record_missing_body),
            modifier = Modifier.weight(1f)
        )
        HardwareButton(
            label = stringResource(R.string.record_back),
            onClick = onBack,
            dominant = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = Space.Control)
        )
    }
}

private fun shareRecord(context: Context, record: SessionRecord) {
    val title = context.getString(R.string.record_number_format, record.number)
    val builder = StringBuilder()
    builder.appendLine(context.getString(R.string.app_name) + " — " + context.getString(R.string.unit_name))
    builder.appendLine(title)
    builder.appendLine(formatRecordDate(record.startedAtEpochMillis))
    builder.appendLine(
        context.getString(R.string.record_duration_format, formatClock(record.durationMillis))
    )
    builder.appendLine()
    builder.appendLine(
        "${context.getString(R.string.record_responses)} ${record.responseCount} · " +
            "${context.getString(R.string.record_contacts)} ${record.contactCount}"
    )
    builder.appendLine(
        "${context.getString(R.string.record_peak_emf)} " +
            "${formatMicroTesla(record.peakMicroTesla)} ${context.getString(R.string.unit_microtesla)}"
    )
    record.strongestInterval()?.let {
        builder.appendLine("${context.getString(R.string.record_strongest)} $it")
    }
    builder.appendLine()
    builder.appendLine(context.getString(R.string.record_timeline))
    record.timeline.forEach { event ->
        builder.appendLine(
            "${formatClock(event.atMillis)}  ${event.describe(context)}" +
                if (event.marked) "  ★" else ""
        )
    }
    record.recurringThemes()?.let {
        builder.appendLine()
        builder.appendLine(context.getString(R.string.record_recurring_format, it))
    }
    builder.appendLine()
    builder.appendLine(context.getString(R.string.record_share_footer))
    builder.appendLine(context.getString(R.string.disclosure_footer))

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(
            Intent.EXTRA_SUBJECT,
            context.getString(R.string.record_share_subject_format, record.number)
        )
        putExtra(Intent.EXTRA_TEXT, builder.toString())
    }
    context.startActivity(
        Intent.createChooser(intent, context.getString(R.string.record_share_chooser))
    )
}
