package com.jworks.kanjilens.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jworks.kanjilens.data.local.entities.BookmarkEntity

@Dao
interface BookmarkDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE word = :word")
    suspend fun delete(word: String)

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE word = :word)")
    suspend fun isBookmarked(word: String): Boolean

    @Query("SELECT * FROM bookmarks ORDER BY bookmarked_at DESC")
    suspend fun getAll(): List<BookmarkEntity>

    @Query("SELECT COUNT(*) FROM bookmarks")
    suspend fun count(): Int
}
