package com.jworks.kanjilens.domain.repository

import com.jworks.kanjilens.domain.models.FuriganaResult

interface FuriganaRepository {
    suspend fun getFurigana(text: String): Result<FuriganaResult>
    suspend fun batchGetFurigana(texts: List<String>): Result<Map<String, FuriganaResult>>
    fun clearCache()
}
