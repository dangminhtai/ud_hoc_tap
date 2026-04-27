package com.duong.udhoctap.core.data.repository

import com.duong.udhoctap.core.network.BackendApiService
import com.duong.udhoctap.core.network.dto.KbItemDto
import com.duong.udhoctap.core.network.dto.KbProgressDto
import com.duong.udhoctap.core.network.dto.KbProgressEvent
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KnowledgeBaseRepository @Inject constructor(
    private val api: BackendApiService,
    private val okHttpClient: OkHttpClient
) {
    companion object {
        private const val WS_BASE = "ws://10.0.2.2:8001/api/v1/knowledge"
    }

    suspend fun listKbs(): List<KbItemDto> =
        try { api.listKbs() } catch (e: Exception) { emptyList() }

    suspend fun getDefault(): KbItemDto? =
        try { api.getDefaultKb() } catch (e: Exception) { null }

    suspend fun setDefault(name: String) =
        api.setDefaultKb(name)

    suspend fun deleteKb(name: String) =
        api.deleteKb(name)

    suspend fun getProgress(name: String): KbProgressDto? =
        try { api.getKbProgress(name) } catch (e: Exception) { null }

    suspend fun createKb(name: String, files: List<File>): KbItemDto {
        val nameBody = name.toRequestBody("text/plain".toMediaTypeOrNull())
        val parts = files.map { file ->
            MultipartBody.Part.createFormData(
                "files", file.name,
                file.asRequestBody("application/octet-stream".toMediaTypeOrNull())
            )
        }
        return api.createKb(nameBody, parts)
    }

    fun watchProgress(name: String): Flow<KbProgressEvent> = callbackFlow {
        val url = "$WS_BASE/$name/progress/ws"
        val request = Request.Builder().url(url).build()

        val ws = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val json = JSONObject(text)
                    val type = json.optString("type")
                    if (type == "progress") {
                        val data = json.optJSONObject("data")
                        if (data != null) {
                            val progress = KbProgressDto(
                                stage = data.optString("stage"),
                                message = data.optString("message"),
                                percent = data.optInt("percent", 0),
                                current = data.optInt("current", 0),
                                total = data.optInt("total", 0),
                                taskId = data.optString("task_id").ifBlank { null },
                                timestamp = data.optString("timestamp").ifBlank { null }
                            )
                            trySend(KbProgressEvent.Progress(progress))
                            if (progress.stage == "completed") {
                                trySend(KbProgressEvent.Completed(name))
                                channel.close()
                            } else if (progress.stage == "error") {
                                trySend(KbProgressEvent.AppError(progress.message))
                                channel.close()
                            }
                        }
                    }
                } catch (e: Exception) {
                    trySend(KbProgressEvent.AppError(e.message ?: "Parse error"))
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: okhttp3.Response?) {
                trySend(KbProgressEvent.AppError(t.message ?: "Connection failed"))
                channel.close(t)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                channel.close()
            }
        })

        awaitClose { ws.close(1000, "Progress watcher closed") }
    }
}
