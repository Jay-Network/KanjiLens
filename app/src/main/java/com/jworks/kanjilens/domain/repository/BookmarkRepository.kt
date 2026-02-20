package com.jworks.kanjilens.domain.repository

import com.jworks.kanjilens.domain.models.BookmarkEntry

interface BookmarkRepository {
    suspend fun toggle(word: String, reading: String): Boolean
    suspend fun isBookmarked(word: String): Boolean
    suspend fun getAll(): List<BookmarkEntry>
    suspend fun count(): Int
    suspend fun delete(word: String)
}
