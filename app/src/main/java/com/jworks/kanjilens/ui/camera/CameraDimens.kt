package com.jworks.kanjilens.ui.camera

import androidx.compose.ui.unit.dp

/**
 * Centralized dimension constants for the camera UI.
 * Eliminates magic numbers scattered across CameraScreen, ButtonCluster, and overlays.
 */
object CameraDimens {
    // Button grid
    val BUTTON_SIZE = 44.dp
    val ICON_SIZE = 22.dp
    const val BUTTON_GAP_PX = 12f
    val LEFT_MARGIN = 18.dp
    const val STATUS_BAR_ESTIMATE_DP = 48

    // Layout margins
    const val RIGHT_MARGIN_LANDSCAPE_DP = 48
    const val RIGHT_MARGIN_PORTRAIT_PX = 24f
    const val BOTTOM_PADDING_LANDSCAPE_PX = 16f
    const val BOTTOM_PADDING_PORTRAIT_DP = 64
    const val TOP_MARGIN_EXTRA_PX = 12f

    // Processing indicator
    val PROCESSING_INDICATOR_SIZE = 24.dp
    val PROCESSING_INDICATOR_PADDING = 12.dp
    const val PROCESSING_INDICATOR_STROKE_WIDTH = 2

    // Version label
    val VERSION_LABEL_BOTTOM_PADDING = 40.dp

    // Scan timer
    const val TIMER_WARNING_THRESHOLD_SECONDS = 10

    // Coin toast
    val COIN_TOAST_BOTTOM_PADDING = 100.dp

    // Partial mode boundary threshold (values >= this are "full screen")
    const val FULL_MODE_THRESHOLD = 0.99f

    // Partial mode boundary targets
    const val HORIZONTAL_PARTIAL_RATIO = 0.25f
    const val VERTICAL_PARTIAL_RATIO = 0.40f

    // Animation
    const val SPRING_DAMPING = 0.7f
    const val SPRING_STIFFNESS = 300f

    // Mode toggle threshold (above this = full, below = partial)
    const val MODE_TOGGLE_THRESHOLD = 0.6f
}
