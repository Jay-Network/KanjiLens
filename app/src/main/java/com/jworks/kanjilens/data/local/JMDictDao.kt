package com.jworks.kanjilens.data.local

import androidx.room.Dao
import androidx.room.Query
import com.jworks.kanjilens.data.local.entities.FuriganaEntry

@Dao
interface JMDictDao {
    @Query("SELECT * FROM furigana WHERE word = :word LIMIT 1")
    suspend fun getFurigana(word: String): FuriganaEntry?

    @Query("SELECT * FROM furigana WHERE word IN (:words)")
    suspend fun batchGetFurigana(words: List<String>): List<FuriganaEntry>
}
