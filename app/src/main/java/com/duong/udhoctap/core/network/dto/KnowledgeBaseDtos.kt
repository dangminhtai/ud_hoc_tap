package com.duong.udhoctap.core.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// ─────────────────────────────────────────────────
// Knowledge Base — REST DTOs
// ─────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class KbItemDto(
    val name: String,
    @Json(name = "is_default") val isDefault: Boolean = false,
    val status: String? = "ready",  // ready | processing | error
    @Json(name = "file_count") val fileCount: Int = 0,
    @Json(name = "chunk_count") val chunkCount: Int = 0,
    @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class KbProgressDto(
    val stage: String,       // processing | completed | error
    val message: String,
    val percent: Int = 0,
    val current: Int = 0,
    val total: Int = 0,
    @Json(name = "task_id") val taskId: String? = null,
    val timestamp: String? = null
)

@JsonClass(generateAdapter = true)
data class KbDefaultResponse(
    val name: String,
    @Json(name = "is_default") val isDefault: Boolean
)

// ─────────────────────────────────────────────────
// Knowledge Base — WebSocket Events
// ─────────────────────────────────────────────────

sealed class KbProgressEvent {
    data class Progress(val data: KbProgressDto) : KbProgressEvent()
    data class Completed(val name: String) : KbProgressEvent()
    data class AppError(val message: String) : KbProgressEvent()
}
