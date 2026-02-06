package com.jworks.kanjilens.data.remote

import retrofit2.http.Body
import retrofit2.http.POST

data class FuriganaRequest(val texts: List<String>)
data class FuriganaResponse(val results: Map<String, String>)

interface KuroshiroApi {
    @POST("/api/furigana")
    suspend fun getFurigana(@Body request: FuriganaRequest): FuriganaResponse
}
