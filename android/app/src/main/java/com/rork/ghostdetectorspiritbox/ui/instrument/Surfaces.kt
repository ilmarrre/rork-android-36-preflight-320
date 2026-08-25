package com.rork.ghostdetectorspiritbox.ui.instrument

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rork.ghostdetectorspiritbox.config.Radius
import com.rork.ghostdetectorspiritbox.config.Space
import com.rork.ghostdetectorspiritbox.config.Tokens

/**
 * The enclosure every screen is built on: matte anthracite with a fine brushed grain,
 * standard horizontal padding and, when the screen owns its insets, safe-area padding.
 */
@Composable
fun InstrumentScreen(
    modifier: Modifier = Modifier,
    applySafeArea: Boolean = true,
    horizontalPadding: Dp = Space.Screen,
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(Space.Control),
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Tokens.case)
            .drawBehind { drawBrushedGrain() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(if (applySafeArea) Modifier.safeDrawingPadding() else Modifier)
                .padding(horizontal = horizontalPadding),
            verticalArrangement = verticalArrangement,
            content = content
        )
    }
}

/** Enclosure without the column, for screens that own their own scaffolding. */
@Composable
fun InstrumentSurface(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Tokens.case)
            .drawBehind { drawBrushedGrain() },
        content = content
    )
}

private fun DrawScope.drawBrushedGrain() {
    var y = 0f
    val step = 3.dp.toPx()
    while (y < size.height) {
        drawLine(
            color = Tokens.caseEdge.copy(alpha = 0.35f),
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 1f
        )
        y += step
    }
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Tokens.ink.copy(alpha = 0.55f), Tokens.ink.copy(alpha = 0f)),
            endY = size.height * 0.3f
        )
    )
}

/** A raised, engraved panel. Cards use the 12dp radius. */
@Composable
fun InstrumentPanel(
    modifier: Modifier = Modifier,
    shape: Shape = Radius.Card,
    screws: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(Tokens.caseEdge)
            .border(Space.Hairline, Tokens.caseLift.copy(alpha = 0.55f), shape)
            .drawBehind {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Tokens.caseLift.copy(alpha = 0.30f),
                            Tokens.caseEdge.copy(alpha = 0f),
                            Tokens.ink.copy(alpha = 0.35f)
                        )
                    )
                )
                if (screws) drawScrews()
            },
        content = content
    )
}

private fun DrawScope.drawScrews() {
    val inset = 10.dp.toPx()
    val radius = 2.4.dp.toPx()
    listOf(
        Offset(inset, inset),
        Offset(size.width - inset, inset),
        Offset(inset, size.height - inset),
        Offset(size.width - inset, size.height - inset)
    ).forEach { center ->
        drawCircle(color = Tokens.ink, radius = radius, center = center)
        drawCircle(
            color = Tokens.caseLift.copy(alpha = 0.7f),
            radius = radius,
            center = center.copy(y = center.y - 0.6.dp.toPx()),
            style = Stroke(width = 0.8.dp.toPx())
        )
    }
}

/**
 * A recessed phosphor display. Scanlines, bloom and vignette exist only inside this
 * component, never over casing text, and the display corner radius is 0.
 */
@Composable
fun PhosphorDisplay(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(Space.Control),
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(Radius.Card)
            .background(Tokens.caseEdge)
            .border(Space.Hairline, Tokens.caseLift.copy(alpha = 0.5f), Radius.Card)
            .padding(Space.Sm)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(Radius.Display)
                .background(Tokens.ink)
                .border(Space.Hairline, Tokens.ink, Radius.Display)
                .drawWithContent {
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Tokens.phosphorDim.copy(alpha = 0.22f),
                                Tokens.ink.copy(alpha = 0f)
                            ),
                            center = Offset(size.width / 2f, size.height / 2f),
                            radius = size.maxDimension * 0.75f
                        )
                    )
                    drawContent()
                    var y = 0f
                    val step = 3.5.dp.toPx()
                    while (y < size.height) {
                        drawLine(
                            color = Tokens.ink.copy(alpha = 0.30f),
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1.2f
                        )
                        y += step
                    }
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Tokens.ink.copy(alpha = 0f),
                                Tokens.ink.copy(alpha = 0.55f)
                            ),
                            center = Offset(size.width / 2f, size.height / 2f),
                            radius = size.maxDimension * 0.62f
                        )
                    )
                }
                .padding(contentPadding),
            content = content
        )
    }
}

/** Hairline rule between rows. Decorative, so it is hidden from accessibility. */
@Composable
fun InstrumentDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(Space.Hairline)
            .background(Tokens.caseLift.copy(alpha = 0.5f))
            .clearAndSetSemantics { }
    )
}

/** Vertical hairline between metric cells. */
@Composable
fun CellDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(Space.Xl)
            .width(Space.Hairline)
            .background(Tokens.caseLift.copy(alpha = 0.5f))
            .clearAndSetSemantics { }
    )
}
