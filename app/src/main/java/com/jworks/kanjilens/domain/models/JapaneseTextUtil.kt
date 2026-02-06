package com.jworks.kanjilens.domain.models

object JapaneseTextUtil {

    // CJK Unified Ideographs: U+4E00 - U+9FFF
    // CJK Extension A: U+3400 - U+4DBF
    private val KANJI_RANGE = Regex("[\\u4E00-\\u9FFF\\u3400-\\u4DBF]")

    // Hiragana: U+3040 - U+309F
    private val HIRAGANA_RANGE = Regex("[\\u3040-\\u309F]")

    // Katakana: U+30A0 - U+30FF
    private val KATAKANA_RANGE = Regex("[\\u30A0-\\u30FF]")

    // Any Japanese character (kanji, hiragana, katakana)
    private val JAPANESE_RANGE = Regex("[\\u3040-\\u309F\\u30A0-\\u30FF\\u4E00-\\u9FFF\\u3400-\\u4DBF]")

    fun containsKanji(text: String): Boolean = KANJI_RANGE.containsMatchIn(text)

    fun containsJapanese(text: String): Boolean = JAPANESE_RANGE.containsMatchIn(text)

    fun containsHiragana(text: String): Boolean = HIRAGANA_RANGE.containsMatchIn(text)

    fun containsKatakana(text: String): Boolean = KATAKANA_RANGE.containsMatchIn(text)

    fun kanjiCount(text: String): Int = KANJI_RANGE.findAll(text).count()

    fun japaneseRatio(text: String): Float {
        if (text.isEmpty()) return 0f
        val jpChars = JAPANESE_RANGE.findAll(text).count()
        return jpChars.toFloat() / text.length
    }
}
