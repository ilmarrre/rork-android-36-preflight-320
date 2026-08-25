package com.rork.ghostdetectorspiritbox.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.rork.ghostdetectorspiritbox.R

/**
 * Equipment labels and headings.
 *
 * Archivo ships as a variable font with a width axis; the SemiExpanded 600 instance is
 * requested through variation settings (applied on API 26+, which covers the supported
 * device range apart from API 24–25, where the default instance is used).
 */
@OptIn(ExperimentalTextApi::class)
val ArchivoFamily: FontFamily = FontFamily(
    Font(
        resId = R.font.archivo_variable,
        weight = FontWeight.SemiBold,
        style = FontStyle.Normal,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(600),
            FontVariation.width(112.5f)
        )
    )
)

/** Values, timers, words and timeline entries. */
val MonoFamily: FontFamily = FontFamily(
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
    Font(R.font.jetbrains_mono_bold, FontWeight.Bold)
)

/** Onboarding, settings and explanations. */
@OptIn(ExperimentalTextApi::class)
val ProseFamily: FontFamily = FontFamily(
    Font(
        resId = R.font.inter_variable,
        weight = FontWeight.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(400))
    ),
    Font(
        resId = R.font.inter_variable,
        weight = FontWeight.Medium,
        variationSettings = FontVariation.Settings(FontVariation.weight(500))
    )
)

/**
 * The complete type scale. Only 11, 13, 15, 20, 28, 44 and 64 exist; nothing in the
 * product may introduce another size.
 */
object Type {

    /** Smallest engraved legend — metric captions, lamp labels. */
    val labelSmall: TextStyle = TextStyle(
        fontFamily = ArchivoFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 1.2.sp
    )

    /** Standard equipment label. */
    val label: TextStyle = TextStyle(
        fontFamily = ArchivoFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 20.sp,
        letterSpacing = 1.6.sp
    )

    /** Panel header. */
    val labelLarge: TextStyle = TextStyle(
        fontFamily = ArchivoFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 2.sp
    )

    /** Screen heading. */
    val heading: TextStyle = TextStyle(
        fontFamily = ArchivoFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = 1.6.sp
    )

    /** Key face label. */
    val keyLabel: TextStyle = TextStyle(
        fontFamily = ArchivoFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 2.sp,
        textAlign = TextAlign.Center
    )

    /** Timestamps and dense mono captions. */
    val monoSmall: TextStyle = TextStyle(
        fontFamily = MonoFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.4.sp
    )

    /** Timeline and log rows. */
    val mono: TextStyle = TextStyle(
        fontFamily = MonoFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.4.sp
    )

    /** Tabular value or timer. */
    val readout: TextStyle = TextStyle(
        fontFamily = MonoFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.8.sp
    )

    /** Emphasised value inside a display window. */
    val readoutLarge: TextStyle = TextStyle(
        fontFamily = MonoFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 32.sp,
        letterSpacing = 1.sp
    )

    /** The Spirit Box hero word. */
    val hero: TextStyle = TextStyle(
        fontFamily = MonoFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 44.sp,
        lineHeight = 52.sp,
        letterSpacing = 2.sp,
        textAlign = TextAlign.Center
    )

    /** The single largest readout: live field magnitude. */
    val display: TextStyle = TextStyle(
        fontFamily = MonoFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 64.sp,
        lineHeight = 72.sp,
        letterSpacing = 1.sp
    )

    /** Explanatory prose. */
    val body: TextStyle = TextStyle(
        fontFamily = ProseFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 24.sp
    )

    /** Secondary prose. */
    val bodySmall: TextStyle = TextStyle(
        fontFamily = ProseFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 20.sp
    )

    /** Emphasised prose. */
    val bodyStrong: TextStyle = TextStyle(
        fontFamily = ProseFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 24.sp
    )
}

val AppTypography: Typography = Typography(
    bodyLarge = Type.body,
    bodyMedium = Type.bodySmall,
    labelLarge = Type.keyLabel,
    labelMedium = Type.label,
    labelSmall = Type.labelSmall,
    titleMedium = Type.labelLarge,
    titleLarge = Type.heading
)
