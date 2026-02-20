package com.jworks.kanjilens.data.repository

import android.util.LruCache
import com.jworks.kanjilens.data.local.KanjiInfoDao
import com.jworks.kanjilens.domain.models.KanjiInfo
import com.jworks.kanjilens.domain.repository.KanjiInfoRepository
import javax.inject.Inject

class KanjiInfoRepositoryImpl @Inject constructor(
    private val kanjiInfoDao: KanjiInfoDao
) : KanjiInfoRepository {

    private val cache = LruCache<String, KanjiInfo>(128)

    override suspend fun getKanji(literal: String): KanjiInfo? {
        cache.get(literal)?.let { return it }

        val entry = kanjiInfoDao.getKanji(literal) ?: return null
        val info = KanjiInfo(
            literal = entry.literal,
            grade = entry.grade,
            strokeCount = entry.strokeCount,
            frequency = entry.freq,
            jlpt = entry.jlpt,
            onReadings = entry.onReadings.split(", ").filter { it.isNotBlank() },
            kunReadings = entry.kunReadings.split(", ").filter { it.isNotBlank() },
            meanings = entry.meanings.split(", ").filter { it.isNotBlank() }
        )
        cache.put(literal, info)
        return info
    }
}
