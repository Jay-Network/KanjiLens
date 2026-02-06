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
    val usePartialMode: Boolean = false,  // Partial screen mode for better FPS
    val partialModeBoundaryRatio: Float = 0.25f  // Camera area height ratio (0.25 = 25% camera, 75% features)
)
