package com.duong.udhoctap.core.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// ─────────────────────────────────────────────────
// Shared WebSocket event wrapper (sealed)
// ─────────────────────────────────────────────────

sealed class WsEvent {
    /** Raw JSON-parsed map arriving from the server */
    data class Message(val payload: Map<String, Any?>) : WsEvent()
    data class Error(val throwable: Throwable) : WsEvent()
    object Closed : WsEvent()
    object Connecting : WsEvent()
}

// ─────────────────────────────────────────────────
// AI CHAT
// ─────────────────────────────────────────────────

data class ChatRequest(
    val message: String,
    @Json(name = "session_id") val sessionId: String? = null,
    @Json(name = "kb_name") val kbName: String = "",
    @Json(name = "enable_rag") val enableRag: Boolean = false,
    @Json(name = "enable_web_search") val enableWebSearch: Boolean = false
)

sealed class ChatEvent {
    data class Session(val sessionId: String) : ChatEvent()
    data class Status(val stage: String, val message: String) : ChatEvent()
    data class Stream(val content: String) : ChatEvent()
    data class Result(val content: String) : ChatEvent()
    data class Sources(val rag: List<Map<String, Any?>>, val web: List<Map<String, Any?>>) : ChatEvent()
    data class AppError(val message: String) : ChatEvent()
}

data class ChatMessage(
    val role: String,      // "user" | "assistant"
    val content: String,
    val isStreaming: Boolean = false
)

// ─────────────────────────────────────────────────
// AI SOLVER
// ─────────────────────────────────────────────────

data class SolveRequest(
    val question: String,
    @Json(name = "session_id") val sessionId: String? = null,
    val tools: List<String>? = null,
    @Json(name = "kb_name") val kbName: String = "ai-textbook",
    @Json(name = "detailed_answer") val detailedAnswer: Boolean = false
)

sealed class SolveEvent {
    data class SessionCreated(val sessionId: String) : SolveEvent()
    data class TaskIdReceived(val taskId: String) : SolveEvent()
    data class Status(val content: String) : SolveEvent()
    data class AgentStatus(val agent: String, val status: String, val allAgents: Map<String, String>) : SolveEvent()
    data class Log(val level: String, val message: String) : SolveEvent()
    data class TokenStats(val stats: Map<String, Any?>) : SolveEvent()
    data class Progress(val stage: String, val progress: Map<String, Any?>) : SolveEvent()
    data class Result(
        val finalAnswer: String,
        val sessionId: String?,
        val outputDir: String?
    ) : SolveEvent()
    data class AppError(val message: String) : SolveEvent()
}

// ─────────────────────────────────────────────────
// AI QUESTION GENERATOR
// ─────────────────────────────────────────────────

data class QuestionRequirement(
    @Json(name = "knowledge_point") val knowledgePoint: String,
    val preference: String = "",
    val difficulty: String = "medium",
    @Json(name = "question_type") val questionType: String = ""
)

data class QuestionGenerateRequest(
    val requirement: QuestionRequirement,
    @Json(name = "kb_name") val kbName: String = "ai_textbook",
    val count: Int = 5
)

data class GeneratedQuestion(
    val id: String = java.util.UUID.randomUUID().toString(),
    val question: String,
    val answer: String,
    val explanation: String = "",
    val questionType: String = "",
    val difficulty: String = "",
    val options: List<String> = emptyList()
)

sealed class QuestionEvent {
    data class TaskIdReceived(val taskId: String) : QuestionEvent()
    data class Status(val content: String) : QuestionEvent()
    data class Log(val content: String) : QuestionEvent()
    data class QuestionGenerated(val question: GeneratedQuestion) : QuestionEvent()
    data class BatchSummary(val requested: Int, val completed: Int, val failed: Int) : QuestionEvent()
    data class Complete(val questions: List<GeneratedQuestion>) : QuestionEvent()
    data class AppError(val message: String) : QuestionEvent()
}

// ─────────────────────────────────────────────────
// REST — Sessions
// ─────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class SessionDto(
    @Json(name = "session_id") val sessionId: String,
    val title: String?,
    val capability: String?,
    @Json(name = "updated_at") val updatedAt: Long?,
    @Json(name = "created_at") val createdAt: Long?,
    @Json(name = "message_count") val messageCount: Int?,
    val status: String?
)

@JsonClass(generateAdapter = true)
data class SessionListResponse(
    val sessions: List<SessionDto>
)

// ─────────────────────────────────────────────────
// REST — Chat Sessions (JSON store, has full messages)
// ─────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class ChatMessageDto(
    val role: String,
    val content: String,
    val timestamp: Double?,
    val sources: Map<String, Any?>? = null
)

@JsonClass(generateAdapter = true)
data class FullChatSessionDto(
    @Json(name = "session_id") val sessionId: String,
    val title: String?,
    val messages: List<ChatMessageDto> = emptyList(),
    val settings: Map<String, Any?>? = null,
    @Json(name = "created_at") val createdAt: Double?,
    @Json(name = "updated_at") val updatedAt: Double?,
    @Json(name = "message_count") val messageCount: Int? = null,
    @Json(name = "last_message") val lastMessage: String? = null
)

@JsonClass(generateAdapter = true)
data class ChatSessionListResponse(
    val sessions: List<FullChatSessionDto>
)

// ─────────────────────────────────────────────────
// REST — Dashboard
// ─────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class ActivityDto(
    val id: String,
    val type: String,
    val capability: String,
    val title: String,
    val timestamp: Long?,
    val summary: String?,
    @Json(name = "message_count") val messageCount: Int?,
    val status: String?
)

// ─────────────────────────────────────────────────
// REST — Knowledge Base
// ─────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class KnowledgeBaseDto(
    val name: String,
    @Json(name = "is_default") val isDefault: Boolean,
    val statistics: Map<String, Any?>,
    val status: String?,
    val progress: Map<String, Any?>?
)

// ─────────────────────────────────────────────────
// REST — Quiz from Knowledge Base
// ─────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class KbQuizRequest(
    @Json(name = "kb_name") val kbName: String,
    val topic: String,
    val count: Int = 5,
    val difficulty: String = "medium",
    @Json(name = "question_type") val questionType: String = "mixed"
)

@JsonClass(generateAdapter = true)
data class KbQuizQuestion(
    val question: String,
    @Json(name = "correct_answer") val correctAnswer: String,
    val explanation: String = "",
    @Json(name = "question_type") val questionType: String,
    val options: List<String> = emptyList(),
    @Json(name = "correct_index") val correctIndex: Int = -1
)

@JsonClass(generateAdapter = true)
data class KbQuizResponse(
    @Json(name = "kb_name") val kbName: String,
    val topic: String,
    val questions: List<KbQuizQuestion>
)
