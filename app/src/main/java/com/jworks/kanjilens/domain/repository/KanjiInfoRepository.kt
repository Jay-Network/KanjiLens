package com.jworks.kanjilens.domain.repository

import com.jworks.kanjilens.domain.models.KanjiInfo

interface KanjiInfoRepository {
    suspend fun getKanji(literal: String): KanjiInfo?
}
