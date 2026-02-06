package com.jworks.kanjilens.domain.usecases

import com.jworks.kanjilens.domain.models.FuriganaResult
import com.jworks.kanjilens.domain.repository.FuriganaRepository
import javax.inject.Inject

class GetFuriganaUseCase @Inject constructor(
    private val repository: FuriganaRepository
) {
    suspend fun execute(text: String): Result<FuriganaResult> {
        return repository.getFurigana(text)
    }

    suspend fun executeBatch(texts: List<String>): Result<Map<String, FuriganaResult>> {
        return repository.batchGetFurigana(texts)
    }
}
