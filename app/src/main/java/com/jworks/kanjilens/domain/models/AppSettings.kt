package com.jworks.kanjilens.domain.models

data class AppSettings(
    val kanjiColor: Long = 0xFF4CAF50,
    val kanaColor: Long = 0xFF2196F3,
    val strokeWidth: Float = 2f,
    val labelFontSize: Float = 14f,
    val labelBackgroundAlpha: Float = 0.7f,
    val frameSkip: Int = 3,
    val showDebugHud: Boolean = true
)
