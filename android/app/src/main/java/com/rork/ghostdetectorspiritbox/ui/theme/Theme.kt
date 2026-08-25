package com.rork.ghostdetectorspiritbox.ui.theme

import android.app.Activity
import android.provider.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.rork.ghostdetectorspiritbox.config.Tokens

/**
 * True when the system animation scales are switched off. Every decorative movement
 * must consult this; hold-to-stop progress is exempt because it is required feedback
 * for a destructive action.
 */
val LocalReducedMotion = staticCompositionLocalOf { false }

private val InstrumentColorScheme = darkColorScheme(
    primary = Tokens.phosphor,
    onPrimary = Tokens.ink,
    secondary = Tokens.phosphorMuted,
    onSecondary = Tokens.bone,
    background = Tokens.case,
    onBackground = Tokens.bone,
    surface = Tokens.caseEdge,
    onSurface = Tokens.bone,
    surfaceVariant = Tokens.caseEdge,
    onSurfaceVariant = Tokens.boneMute,
    surfaceContainer = Tokens.caseEdge,
    surfaceContainerHigh = Tokens.caseLift,
    surfaceContainerLow = Tokens.case,
    outline = Tokens.caseLift,
    outlineVariant = Tokens.ink,
    error = Tokens.signal,
    onError = Tokens.bone,
    scrim = Tokens.ink
)

/** One dark instrument: no light theme, no dynamic colour, no white anywhere. */
@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    val context = LocalContext.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = false
            controller.isAppearanceLightNavigationBars = false
        }
    }

    val reducedMotion = remember(context) {
        runCatching {
            val resolver = context.contentResolver
            val animator = Settings.Global.getFloat(
                resolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            )
            val transition = Settings.Global.getFloat(
                resolver,
                Settings.Global.TRANSITION_ANIMATION_SCALE,
                1f
            )
            animator == 0f || transition == 0f
        }.getOrDefault(false)
    }

    CompositionLocalProvider(LocalReducedMotion provides reducedMotion) {
        MaterialTheme(
            colorScheme = InstrumentColorScheme,
            typography = AppTypography,
            content = content
        )
    }
}
