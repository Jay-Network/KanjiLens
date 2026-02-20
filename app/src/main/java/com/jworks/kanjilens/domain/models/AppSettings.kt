package com.jworks.kanjilens.domain.models

data class AppSettings(
    val kanjiColor: Long = 0xFF4CAF50,
    val kanaColor: Long = 0xFF2196F3,
    val strokeWidth: Float = 2f,
    val labelFontSize: Float = 14f,
    val frameSkip: Int = 1,  // Process every frame for real-time feel
    val showDebugHud: Boolean = false,
    val showBoxes: Boolean = false,  // Show bounding boxes around text
    val furiganaIsBold: Boolean = true,  // Make furigana text bold
    val furiganaUseWhiteText: Boolean = true,  // White text (true) or black text (false)
    val partialModeBoundaryRatio: Float = 1.0f,  // 1.0 = full screen, 0.25 = horizontal partial, 0.40 = vertical partial
    val verticalTextMode: Boolean = false,  // Vertical text rendering (縦書き)
    val furiganaAdaptiveColor: Boolean = true  // Per-element background-contrast furigana color
)
