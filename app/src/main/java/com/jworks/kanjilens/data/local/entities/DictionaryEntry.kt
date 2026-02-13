package com.jworks.kanjilens.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "jmdict_meanings")
data class DictionaryEntry(
    @PrimaryKey val word: String,
    val reading: String,
    val senses: String, // JSON: [{"p":["n"],"g":["language","tongue"]}]
    val common: Int = 0
)
