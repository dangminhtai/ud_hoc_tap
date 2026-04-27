package com.duong.udhoctap.core.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// ─────────────────────────────────────────────────
// Notebook — REST DTOs
// ─────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class NotebookDto(
    val id: String,
    val name: String,
    val description: String? = null,
    val tags: List<String> = emptyList(),
    @Json(name = "record_count") val recordCount: Int = 0,
    @Json(name = "created_at") val createdAt: String?,
    @Json(name = "updated_at") val updatedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class NotebookDetailDto(
    val id: String,
    val name: String,
    val description: String? = null,
    val tags: List<String> = emptyList(),
    val records: List<NotebookRecordDto> = emptyList(),
    @Json(name = "created_at") val createdAt: String?
)

@JsonClass(generateAdapter = true)
data class NotebookRecordDto(
    val id: String,
    @Json(name = "notebook_ids") val notebookIds: List<String> = emptyList(),
    @Json(name = "record_type") val recordType: String,
    val title: String,
    val summary: String? = null,
    @Json(name = "user_query") val userQuery: String? = null,
    val output: String? = null,
    @Json(name = "kb_name") val kbName: String? = null,
    @Json(name = "created_at") val createdAt: String?
)

@JsonClass(generateAdapter = true)
data class CreateNotebookRequest(
    val name: String,
    val description: String? = null,
    val tags: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class AddRecordRequest(
    @Json(name = "notebook_ids") val notebookIds: List<String>,
    @Json(name = "record_type") val recordType: String,
    val title: String,
    val summary: String? = null,
    @Json(name = "user_query") val userQuery: String? = null,
    val output: String? = null,
    @Json(name = "kb_name") val kbName: String? = null
)

@JsonClass(generateAdapter = true)
data class NotebookStatsDto(
    @Json(name = "total_notebooks") val totalNotebooks: Int = 0,
    @Json(name = "total_records") val totalRecords: Int = 0,
    @Json(name = "records_by_type") val recordsByType: Map<String, Int> = emptyMap()
)

enum class RecordType(val value: String, val displayName: String) {
    SOLVE("solve", "Giải toán"),
    QUESTION("question", "Câu hỏi"),
    RESEARCH("research", "Nghiên cứu"),
    CO_WRITER("co_writer", "Viết"),
    CHAT("chat", "Chat"),
    GUIDED_LEARNING("guided_learning", "Học có hướng dẫn");

    companion object {
        fun from(value: String) = entries.find { it.value == value } ?: CHAT
    }
}
