package com.jworks.kanjilens.data.nlp

import android.util.Log
import com.atilika.kuromoji.ipadic.Token
import com.atilika.kuromoji.ipadic.Tokenizer
import com.jworks.kanjilens.domain.models.JapaneseTextUtil
import com.jworks.kanjilens.domain.models.JapaneseToken
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KuromojiTokenizer @Inject constructor() {

    companion object {
        private const val TAG = "KuromojiTokenizer"
    }

    private var tokenizer: Tokenizer? = null
    private var initialized = false

    fun isReady(): Boolean = initialized

    fun initialize() {
        if (initialized) return
        try {
            tokenizer = Tokenizer()
            initialized = true
            Log.d(TAG, "Kuromoji initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Kuromoji", e)
        }
    }

    fun tokenize(text: String): List<JapaneseToken> {
        val tok = tokenizer ?: return emptyList()
        val tokens = tok.tokenize(text)
        return mapTokens(text, tokens)
    }

    private fun mapTokens(originalText: String, tokens: List<Token>): List<JapaneseToken> {
        val result = mutableListOf<JapaneseToken>()
        var pos = 0

        for (token in tokens) {
            val surface = token.surface
            // Find this token's position in the original text
            val idx = originalText.indexOf(surface, pos)
            if (idx < 0) continue

            val reading = katakanaToHiragana(token.reading ?: surface)
            val hasKanji = JapaneseTextUtil.containsKanji(surface)

            result.add(
                JapaneseToken(
                    surface = surface,
                    reading = reading,
                    startIndex = idx,
                    containsKanji = hasKanji
                )
            )
            pos = idx + surface.length
        }
        return result
    }

    private fun katakanaToHiragana(text: String): String {
        val sb = StringBuilder()
        for (ch in text) {
            if (ch in '\u30A0'..'\u30FF') {
                sb.append((ch.code - 0x60).toChar())
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }
}
