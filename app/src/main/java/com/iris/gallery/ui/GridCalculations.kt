package com.iris.gallery.ui

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp

/**
 * Shared grid layout calculations to keep grid column breakpoints and sizes
 * 100% unified between the live gallery views (PhotoGrid, AlbumsGrid) and Settings previews.
 */
object GridCalculations {

    /**
     * Calculates the exact number of columns for an adaptive grid matching Compose's
     * [androidx.compose.foundation.lazy.grid.GridCells.Adaptive] cross-axis size calculation.
     */
    fun calculateColumns(
        availableWidthPx: Int,
        minCellSizePx: Int,
        spacingPx: Int
    ): Int {
        if (minCellSizePx + spacingPx <= 0) return 1
        return maxOf(1, (availableWidthPx + spacingPx) / (minCellSizePx + spacingPx))
    }

    /**
     * Calculates columns for the Photos grid matching [PhotoGrid] layout:
     * - contentPadding start/end: gridSpacing.dp
     * - horizontalArrangement: gridSpacing.dp
     */
    fun calculatePhotoColumns(
        screenWidthDp: Float,
        cellSizeDp: Float,
        gridSpacingDp: Float,
        density: Density,
        horizontalPaddingDp: Float = 0f
    ): Int = with(density) {
        val totalWidthPx = screenWidthDp.dp.roundToPx()
        val paddingPx = horizontalPaddingDp.dp.roundToPx()
        val spacingPx = gridSpacingDp.dp.roundToPx()
        val availableWidthPx = totalWidthPx - 2 * paddingPx - 2 * spacingPx
        val cellSizePx = cellSizeDp.dp.roundToPx()
        calculateColumns(availableWidthPx, cellSizePx, spacingPx)
    }

    /**
     * Calculates columns for the Albums grid matching [AlbumsGrid] layout:
     * - contentPadding start/end: 12.dp
     * - horizontalArrangement: (gridSpacing.dp + 8).dp
     */
    fun calculateAlbumColumns(
        screenWidthDp: Float,
        cellSizeDp: Float,
        gridSpacingDp: Float,
        density: Density,
        horizontalPaddingDp: Float = 0f
    ): Int = with(density) {
        val totalWidthPx = screenWidthDp.dp.roundToPx()
        val paddingPx = horizontalPaddingDp.dp.roundToPx()
        val spacingPx = (gridSpacingDp + 8f).dp.roundToPx()
        val contentPaddingPx = 12.dp.roundToPx()
        val availableWidthPx = totalWidthPx - 2 * paddingPx - 2 * contentPaddingPx
        val cellSizePx = cellSizeDp.dp.roundToPx()
        calculateColumns(availableWidthPx, cellSizePx, spacingPx)
    }

    /**
     * Calculates the ideal cellSize in Dp to target a specific number of columns
     * placed right in the center of that column count's active range.
     */
    fun idealPhotoCellSizeForColumns(
        targetCols: Int,
        screenWidthDp: Float,
        gridSpacingDp: Float,
        density: Density,
        horizontalPaddingDp: Float = 0f
    ): Float = with(density) {
        val totalWidthPx = screenWidthDp.dp.roundToPx()
        val paddingPx = horizontalPaddingDp.dp.roundToPx()
        val spacingPx = gridSpacingDp.dp.roundToPx()
        val availableWidthPx = totalWidthPx - 2 * paddingPx - 2 * spacingPx
        val targetSizePx = ((availableWidthPx + spacingPx) / (targetCols + 0.45f) - spacingPx)
        (targetSizePx / density.density).coerceAtLeast(36f)
    }

    fun idealAlbumCellSizeForColumns(
        targetCols: Int,
        screenWidthDp: Float,
        gridSpacingDp: Float,
        density: Density,
        horizontalPaddingDp: Float = 0f
    ): Float = with(density) {
        val totalWidthPx = screenWidthDp.dp.roundToPx()
        val paddingPx = horizontalPaddingDp.dp.roundToPx()
        val spacingPx = (gridSpacingDp + 8f).dp.roundToPx()
        val contentPaddingPx = 12.dp.roundToPx()
        val availableWidthPx = totalWidthPx - 2 * paddingPx - 2 * contentPaddingPx
        val targetSizePx = ((availableWidthPx + spacingPx) / (targetCols + 0.45f) - spacingPx)
        (targetSizePx / density.density).coerceAtLeast(48f)
    }
}
