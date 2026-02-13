package com.jworks.kanjilens.domain.repository

import com.jworks.kanjilens.domain.models.DictionaryResult

interface DictionaryRepository {
    suspend fun lookup(word: String): DictionaryResult?
    suspend fun search(prefix: String): List<DictionaryResult>
}
