package com.duong.udhoctap.core.network

import com.duong.udhoctap.core.network.dto.ActivityDto
import com.duong.udhoctap.core.network.dto.AddRecordRequest
import com.duong.udhoctap.core.network.dto.CreateGuideSessionRequest
import com.duong.udhoctap.core.network.dto.CreateNotebookRequest
import com.duong.udhoctap.core.network.dto.GuideChatRequest
import com.duong.udhoctap.core.network.dto.GuideChatResponse
import com.duong.udhoctap.core.network.dto.GuideHtmlResponse
import com.duong.udhoctap.core.network.dto.GuideNavigateRequest
import com.duong.udhoctap.core.network.dto.GuideSessionDto
import com.duong.udhoctap.core.network.dto.KbItemDto
import com.duong.udhoctap.core.network.dto.KbProgressDto
import com.duong.udhoctap.core.network.dto.KnowledgeBaseDto
import com.duong.udhoctap.core.network.dto.NotebookDetailDto
import com.duong.udhoctap.core.network.dto.NotebookDto
import com.duong.udhoctap.core.network.dto.NotebookRecordDto
import com.duong.udhoctap.core.network.dto.NotebookStatsDto
import com.duong.udhoctap.core.network.dto.SessionDto
import com.duong.udhoctap.core.network.dto.SessionListResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface BackendApiService {

    // ── Dashboard ─────────────────────────────────────────────────────────────

    @GET("api/v1/dashboard/recent")
    suspend fun getRecentActivities(
        @Query("limit") limit: Int = 20,
        @Query("type") type: String? = null
    ): List<ActivityDto>

    // ── Sessions ──────────────────────────────────────────────────────────────

    @GET("api/v1/sessions")
    suspend fun listSessions(
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0
    ): SessionListResponse

    @GET("api/v1/sessions/{session_id}")
    suspend fun getSession(@Path("session_id") sessionId: String): SessionDto

    @DELETE("api/v1/sessions/{session_id}")
    suspend fun deleteSession(@Path("session_id") sessionId: String): Map<String, Any>

    // ── Knowledge Base (legacy) ───────────────────────────────────────────────

    @GET("api/v1/knowledge/list")
    suspend fun listKnowledgeBases(): List<KnowledgeBaseDto>

    // ── Knowledge Base (full) ─────────────────────────────────────────────────

    @GET("api/v1/knowledge/list")
    suspend fun listKbs(): List<KbItemDto>

    @GET("api/v1/knowledge/default")
    suspend fun getDefaultKb(): KbItemDto

    @PUT("api/v1/knowledge/default/{name}")
    suspend fun setDefaultKb(@Path("name") name: String): Map<String, Any>

    @DELETE("api/v1/knowledge/{name}")
    suspend fun deleteKb(@Path("name") name: String): Map<String, Any>

    @GET("api/v1/knowledge/{name}/progress")
    suspend fun getKbProgress(@Path("name") name: String): KbProgressDto

    @Multipart
    @POST("api/v1/knowledge/create")
    suspend fun createKb(
        @Part("name") name: RequestBody,
        @Part files: List<MultipartBody.Part>
    ): KbItemDto

    @Multipart
    @POST("api/v1/knowledge/{name}/upload")
    suspend fun uploadToKb(
        @Path("name") name: String,
        @Part files: List<MultipartBody.Part>
    ): Map<String, Any>

    // ── System ────────────────────────────────────────────────────────────────

    @GET("api/v1/system/status")
    suspend fun getSystemStatus(): Map<String, Any>

    // ── Guide ─────────────────────────────────────────────────────────────────

    @POST("api/v1/guide/create_session")
    suspend fun createGuideSession(@Body request: CreateGuideSessionRequest): com.duong.udhoctap.core.network.dto.GuideCreateSessionResponse

    @POST("api/v1/guide/chat")
    suspend fun guideChat(@Body request: GuideChatRequest): GuideChatResponse

    @GET("api/v1/guide/session/{id}/html")
    suspend fun getGuideHtml(@Path("id") sessionId: String): GuideHtmlResponse

    @GET("api/v1/guide/sessions")
    suspend fun listGuideSessions(): com.duong.udhoctap.core.network.dto.GuideSessionListResponse

    @DELETE("api/v1/guide/session/{id}")
    suspend fun deleteGuideSession(@Path("id") sessionId: String): Map<String, Any>

    // ── Notebook ──────────────────────────────────────────────────────────────

    @GET("api/v1/notebook/list")
    suspend fun listNotebooks(): List<NotebookDto>

    @POST("api/v1/notebook/create")
    suspend fun createNotebook(@Body request: CreateNotebookRequest): NotebookDto

    @GET("api/v1/notebook/{id}")
    suspend fun getNotebook(@Path("id") id: String): NotebookDetailDto

    @DELETE("api/v1/notebook/{id}")
    suspend fun deleteNotebook(@Path("id") id: String): Map<String, Any>

    @POST("api/v1/notebook/add_record")
    suspend fun addRecord(@Body request: AddRecordRequest): NotebookRecordDto

    @DELETE("api/v1/notebook/{id}/records/{recordId}")
    suspend fun deleteRecord(
        @Path("id") notebookId: String,
        @Path("recordId") recordId: String
    ): Map<String, Any>

    @GET("api/v1/notebook/statistics")
    suspend fun getNotebookStats(): NotebookStatsDto
}
