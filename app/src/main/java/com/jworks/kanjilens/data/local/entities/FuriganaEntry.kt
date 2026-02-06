package com.jworks.kanjilens.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "furigana")
data class FuriganaEntry(
    @PrimaryKey val word: String,
    val reading: String,
    val frequency: Int? = null
)
