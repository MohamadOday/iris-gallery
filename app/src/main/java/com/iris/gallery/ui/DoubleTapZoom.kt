package com.iris.gallery.ui

private const val STAGE_EPSILON = 0.015f

/**
 * Next scale for a double tap.
 *
 * Multi-stage zoom follows the Fit -> Fill -> 1:1 -> Fit cycle. The stages are
 * derived from the current viewport, so a photo never needs more than three
 * double taps to get back to Fit regardless of its aspect ratio or resolution:
 *
 * - If both sides already exceed the viewport, go back to Fit.
 * - If a side is still missing, go to Fill.
 * - At Fill, go to 1:1 when the source has pixels to spare, otherwise Fit.
 * - When Fit and Fill are the same (aspect matches the viewport), 1:1 takes
 *   the second slot, or the configured zoom factor when the source is not
 *   bigger than the screen.
 *
 * Without multi-stage zoom it is a plain Fit <-> configured scale toggle.
 */
internal fun nextDoubleTapScale(
    currentScale: Float,
    fillScale: Float,
    oneToOneScale: Float,
    configuredScale: Float,
    multiStage: Boolean,
): Float {
    val fit = 1f
    val current = currentScale.coerceAtLeast(0.01f)
    val fill = fillScale.takeIf { it.isFinite() && it > fit + STAGE_EPSILON }
    val oneToOne = oneToOneScale.takeIf { it.isFinite() && it > fit + STAGE_EPSILON }
    val configured = configuredScale.takeIf { it.isFinite() && it > fit + STAGE_EPSILON }

    if (!multiStage) {
        return if (current > fit + STAGE_EPSILON) fit else (configured ?: oneToOne ?: fit)
    }

    if (fill == null) {
        // Fit already covers both sides.
        if (current > fit + STAGE_EPSILON) return fit
        return oneToOne ?: configured ?: fit
    }

    // Past Fill both sides exceed the viewport, which always goes back to Fit.
    if (current > fill + STAGE_EPSILON) return fit
    if (current < fill - STAGE_EPSILON) return fill
    return if (oneToOne != null && oneToOne > fill + STAGE_EPSILON) oneToOne else fit
}
