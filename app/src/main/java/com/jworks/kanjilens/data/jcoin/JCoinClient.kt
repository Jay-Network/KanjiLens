package com.jworks.kanjilens.data.jcoin

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.functions.functions
import io.ktor.client.call.body
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class JCoinBalance(
    val balance: Int = 0,
    val lifetimeEarned: Int = 0
)

@Serializable
data class JCoinEarnResponse(
    val success: Boolean,
    val coinsAwarded: Int = 0,
    val newBalance: Int = 0,
    val message: String? = null
)

@Singleton
class JCoinClient @Inject constructor(
    private val supabaseClient: SupabaseClient
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun getBalance(accessToken: String): Result<JCoinBalance> {
        return try {
            val response = supabaseClient.functions.invoke(
                function = "jcoin-balance",
                headers = Headers.build {
                    append(HttpHeaders.Authorization, "Bearer $accessToken")
                },
                body = buildJsonObject {
                    put("business", "kanjilens")
                }
            )
            val body = response.body<String>()
            Result.success(json.decodeFromString<JCoinBalance>(body))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun earn(
        accessToken: String,
        sourceType: String,
        baseAmount: Int,
        metadata: Map<String, String> = emptyMap()
    ): Result<JCoinEarnResponse> {
        return try {
            val response = supabaseClient.functions.invoke(
                function = "jcoin-earn",
                headers = Headers.build {
                    append(HttpHeaders.Authorization, "Bearer $accessToken")
                },
                body = buildJsonObject {
                    put("source_business", "kanjilens")
                    put("source_type", sourceType)
                    put("base_amount", baseAmount)
                    put("metadata", buildJsonObject {
                        metadata.forEach { (k, v) -> put(k, v) }
                    })
                }
            )
            val body = response.body<String>()
            Result.success(json.decodeFromString<JCoinEarnResponse>(body))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun spend(
        accessToken: String,
        sourceType: String,
        amount: Int,
        description: String
    ): Result<JCoinEarnResponse> {
        return try {
            val response = supabaseClient.functions.invoke(
                function = "jcoin-spend",
                headers = Headers.build {
                    append(HttpHeaders.Authorization, "Bearer $accessToken")
                },
                body = buildJsonObject {
                    put("source_business", "kanjilens")
                    put("source_type", sourceType)
                    put("amount", amount)
                    put("description", description)
                }
            )
            val body = response.body<String>()
            Result.success(json.decodeFromString<JCoinEarnResponse>(body))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
