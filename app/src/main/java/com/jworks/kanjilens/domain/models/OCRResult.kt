package com.jworks.kanjilens.domain.models

import android.util.Size

data class OCRResult(
    val texts: List<DetectedText>,
    val timestamp: Long,
    val imageSize: Size,
    val processingTimeMs: Long = 0
)
