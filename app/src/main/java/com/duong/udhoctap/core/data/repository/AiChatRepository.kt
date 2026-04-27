package com.duong.udhoctap.core.data.repository

import com.duong.udhoctap.core.network.WebSocketManager
import com.duong.udhoctap.core.network.dto.ChatEvent
import com.duong.udhoctap.core.network.dto.ChatRequest
import com.duong.udhoctap.core.network.dto.WsEvent
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiChatRepository @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val moshi: Moshi
) {
    companion object {
        // ws:// because the emulator maps 10.0.2.2 → host
        private const val WS_URL = "ws://10.0.2.2:8001/api/v1/chat"
    }

    /**
     * Opens a streaming chat session.
     * Returns a [Flow] of [ChatEvent] that stays alive until the conversation ends.
     * Use [sendBlock] to transmit the first (and any subsequent) messages.
     */
    fun chat(
        message: String,
        sessionId: String? = null,
        enableRag: Boolean = false,
        enableWebSearch: Boolean = false,
        kbName: String = ""
    ): Flow<ChatEvent> = callbackFlow {

        val request = Request.Builder().url(WS_URL).build()

        val ws = okHttpClient.newWebSocket(request, object : WebSocketListener() {

            override fun onOpen(webSocket: WebSocket, response: okhttp3.Response) {
                // Send initial message
                val payload = JSONObject().apply {
                    put("message", message)
                    if (sessionId != null) put("session_id", sessionId)
                    put("kb_name", kbName)
                    put("enable_rag", enableRag)
                    put("enable_web_search", enableWebSearch)
                }
                webSocket.send(payload.toString())
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val json = JSONObject(text)
                    val type = json.optString("type")
                    val event: ChatEvent = when (type) {
                        "session"  -> ChatEvent.Session(json.optString("session_id"))
                        "status"   -> ChatEvent.Status(
                            stage = json.optString("stage"),
                            message = json.optString("message")
                        )
                        "stream"   -> ChatEvent.Stream(json.optString("content"))
                        "result"   -> ChatEvent.Result(json.optString("content"))
                        "sources"  -> ChatEvent.Sources(
                            rag = emptyList(), web = emptyList()
                        )
                        "error"    -> ChatEvent.AppError(json.optString("message"))
                        else       -> ChatEvent.Status("unknown", text)
                    }
                    trySend(event)
                    if (type == "result" || type == "error") {
                        channel.close()
                    }
                } catch (e: Exception) {
                    trySend(ChatEvent.AppError(e.message ?: "Parse error"))
                    channel.close(e)
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: okhttp3.Response?) {
                trySend(ChatEvent.AppError(t.message ?: "WebSocket failure"))
                channel.close(t)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                channel.close()
            }
        })

        awaitClose { ws.close(1000, "Chat flow cancelled") }
    }
}
