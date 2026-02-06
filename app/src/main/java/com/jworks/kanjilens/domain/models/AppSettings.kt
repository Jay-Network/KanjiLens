package com.jworks.kanjilens.domain.models

data class AppSettings(
    val kanjiColor: Long = 0xFF4CAF50,
    val kanaColor: Long = 0xFF2196F3,
    val strokeWidth: Float = 2f,
    val labelFontSize: Float = 14f,
    val labelBackgroundAlpha: Float = 0.7f,
    val frameSkip: Int = 1,  // Process every frame for real-time feel
    val showDebugHud: Boolean = true,
    val showBoxes: Boolean = true,  // Show bounding boxes around text
    val furiganaIsBold: Boolean = false,  // Make furigana text bold
    val furiganaUseWhiteText: Boolean = false,  // White text (true) or black text (false)
    val partialModeBoundaryRatio: Float = 0.25f,  // 1.0 = full screen, 0.25 = horizontal partial, 0.40 = vertical partial
    val verticalTextMode: Boolean = false  // Vertical text rendering (縦書き)
)
