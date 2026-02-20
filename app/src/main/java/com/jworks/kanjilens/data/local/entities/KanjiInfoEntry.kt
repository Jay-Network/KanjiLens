package com.jworks.kanjilens.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "kanji_info")
data class KanjiInfoEntry(
    @PrimaryKey val literal: String,
    val grade: Int?,
    @ColumnInfo(name = "stroke_count") val strokeCount: Int = 0,
    val freq: Int?,
    val jlpt: Int?,
    @ColumnInfo(name = "on_readings") val onReadings: String = "",
    @ColumnInfo(name = "kun_readings") val kunReadings: String = "",
    val meanings: String = ""
)
