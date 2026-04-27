package com.duong.udhoctap.core.data.repository

import com.duong.udhoctap.core.network.dto.GeneratedQuestion
import com.duong.udhoctap.core.network.dto.QuestionEvent
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiQuestionRepository @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    companion object {
        private const val WS_URL = "ws://10.0.2.2:8001/api/v1/question/generate"
    }

    fun generate(
        topic: String,
        count: Int = 5,
        difficulty: String = "medium",
        questionType: String = "",
        preference: String = "",
        kbName: String = "ai_textbook"
    ): Flow<QuestionEvent> = callbackFlow {

        val request = Request.Builder().url(WS_URL).build()

        val collectedQuestions = mutableListOf<GeneratedQuestion>()

        val ws = okHttpClient.newWebSocket(request, object : WebSocketListener() {

            override fun onOpen(webSocket: WebSocket, response: okhttp3.Response) {
                val requirement = JSONObject().apply {
                    put("knowledge_point", topic)
                    put("preference", preference)
                    put("difficulty", difficulty)
                    put("question_type", questionType)
                }
                val payload = JSONObject().apply {
                    put("requirement", requirement)
                    put("kb_name", kbName)
                    put("count", count)
                }
                webSocket.send(payload.toString())
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val json = JSONObject(text)
                    val type = json.optString("type")

                    when (type) {
                        "task_id" -> trySend(QuestionEvent.TaskIdReceived(json.optString("task_id")))
                        "status"  -> trySend(QuestionEvent.Status(json.optString("content")))
                        "log"     -> trySend(QuestionEvent.Log(json.optString("content")))

                        "question", "result" -> {
                            // Individual question streamed from the coordinator
                            val questionObj = json.optJSONObject("question") ?: json
                            val q = parseQuestion(questionObj)
                            collectedQuestions.add(q)
                            trySend(QuestionEvent.QuestionGenerated(q))
                        }

                        "batch_summary" -> trySend(
                            QuestionEvent.BatchSummary(
                                requested = json.optInt("requested", count),
                                completed = json.optInt("completed", 0),
                                failed = json.optInt("failed", 0)
                            )
                        )

                        "complete" -> {
                            trySend(QuestionEvent.Complete(collectedQuestions.toList()))
                            channel.close()
                        }

                        "error" -> {
                            trySend(QuestionEvent.AppError(json.optString("content")))
                            channel.close()
                        }
                    }
                } catch (e: Exception) {
                    trySend(QuestionEvent.AppError(e.message ?: "Parse error"))
                    channel.close(e)
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: okhttp3.Response?) {
                trySend(QuestionEvent.AppError(t.message ?: "WebSocket failure"))
                channel.close(t)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                channel.close()
            }
        })

        awaitClose { ws.close(1000, "Question flow cancelled") }
    }

    private fun parseQuestion(json: JSONObject): GeneratedQuestion {
        // Backend sends question data — try multiple field layouts
        val questionText = json.optString("question")
            .ifBlank { json.optString("stem") }
            .ifBlank { json.optString("content") }
        val answer = json.optString("answer")
            .ifBlank { json.optString("correct_answer") }
        val explanation = json.optString("explanation")
            .ifBlank { json.optString("analysis") }
        val qType = json.optString("question_type")
            .ifBlank { json.optString("type") }
        val difficulty = json.optString("difficulty")

        val optionsArray: List<String> = try {
            val arr: JSONArray? = json.optJSONArray("options")
                ?: json.optJSONArray("choices")
            buildList {
                if (arr != null) {
                    for (i in 0 until arr.length()) add(arr.getString(i))
                }
            }
        } catch (e: Exception) { emptyList() }

        return GeneratedQuestion(
            id = UUID.randomUUID().toString(),
            question = questionText,
            answer = answer,
            explanation = explanation,
            questionType = qType,
            difficulty = difficulty,
            options = optionsArray
        )
    }
}
