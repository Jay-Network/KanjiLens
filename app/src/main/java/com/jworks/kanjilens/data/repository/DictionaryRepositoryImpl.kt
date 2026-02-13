package com.jworks.kanjilens.data.repository

import android.util.LruCache
import com.jworks.kanjilens.data.local.DictionaryDao
import com.jworks.kanjilens.data.local.entities.DictionaryEntry
import com.jworks.kanjilens.domain.models.DictionaryResult
import com.jworks.kanjilens.domain.models.DictionarySense
import com.jworks.kanjilens.domain.repository.DictionaryRepository
import org.json.JSONArray
import javax.inject.Inject

class DictionaryRepositoryImpl @Inject constructor(
    private val dictionaryDao: DictionaryDao
) : DictionaryRepository {

    private val cache = LruCache<String, DictionaryResult>(64)

    override suspend fun lookup(word: String): DictionaryResult? {
        cache.get(word)?.let { return it }

        val entry = dictionaryDao.getEntry(word) ?: return null
        val result = entry.toDomainModel()
        cache.put(word, result)
        return result
    }

    override suspend fun search(prefix: String): List<DictionaryResult> {
        return dictionaryDao.search(prefix).map { it.toDomainModel() }
    }

    private fun DictionaryEntry.toDomainModel(): DictionaryResult {
        val parsedSenses = parseSensesJson(senses)
        return DictionaryResult(
            word = word,
            reading = reading,
            senses = parsedSenses,
            isCommon = common == 1
        )
    }

    private fun parseSensesJson(json: String): List<DictionarySense> {
        return try {
            val array = JSONArray(json)
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                val glosses = mutableListOf<String>()
                val glossArray = obj.getJSONArray("g")
                for (j in 0 until glossArray.length()) {
                    glosses.add(glossArray.getString(j))
                }
                val pos = mutableListOf<String>()
                if (obj.has("p")) {
                    val posArray = obj.getJSONArray("p")
                    for (j in 0 until posArray.length()) {
                        pos.add(posArray.getString(j))
                    }
                }
                DictionarySense(partOfSpeech = pos, glosses = glosses)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
