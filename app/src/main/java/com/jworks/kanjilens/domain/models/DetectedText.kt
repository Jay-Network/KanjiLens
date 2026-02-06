package com.jworks.kanjilens.domain.models

import android.graphics.Rect

data class DetectedText(
    val text: String,
    val bounds: Rect?,
    val confidence: Float,
    val language: String = "ja"
)
