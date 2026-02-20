package com.jworks.kanjilens.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.jworks.kanjilens.data.local.entities.BookmarkEntity

@Database(entities = [BookmarkEntity::class], version = 1, exportSchema = false)
abstract class UserDataDatabase : RoomDatabase() {
    abstract fun bookmarkDao(): BookmarkDao
}
