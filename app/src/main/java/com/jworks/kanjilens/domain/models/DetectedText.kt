package com.jworks.kanjilens.domain.models

import android.graphics.Rect

data class DetectedText(
    val text: String,
    val bounds: Rect?,
    val confidence: Float,
    val language: String = "ja",
    val containsKanji: Boolean = false,
    val elements: List<TextElement> = emptyList()
)

data class TextElement(
    val text: String,
    val bounds: Rect?,
    val containsKanji: Boolean = false,
    val reading: String? = null,
    val kanjiSegments: List<KanjiSegment> = emptyList()
)

data class KanjiSegment(
    val text: String,
    val reading: String,
    val startIndex: Int,
    val endIndex: Int
)
