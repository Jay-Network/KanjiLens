package com.jworks.kanjilens.data.bookmark

import com.jworks.kanjilens.data.local.BookmarkDao
import com.jworks.kanjilens.data.local.entities.BookmarkEntity
import com.jworks.kanjilens.domain.models.BookmarkEntry
import com.jworks.kanjilens.domain.repository.BookmarkRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookmarkRepositoryImpl @Inject constructor(
    private val bookmarkDao: BookmarkDao
) : BookmarkRepository {

    override suspend fun toggle(word: String, reading: String): Boolean {
        return if (bookmarkDao.isBookmarked(word)) {
            bookmarkDao.delete(word)
            false
        } else {
            bookmarkDao.insert(BookmarkEntity(word = word, reading = reading))
            true
        }
    }

    override suspend fun isBookmarked(word: String): Boolean {
        return bookmarkDao.isBookmarked(word)
    }

    override suspend fun getAll(): List<BookmarkEntry> {
        return bookmarkDao.getAll().map { entity ->
            BookmarkEntry(
                word = entity.word,
                reading = entity.reading,
                bookmarkedAt = entity.bookmarkedAt
            )
        }
    }

    override suspend fun count(): Int {
        return bookmarkDao.count()
    }

    override suspend fun delete(word: String) {
        bookmarkDao.delete(word)
    }
}
