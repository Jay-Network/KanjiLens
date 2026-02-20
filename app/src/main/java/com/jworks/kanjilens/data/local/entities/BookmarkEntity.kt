package com.jworks.kanjilens.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey val word: String,
    val reading: String,
    @ColumnInfo(name = "bookmarked_at") val bookmarkedAt: Long = System.currentTimeMillis()
)
