package com.jworks.kanjilens.data.repository

import com.jworks.kanjilens.data.local.JMDictDao
import com.jworks.kanjilens.data.remote.FuriganaRequest
import com.jworks.kanjilens.data.remote.KuroshiroApi
import com.jworks.kanjilens.domain.models.FuriganaResult
import com.jworks.kanjilens.domain.repository.FuriganaRepository
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FuriganaRepositoryImpl @Inject constructor(
    private val jmDictDao: JMDictDao,
    private val kuroshiroApi: KuroshiroApi
) : FuriganaRepository {

    private val memoryCache = ConcurrentHashMap<String, FuriganaResult>()

    override suspend fun getFurigana(text: String): Result<FuriganaResult> {
        // 1. Check memory cache
        memoryCache[text]?.let { return Result.success(it) }

        // 2. Check local database
        jmDictDao.getFurigana(text)?.let { entry ->
            val result = FuriganaResult(
                word = entry.word,
                reading = entry.reading,
                frequency = entry.frequency
            )
            memoryCache[text] = result
            return Result.success(result)
        }

        // 3. Fallback to backend API
        return try {
            val response = kuroshiroApi.getFurigana(FuriganaRequest(listOf(text)))
            val reading = response.results[text]
                ?: return Result.failure(Exception("No furigana found for: $text"))
            val result = FuriganaResult(word = text, reading = reading)
            memoryCache[text] = result
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun batchGetFurigana(texts: List<String>): Result<Map<String, FuriganaResult>> {
        val results = mutableMapOf<String, FuriganaResult>()
        val missing = mutableListOf<String>()

        for (text in texts) {
            val cached = memoryCache[text]
            if (cached != null) {
                results[text] = cached
            } else {
                val entry = jmDictDao.getFurigana(text)
                if (entry != null) {
                    val result = FuriganaResult(
                        word = entry.word,
                        reading = entry.reading,
                        frequency = entry.frequency
                    )
                    memoryCache[text] = result
                    results[text] = result
                } else {
                    missing.add(text)
                }
            }
        }

        if (missing.isNotEmpty()) {
            try {
                val response = kuroshiroApi.getFurigana(FuriganaRequest(missing))
                for ((word, reading) in response.results) {
                    val result = FuriganaResult(word = word, reading = reading)
                    memoryCache[word] = result
                    results[word] = result
                }
            } catch (e: Exception) {
                if (results.isEmpty()) return Result.failure(e)
            }
        }

        return Result.success(results)
    }

    override fun clearCache() {
        memoryCache.clear()
    }
}
