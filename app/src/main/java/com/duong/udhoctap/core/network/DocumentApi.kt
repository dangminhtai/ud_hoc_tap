package com.duong.udhoctap.core.network

import com.squareup.moshi.Json
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

data class GeneratedFlashcardDto(
    val front: String,
    val back: String
)

data class GenerateFlashcardsResponse(
    val flashcards: List<GeneratedFlashcardDto>
)

data class ExplainCardRequest(
    val front: String,
    val back: String
)

data class ExplainCardResponse(
    val explanation: String
)

data class GenerateFromTextRequest(
    val text: String,
    @Json(name = "is_url") val isUrl: Boolean = false,
    val count: Int = 10,
    val difficulty: String = "medium"
)

interface DocumentApi {
    @Multipart
    @POST("api/v1/generate-flashcards")
    suspend fun generateFlashcards(
        @Part file: MultipartBody.Part,
        @Part("count") count: RequestBody,
        @Part("difficulty") difficulty: RequestBody
    ): GenerateFlashcardsResponse

    @POST("api/v1/explain-card")
    suspend fun explainCard(
        @Body request: ExplainCardRequest
    ): ExplainCardResponse

    @POST("api/v1/generate-from-text")
    suspend fun generateFromText(
        @Body request: GenerateFromTextRequest
    ): GenerateFlashcardsResponse
}
