package com.jworks.kanjilens.domain.models

data class DictionaryResult(
    val word: String,
    val reading: String,
    val senses: List<DictionarySense>,
    val isCommon: Boolean
)

data class DictionarySense(
    val partOfSpeech: List<String>,
    val glosses: List<String>
)
