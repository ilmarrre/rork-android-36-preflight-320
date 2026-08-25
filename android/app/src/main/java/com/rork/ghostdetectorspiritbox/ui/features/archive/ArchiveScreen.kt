package com.rork.ghostdetectorspiritbox.ui.features.archive

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.rork.ghostdetectorspiritbox.R
import com.rork.ghostdetectorspiritbox.config.Space
import com.rork.ghostdetectorspiritbox.config.Tokens
import com.rork.ghostdetectorspiritbox.domain.SessionRecord
import com.rork.ghostdetectorspiritbox.domain.contactCount
import com.rork.ghostdetectorspiritbox.domain.formatClock
import com.rork.ghostdetectorspiritbox.domain.formatMicroTesla
import com.rork.ghostdetectorspiritbox.domain.responseCount
import com.rork.ghostdetectorspiritbox.ui.instrument.CellDivider
import com.rork.ghostdetectorspiritbox.ui.instrument.EmptyInstrumentState
import com.rork.ghostdetectorspiritbox.ui.instrument.InstrumentPanel
import com.rork.ghostdetectorspiritbox.ui.instrument.InstrumentScreen
import com.rork.ghostdetectorspiritbox.ui.instrument.MetricCell
import com.rork.ghostdetectorspiritbox.ui.instrument.TopStatusBar
import com.rork.ghostdetectorspiritbox.ui.text.formatRecordDate
import com.rork.ghostdetectorspiritbox.ui.theme.Type

/** Local archive of saved evidence records. */
@Composable
fun ArchiveScreen(
    records: List<SessionRecord>,
    capacity: Int,
    onOpen: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    InstrumentScreen(modifier = modifier, applySafeArea = false) {
        TopStatusBar {
            Text(
                text = stringResource(R.string.archive_title),
                style = Type.heading,
                color = Tokens.bone,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = stringResource(
                    R.string.archive_capacity_format,
                    records.size,
                    capacity
                ),
                style = Type.readout,
                color = if (records.size >= capacity) Tokens.signalText else Tokens.phosphor
            )
        }

        if (records.isEmpty()) {
            EmptyInstrumentState(
                title = stringResource(R.string.archive_empty_title),
                body = stringResource(R.string.archive_empty_body),
                modifier = Modifier.weight(1f)
            )
            return@InstrumentScreen
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(Space.Control),
            contentPadding = PaddingValues(bottom = Space.Md)
        ) {
            items(items = records, key = { it.id }) { record ->
                RecordRow(record = record, onOpen = { onOpen(record.id) })
            }
        }
    }
}

@Composable
private fun RecordRow(record: SessionRecord, onOpen: () -> Unit) {
    InstrumentPanel(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onOpen),
        screws = true
    ) {
        Column(modifier = Modifier.padding(vertical = Space.Control)) {
            Column(modifier = Modifier.padding(horizontal = Space.Md)) {
                Text(
                    text = stringResource(R.string.record_number_format, record.number),
                    style = Type.labelLarge,
                    color = Tokens.bone
                )
                Text(
                    text = formatRecordDate(record.startedAtEpochMillis),
                    style = Type.monoSmall,
                    color = Tokens.boneMute
                )
            }
            Spacer(Modifier.padding(top = Space.Xs))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Space.Xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MetricCell(
                    label = stringResource(R.string.record_duration),
                    value = formatClock(record.durationMillis),
                    modifier = Modifier.weight(1f)
                )
                CellDivider()
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
            }
        }
    }
}
