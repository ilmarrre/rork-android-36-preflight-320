package com.rork.ghostdetectorspiritbox.ui.features

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.rork.ghostdetectorspiritbox.R
import com.rork.ghostdetectorspiritbox.config.Space
import com.rork.ghostdetectorspiritbox.config.Tokens
import com.rork.ghostdetectorspiritbox.ui.instrument.InstrumentSurface
import com.rork.ghostdetectorspiritbox.ui.theme.Type

/**
 * Shown while the archive loads from disk. It is drawn on the same anthracite casing as
 * every other screen, so there is never a white or light frame between launch and idle.
 */
@Composable
fun BootstrapScreen(modifier: Modifier = Modifier) {
    InstrumentSurface(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.unit_badge),
                style = Type.labelLarge,
                color = Tokens.boneMute
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = Space.Xl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Space.Sm)
            ) {
                Text(
                    text = stringResource(R.string.disclosure_footer),
                    style = Type.labelSmall,
                    color = Tokens.boneFaint,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
