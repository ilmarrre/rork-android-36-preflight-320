package com.rork.ghostdetectorspiritbox.ui.instrument

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rork.ghostdetectorspiritbox.R
import com.rork.ghostdetectorspiritbox.config.Sizes
import com.rork.ghostdetectorspiritbox.config.Space
import com.rork.ghostdetectorspiritbox.config.Tokens
import com.rork.ghostdetectorspiritbox.ui.theme.LocalReducedMotion
import com.rork.ghostdetectorspiritbox.ui.theme.Type

/**
 * Indicator lamp. It pulses only while a session is logging, and never when Reduced
 * Motion is on. Purely decorative: the meaning lives in the label beside it.
 */
@Composable
fun StatusLamp(
    lit: Boolean,
    modifier: Modifier = Modifier,
    pulsing: Boolean = false,
    size: Dp = Sizes.Lamp
) {
    val reducedMotion = LocalReducedMotion.current
    val transition = rememberInfiniteTransition(label = "lamp")
    val pulse by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100),
            repeatMode = RepeatMode.Reverse
        ),
        label = "lampPulse"
    )
    val intensity = if (pulsing && lit && !reducedMotion) pulse else 1f

    Canvas(
        modifier = modifier
            .size(size)
            .clearAndSetSemantics { }
    ) {
        val radius = this.size.minDimension / 2f
        if (lit) {
            drawCircle(color = Tokens.phosphor.copy(alpha = 0.20f * intensity), radius = radius * 1.9f)
        }
        drawCircle(
            color = if (lit) Tokens.phosphor.copy(alpha = intensity) else Tokens.lampOff,
            radius = radius
        )
        drawCircle(
            color = Tokens.ink.copy(alpha = 0.5f),
            radius = radius,
            style = Stroke(width = 1.dp.toPx())
        )
    }
}

/** Lamp plus label, announced as one thing. */
@Composable
fun StatusLabel(
    text: String,
    lit: Boolean,
    modifier: Modifier = Modifier,
    pulsing: Boolean = false,
    textColor: Color = Tokens.bone
) {
    val context = LocalContext.current
    val state = if (lit) {
        context.getString(R.string.a11y_state_active)
    } else {
        context.getString(R.string.a11y_state_inactive)
    }
    Row(
        modifier = modifier.semantics(mergeDescendants = true) { },
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatusLamp(lit = lit, pulsing = pulsing)
        Spacer(Modifier.width(Space.Sm))
        Text(
            text = text,
            style = Type.label,
            color = textColor,
            modifier = Modifier.semantics { contentDescription = "$text, $state" }
        )
    }
}

/** Header strip of a screen: title block on the left, live readouts and actions right. */
@Composable
fun TopStatusBar(
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = Space.Control, bottom = Space.Xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.Control),
        content = content
    )
}

/**
 * A lit value drawn over its own dim inactive segments, the way a real segment display
 * shows the unlit parts of every digit. The ghost layer is decorative and is hidden
 * from accessibility; only the live value is announced.
 */
@Composable
fun SegmentValue(
    value: String,
    modifier: Modifier = Modifier,
    placeholder: String = value.map { if (it.isDigit()) '8' else it }.joinToString(""),
    style: androidx.compose.ui.text.TextStyle = Type.readout,
    color: Color = Tokens.phosphor,
    contentDescription: String? = null
) {
    Box(modifier = modifier) {
        Text(
            text = placeholder,
            style = style,
            color = Tokens.phosphorDim,
            modifier = Modifier.clearAndSetSemantics { }
        )
        Text(
            text = value,
            style = style,
            color = color,
            modifier = if (contentDescription != null) {
                Modifier.semantics { this.contentDescription = contentDescription }
            } else {
                Modifier
            }
        )
    }
}

/** A silkscreened legend with a right-aligned value. */
@Composable
fun ReadoutRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = Tokens.phosphor,
    divider: Boolean = true
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) { }
                .padding(horizontal = Space.Md, vertical = Space.Control),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, style = Type.label, color = Tokens.bone)
            Text(text = value, style = Type.readout, color = valueColor)
        }
        if (divider) InstrumentDivider()
    }
}

/** Compact metric used in summary blocks. */
@Composable
fun MetricCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = Tokens.bone
) {
    Column(
        modifier = modifier
            .semantics(mergeDescendants = true) { }
            .padding(vertical = Space.Control, horizontal = Space.Sm),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Space.Xs)
    ) {
        Text(
            text = label,
            style = Type.labelSmall,
            color = Tokens.boneMute,
            textAlign = TextAlign.Center
        )
        Text(
            text = value,
            style = Type.readout,
            color = valueColor,
            textAlign = TextAlign.Center
        )
    }
}

/** Empty state for a panel or a whole screen. */
@Composable
fun EmptyInstrumentState(
    title: String,
    body: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(Space.Lg)
                .semantics(mergeDescendants = true) { },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Space.Sm)
        ) {
            Text(text = title, style = Type.labelLarge, color = Tokens.boneMute)
            Text(
                text = body,
                style = Type.body,
                color = Tokens.boneMute,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Inline warning or error. This is one of the only four places signal red is allowed,
 * and it renders only while the condition is actually true.
 */
@Composable
fun InlineInstrumentError(
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                drawRect(color = Tokens.signal.copy(alpha = 0.10f))
                drawRect(color = Tokens.signal, style = Stroke(width = 1.dp.toPx()))
            }
            .padding(horizontal = Space.Md, vertical = Space.Control),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = Type.label,
            color = Tokens.signalText,
            textAlign = TextAlign.Center
        )
    }
}
