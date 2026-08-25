package com.rork.ghostdetectorspiritbox.config

import androidx.compose.ui.graphics.Color

/**
 * The instrument palette. These are the only colours in the product.
 *
 * The first block is the approved token set and must not be edited. The derived block
 * below contains values computed from those tokens for states the base set does not
 * name (pressed key faces, the stop border at rest, readable secondary phosphor text).
 */
object Tokens {

    // --- Approved tokens -----------------------------------------------------------

    /** Deepest recess: display windows, scrim base. */
    val ink: Color = Color(0xFF0B0B0D)

    /** Matte anthracite enclosure — the screen background. */
    val case: Color = Color(0xFF16171A)

    /** Raised panels, key faces, engraved edges. */
    val caseEdge: Color = Color(0xFF24262B)

    /** Live amber phosphor. */
    val phosphor: Color = Color(0xFFF2A93B)

    /** Inactive segments, graticules and history. Decorative only — never body text. */
    val phosphorDim: Color = Color(0xFF4A3616)

    /** Signal red: warnings, threshold states, hold progress, destructive confirmation. */
    val signal: Color = Color(0xFFD8443C)

    /** Signal red for text and icons on dark surfaces. */
    val signalText: Color = Color(0xFFE05A50)

    /** Stable silkscreen text. */
    val bone: Color = Color(0xFFDCD7CC)

    /** Secondary text. */
    val boneMute: Color = Color(0xFF8B857B)

    /** Disabled text and faint rules. */
    val boneFaint: Color = Color(0xFF6E6A63)

    // --- Derived -------------------------------------------------------------------

    /**
     * Readable secondary phosphor for display text. [phosphorDim] is reserved for
     * decorative inactive segments; at 13sp on [ink] it would fall under a 2:1 contrast
     * ratio, so display sub-labels use this instead.
     */
    val phosphorMuted: Color = Color(0xFFA9741F)

    /** Pressed and selected key face, one step up from [caseEdge]. */
    val caseLift: Color = Color(0xFF33363C)

    /** Stop control border at rest — signal red held back until the hold begins. */
    val signalDeep: Color = Color(0xFF5E211D)

    /** Unlit indicator lamp. */
    val lampOff: Color = Color(0xFF3A3D42)
}
