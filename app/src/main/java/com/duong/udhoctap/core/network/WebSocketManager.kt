package com.duong.udhoctap.core.network

import com.duong.udhoctap.core.network.dto.WsEvent
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebSocketManager @Inject constructor(
    private val okHttpClient: OkHttpClient
) {

    /**
     * Opens a WebSocket connection to [url] and returns a cold [Flow] of [WsEvent].
     * The flow emits events until the connection closes or an error occurs.
     * Cancelling the flow collector will close the WebSocket.
     */
    fun connect(url: String): Flow<WsEvent> = callbackFlow {
        trySend(WsEvent.Connecting)

        val request = Request.Builder().url(url).build()

        val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                // Connection is open — consumers can now send frames
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val json = JSONObject(text)
                    val map = json.toMap()
                    trySend(WsEvent.Message(map))
                } catch (e: Exception) {
                    trySend(WsEvent.Error(e))
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
                trySend(WsEvent.Closed)
                channel.close()
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                trySend(WsEvent.Closed)
                channel.close()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                trySend(WsEvent.Error(t))
                channel.close(t)
            }
        }

        val ws = okHttpClient.newWebSocket(request, listener)

        awaitClose { ws.close(1000, "Flow cancelled") }
    }

    /** Sends a raw JSON string over an already-open WebSocket. */
    fun send(webSocket: WebSocket, json: String): Boolean = webSocket.send(json)
}

// ─── Extension helpers ───────────────────────────────────────────────────────

/** Recursively converts a [JSONObject] to a Kotlin Map<String, Any?>. */
private fun JSONObject.toMap(): Map<String, Any?> {
    val map = mutableMapOf<String, Any?>()
    keys().forEach { key ->
        map[key] = when (val value = get(key)) {
            is JSONObject -> value.toMap()
            JSONObject.NULL -> null
            else -> value
        }
    }
    return map
}
