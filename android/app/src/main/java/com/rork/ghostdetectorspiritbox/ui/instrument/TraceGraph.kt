package com.rork.ghostdetectorspiritbox.ui.instrument

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.rork.ghostdetectorspiritbox.config.Limits
import com.rork.ghostdetectorspiritbox.config.Tokens
import kotlin.math.max

/**
 * Rolling 60-second magnetometer trace. Only real samples are plotted; an empty buffer
 * draws an empty graticule rather than an invented curve. The graticule is decorative,
 * so the whole canvas carries one summary description instead.
 */
@Composable
fun TraceGraph(
    samples: List<Float>,
    threshold: Float?,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .then(
                if (contentDescription != null) {
                    Modifier.semantics { this.contentDescription = contentDescription }
                } else {
                    Modifier.clearAndSetSemantics { }
                }
            )
    ) {
        val width = size.width
        val height = size.height
        val maxValue = max(samples.maxOrNull() ?: 0f, threshold ?: 0f)
            .let { if (it <= 0f) 100f else it * 1.25f }

        for (index in 0..3) {
            val y = height * index / 3f
            drawLine(
                color = Tokens.phosphorDim.copy(alpha = 0.7f),
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1f
            )
        }
        for (index in 0..4) {
            val x = width * index / 4f
            drawLine(
                color = Tokens.phosphorDim.copy(alpha = 0.55f),
                start = Offset(x, 0f),
                end = Offset(x, height),
                strokeWidth = 1f
            )
        }

        threshold?.let { value ->
            val y = height - (value / maxValue) * height
            drawLine(
                color = Tokens.signal.copy(alpha = 0.75f),
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1.2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(9f, 9f))
            )
        }

        if (samples.size < 2) return@Canvas

        val capacity = Limits.HISTORY_POINTS
        val stepX = width / (capacity - 1).toFloat()
        val startIndex = capacity - samples.size
        val line = Path()
        val fill = Path()
        samples.forEachIndexed { index, value ->
            val x = (startIndex + index) * stepX
            val y = height - (value / maxValue) * height
            if (index == 0) {
                line.moveTo(x, y)
                fill.moveTo(x, height)
                fill.lineTo(x, y)
            } else {
                line.lineTo(x, y)
                fill.lineTo(x, y)
            }
        }
        fill.lineTo((startIndex + samples.size - 1) * stepX, height)
        fill.close()

        drawPath(
            path = fill,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Tokens.phosphor.copy(alpha = 0.20f),
                    Tokens.phosphor.copy(alpha = 0f)
                )
            )
        )
        drawPath(path = line, color = Tokens.phosphor, style = Stroke(width = 1.8.dp.toPx()))

        val lastValue = samples.last()
        val lastX = (startIndex + samples.size - 1) * stepX
        val lastY = height - (lastValue / maxValue) * height
        val above = threshold != null && lastValue >= threshold
        val head = if (above) Tokens.signal else Tokens.phosphor
        drawCircle(color = head, radius = 3.dp.toPx(), center = Offset(lastX, lastY))
        drawCircle(
            color = head.copy(alpha = 0.25f),
            radius = 7.dp.toPx(),
            center = Offset(lastX, lastY)
        )
    }
}
