package com.rork.ghostdetectorspiritbox.ui.instrument

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.rork.ghostdetectorspiritbox.config.Limits
import com.rork.ghostdetectorspiritbox.config.Radius
import com.rork.ghostdetectorspiritbox.config.Sizes
import com.rork.ghostdetectorspiritbox.config.Space
import com.rork.ghostdetectorspiritbox.config.Tokens
import com.rork.ghostdetectorspiritbox.ui.theme.Type
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * A physical engraved key. [dominant] adds the thin amber outline reserved for the one
 * primary action on a screen. Always at least a 56dp target in both directions.
 */
@Composable
fun HardwareButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    dominant: Boolean = false,
    enabled: Boolean = true,
    caption: String? = null,
    accessibilityLabel: String = label,
    labelColor: Color = Tokens.bone
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val active = pressed && enabled
    val borderColor = when {
        !enabled -> Tokens.caseLift.copy(alpha = 0.35f)
        dominant -> Tokens.phosphor.copy(alpha = if (pressed) 1f else 0.85f)
        else -> Tokens.caseLift.copy(alpha = 0.7f)
    }

    Box(
        modifier = modifier
            .defaultMinSize(minWidth = Sizes.Touch, minHeight = Sizes.Touch)
            .clip(Radius.Control)
            .background(if (active) Tokens.caseLift else Tokens.caseEdge)
            .border(Space.Hairline, borderColor, Radius.Control)
            .drawBehind {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Tokens.caseLift.copy(alpha = if (active) 0f else 0.35f),
                            Tokens.caseEdge.copy(alpha = 0f),
                            Tokens.ink.copy(alpha = if (active) 0f else 0.30f)
                        )
                    )
                )
                if (dominant && enabled) {
                    drawRect(color = Tokens.phosphor.copy(alpha = if (pressed) 0.10f else 0.05f))
                }
            }
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick
            )
            .semantics {
                contentDescription = accessibilityLabel
                if (!enabled) disabled()
            }
            .padding(horizontal = Space.Control, vertical = Space.Sm),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = Type.keyLabel,
                color = if (enabled) labelColor else Tokens.boneFaint
            )
            if (caption != null) {
                Text(
                    text = caption,
                    style = Type.labelSmall,
                    color = if (enabled) Tokens.boneMute else Tokens.boneFaint
                )
            }
        }
    }
}

/**
 * The restrained hold variant of [HardwareButton]: a dark key with a signal-red border
 * at rest. Red only fills the face while the hold is in progress, then briefly confirms.
 * Hold progress is intentionally exempt from Reduced Motion — it is the feedback that
 * makes a destructive action safe.
 */
@Composable
fun HardwareHoldButton(
    label: String,
    holdCaption: String,
    completingCaption: String,
    accessibilityLabel: String,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
    holdMillis: Int = Limits.HOLD_STOP_MILLIS
) {
    val progress = remember { Animatable(0f) }
    var completed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val currentOnComplete by rememberUpdatedState(onComplete)

    Box(
        modifier = modifier
            .defaultMinSize(minWidth = Sizes.Touch, minHeight = Sizes.Touch)
            .clip(Radius.Control)
            .background(if (completed) Tokens.signal else Tokens.caseEdge)
            .border(
                Space.Hairline,
                if (completed) Tokens.signal else Tokens.signalDeep,
                Radius.Control
            )
            .drawBehind {
                if (progress.value > 0f && !completed) {
                    drawRect(
                        color = Tokens.signal.copy(alpha = 0.85f),
                        size = Size(size.width * progress.value, size.height)
                    )
                }
            }
            .semantics {
                role = Role.Button
                contentDescription = accessibilityLabel
                stateDescription = if (completed) completingCaption else holdCaption
            }
            .pointerInput(holdMillis) {
                detectTapGestures(
                    onPress = {
                        val job = scope.launch {
                            progress.snapTo(0f)
                            progress.animateTo(1f, tween(holdMillis, easing = LinearEasing))
                            completed = true
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            delay(200)
                            currentOnComplete()
                        }
                        tryAwaitRelease()
                        if (!completed) {
                            job.cancel()
                            scope.launch { progress.animateTo(0f, tween(160)) }
                        }
                    }
                )
            }
            .padding(horizontal = Space.Control, vertical = Space.Sm),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = Type.keyLabel,
                color = if (completed || progress.value > 0.5f) Tokens.bone else Tokens.signalText
            )
            Text(
                text = if (completed) completingCaption else holdCaption,
                style = Type.labelSmall,
                color = if (completed) Tokens.bone else Tokens.boneMute
            )
        }
    }
}

/** Segmented mode selector: the active segment carries bone text and an amber underline. */
@Composable
fun <T> ModeSelector(
    options: List<ModeOption<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.heightIn(min = Sizes.Touch),
        horizontalArrangement = Arrangement.spacedBy(Space.Sm)
    ) {
        options.forEach { option ->
            val active = option.value == selected
            val interaction = remember { MutableInteractionSource() }
            val pressed by interaction.collectIsPressedAsState()
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = Sizes.Touch)
                    .clip(Radius.Control)
                    .background(if (active || pressed) Tokens.caseEdge else Tokens.caseEdge.copy(alpha = 0.6f))
                    .border(
                        Space.Hairline,
                        if (active) Tokens.caseLift else Tokens.caseLift.copy(alpha = 0.35f),
                        Radius.Control
                    )
                    .clickable(
                        interactionSource = interaction,
                        indication = null,
                        role = Role.Tab,
                        onClick = { onSelect(option.value) }
                    )
                    .semantics {
                        this.contentDescription = option.accessibilityLabel
                        this.selected = active
                    }
                    .drawBehind {
                        if (active) {
                            drawLine(
                                color = Tokens.phosphor,
                                start = Offset(Space.Control.toPx(), size.height - 1.dp.toPx()),
                                end = Offset(size.width - Space.Control.toPx(), size.height - 1.dp.toPx()),
                                strokeWidth = 2.dp.toPx()
                            )
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = option.label,
                    style = Type.keyLabel,
                    color = if (active) Tokens.bone else Tokens.boneMute
                )
            }
        }
    }
}

/** One selectable mode, with the spoken label used by accessibility services. */
data class ModeOption<T>(
    val value: T,
    val label: String,
    val accessibilityLabel: String = label
)

/** Row of keys sharing the width, spaced on the control grid. */
@Composable
fun ControlRow(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Space.Control),
        content = content
    )
}
