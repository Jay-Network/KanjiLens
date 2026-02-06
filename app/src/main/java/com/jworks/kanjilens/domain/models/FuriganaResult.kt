package com.jworks.kanjilens.domain.models

data class FuriganaResult(
    val word: String,
    val reading: String,
    val frequency: Int? = null
)
