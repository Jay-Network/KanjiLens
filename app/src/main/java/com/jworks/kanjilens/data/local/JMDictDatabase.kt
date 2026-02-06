package com.jworks.kanjilens.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.jworks.kanjilens.data.local.entities.FuriganaEntry

@Database(
    entities = [FuriganaEntry::class],
    version = 1,
    exportSchema = false
)
abstract class JMDictDatabase : RoomDatabase() {
    abstract fun jmDictDao(): JMDictDao
}
