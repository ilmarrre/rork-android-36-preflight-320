package com.rork.ghostdetectorspiritbox.config

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Spacing, sizing and radius scale. Everything sits on an 8dp grid. */
object Space {
    val Hairline: Dp = 1.dp
    val Xs: Dp = 4.dp
    val Sm: Dp = 8.dp
    /** Standard gap between controls. */
    val Control: Dp = 12.dp
    val Md: Dp = 16.dp
    /** Standard horizontal screen padding. */
    val Screen: Dp = 20.dp
    val Lg: Dp = 24.dp
    val Xl: Dp = 32.dp
}

/** Corner radii: displays are square, controls are barely softened, cards are rounded. */
object Radius {
    val Display: Shape = RoundedCornerShape(0.dp)
    val Control: Shape = RoundedCornerShape(4.dp)
    val Card: Shape = RoundedCornerShape(12.dp)
}

/** Physical control sizing. */
object Sizes {
    /** Minimum touch target in every direction. */
    val Touch: Dp = 56.dp
    /** Indicator lamp. */
    val Lamp: Dp = 12.dp
    /** Largest permitted light fill, per the visual contract. */
    val MaxLightFill: Dp = 40.dp
}

/**
 * Motion contract: 180ms in, 240ms out, restrained springs only. Anything decorative
 * must collapse to zero duration when Reduced Motion is on.
 */
object Motion {
    const val ENTER_MILLIS: Int = 180
    const val EXIT_MILLIS: Int = 240

    fun <T> enter(reducedMotion: Boolean): FiniteAnimationSpec<T> =
        tween(if (reducedMotion) 0 else ENTER_MILLIS)

    fun <T> exit(reducedMotion: Boolean): FiniteAnimationSpec<T> =
        tween(if (reducedMotion) 0 else EXIT_MILLIS)

    /** The only spring in the product: critically damped, no overshoot. */
    fun <T> settle(): FiniteAnimationSpec<T> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow
    )
}
