package com.rork.ghostdetectorspiritbox.ui.instrument

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.rork.ghostdetectorspiritbox.config.Tokens
import com.rork.ghostdetectorspiritbox.domain.BlipState
import com.rork.ghostdetectorspiritbox.domain.RadarBlip
import com.rork.ghostdetectorspiritbox.ui.theme.LocalReducedMotion
import kotlin.math.cos
import kotlin.math.sin

/**
 * The signal model. It is perfectly still until a session runs, shows no distances,
 * ranges or bearings, and is decorative to accessibility services — the same events are
 * announced as text in the log.
 */
@Composable
fun RadarDisplay(
    active: Boolean,
    blips: List<RadarBlip>,
    modifier: Modifier = Modifier
) {
    val reducedMotion = LocalReducedMotion.current
    val transition = rememberInfiniteTransition(label = "radar")
    val sweep by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(4800, easing = LinearEasing)),
        label = "sweep"
    )
    val sweepAngle = if (reducedMotion) 0f else sweep

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .clearAndSetSemantics { }
    ) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = minOf(size.width, size.height) / 2f * 0.92f
        drawGraticule(center, radius)
        if (active) {
            if (!reducedMotion) drawSweep(center, radius, sweepAngle)
            blips.forEach { blip -> drawBlip(center, radius, blip, sweepAngle, reducedMotion) }
        }
    }
}

/** Static idle graticule, guaranteed to render nothing that moves. */
@Composable
fun StaticRadarGraticule(modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .clearAndSetSemantics { }
    ) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = minOf(size.width, size.height) / 2f * 0.92f
        drawGraticule(center, radius)
    }
}

private fun DrawScope.drawGraticule(center: Offset, radius: Float) {
    val ring = Tokens.phosphorDim
    listOf(0.34f, 0.67f, 1f).forEach { fraction ->
        drawCircle(
            color = ring.copy(alpha = if (fraction == 1f) 1f else 0.7f),
            radius = radius * fraction,
            center = center,
            style = Stroke(width = 1.dp.toPx())
        )
    }
    drawLine(
        color = ring.copy(alpha = 0.85f),
        start = Offset(center.x - radius, center.y),
        end = Offset(center.x + radius, center.y),
        strokeWidth = 1.dp.toPx()
    )
    drawLine(
        color = ring.copy(alpha = 0.85f),
        start = Offset(center.x, center.y - radius),
        end = Offset(center.x, center.y + radius),
        strokeWidth = 1.dp.toPx()
    )
    val tick = 5.dp.toPx()
    for (index in 1..23) {
        val offset = radius * (index / 24f)
        drawLine(
            color = ring.copy(alpha = 0.6f),
            start = Offset(center.x, center.y - offset),
            end = Offset(center.x + tick * 0.55f, center.y - offset),
            strokeWidth = 1f
        )
        drawLine(
            color = ring.copy(alpha = 0.6f),
            start = Offset(center.x - offset, center.y),
            end = Offset(center.x - offset, center.y + tick * 0.55f),
            strokeWidth = 1f
        )
    }
}

private fun DrawScope.drawSweep(center: Offset, radius: Float, sweepDeg: Float) {
    val segments = 42
    for (index in 0 until segments) {
        val angle = sweepDeg - index * 2.6f
        val alpha = (1f - index / segments.toFloat()) * 0.5f
        val radians = Math.toRadians(angle.toDouble() - 90.0)
        drawLine(
            color = Tokens.phosphor.copy(alpha = alpha * 0.5f),
            start = center,
            end = Offset(
                center.x + (cos(radians) * radius).toFloat(),
                center.y + (sin(radians) * radius).toFloat()
            ),
            strokeWidth = 2.6.dp.toPx()
        )
    }
    val leading = Math.toRadians(sweepDeg.toDouble() - 90.0)
    drawLine(
        color = Tokens.phosphor.copy(alpha = 0.9f),
        start = center,
        end = Offset(
            center.x + (cos(leading) * radius).toFloat(),
            center.y + (sin(leading) * radius).toFloat()
        ),
        strokeWidth = 1.6.dp.toPx()
    )
}

private fun DrawScope.drawBlip(
    center: Offset,
    radius: Float,
    blip: RadarBlip,
    sweepDeg: Float,
    reducedMotion: Boolean
) {
    val radians = Math.toRadians(blip.angleDeg.toDouble() - 90.0)
    val distance = radius * blip.radiusFraction
    val position = Offset(
        center.x + (cos(radians) * distance).toFloat(),
        center.y + (sin(radians) * distance).toFloat()
    )
    val freshness = if (reducedMotion) {
        1f
    } else {
        (1f - ((sweepDeg - blip.angleDeg + 360f) % 360f) / 360f).coerceIn(0f, 1f)
    }
    val alpha = (0.25f + freshness * 0.75f) * blip.strength
    val dotRadius = if (blip.state == BlipState.CONTACT) 4.5.dp.toPx() else 3.dp.toPx()

    drawCircle(
        color = Tokens.phosphor.copy(alpha = alpha * 0.20f),
        radius = dotRadius * 3.2f,
        center = position
    )
    drawCircle(color = Tokens.phosphor.copy(alpha = alpha), radius = dotRadius, center = position)
    if (blip.state == BlipState.CONTACT) {
        drawCircle(
            color = Tokens.phosphor.copy(alpha = alpha * 0.7f),
            radius = dotRadius * 2.4f,
            center = position,
            style = Stroke(width = 1.dp.toPx())
        )
    }
}
