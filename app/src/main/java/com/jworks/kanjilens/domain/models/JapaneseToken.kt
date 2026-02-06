package com.jworks.kanjilens.domain.models

data class JapaneseToken(
    val surface: String,       // Original text (e.g. "今日")
    val reading: String,       // Hiragana reading (e.g. "きょう")
    val startIndex: Int,       // Position in original sentence
    val containsKanji: Boolean
)
