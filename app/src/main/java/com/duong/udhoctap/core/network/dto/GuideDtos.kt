package com.duong.udhoctap.core.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// ─────────────────────────────────────────────────
// Guide — REST DTOs
// ─────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class GuideSessionDto(
    val id: String,
    val title: String?,
    @Json(name = "user_input") val userInput: String,
    @Json(name = "knowledge_points") val knowledgePoints: List<String> = emptyList(),
    @Json(name = "current_index") val currentIndex: Int = -1,
    @Json(name = "created_at") val createdAt: String?,
    val status: String? = "pending"
)

@JsonClass(generateAdapter = true)
data class CreateGuideSessionRequest(
    @Json(name = "user_input") val userInput: String,
    @Json(name = "notebook_id") val notebookId: String? = null,
    val records: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class GuideChatRequest(
    @Json(name = "session_id") val sessionId: String,
    val message: String,
    @Json(name = "knowledge_index") val knowledgeIndex: Int = 0
)

@JsonClass(generateAdapter = true)
data class GuideChatResponse(
    val content: String,
    @Json(name = "session_id") val sessionId: String? = null
)

@JsonClass(generateAdapter = true)
data class GuideHtmlResponse(
    val html: String,
    val title: String? = null
)

@JsonClass(generateAdapter = true)
data class GuideNavigateRequest(
    @Json(name = "session_id") val sessionId: String,
    @Json(name = "knowledge_index") val knowledgeIndex: Int
)

@JsonClass(generateAdapter = true)
data class GuideActionRequest(
    @Json(name = "session_id") val sessionId: String
)

// ─────────────────────────────────────────────────
// Guide — WebSocket Events
// ─────────────────────────────────────────────────

sealed class GuideEvent {
    data class SessionInfo(val session: GuideSessionDto) : GuideEvent()
    data class StartResult(val currentIndex: Int, val totalPoints: Int) : GuideEvent()
    data class NavigateResult(val index: Int, val title: String) : GuideEvent()
    data class CompleteResult(val summary: String) : GuideEvent()
    data class ChatResult(val content: String, val isStreaming: Boolean = false) : GuideEvent()
    data class PagesInfo(val pages: List<String>, val readyCount: Int) : GuideEvent()
    data class HtmlReady(val index: Int) : GuideEvent()
    data class Status(val message: String) : GuideEvent()
    data class AppError(val message: String) : GuideEvent()
}

@JsonClass(generateAdapter = true)
data class GuideSessionSummaryResponse(
    @Json(name = "session_id") val sessionId: String,
    val topic: String?,
    val status: String?,
    @Json(name = "created_at") val createdAt: Double?,
    @Json(name = "total_points") val totalPoints: Int?,
    @Json(name = "ready_count") val readyCount: Int?,
    val progress: Int?
)

@JsonClass(generateAdapter = true)
data class GuideSessionListResponse(
    val sessions: List<GuideSessionSummaryResponse>
)

@JsonClass(generateAdapter = true)
data class GuideCreateSessionResponse(
    val success: Boolean,
    @Json(name = "session_id") val sessionId: String?,
    @Json(name = "total_points") val totalPoints: Int?,
    val error: String?
)
