package com.duong.udhoctap.core.network

import okhttp3.MultipartBody
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

interface DocumentApi {
    @Multipart
    @POST("api/v1/generate-flashcards")
    suspend fun generateFlashcards(
        @Part file: MultipartBody.Part
    ): GenerateFlashcardsResponse
}
