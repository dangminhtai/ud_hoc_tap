package com.duong.udhoctap.feature.aichat.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duong.udhoctap.core.data.repository.AiChatRepository
import com.duong.udhoctap.core.network.BackendApiService
import com.duong.udhoctap.core.network.dto.ChatEvent
import com.duong.udhoctap.core.network.dto.ChatMessage
import com.duong.udhoctap.core.network.dto.SessionDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ConnectionStatus { IDLE, CONNECTING, STREAMING, DONE, ERROR }

data class AiChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val status: ConnectionStatus = ConnectionStatus.IDLE,
    val statusMessage: String = "",
    val sessionId: String? = null,
    val enableRag: Boolean = false,
    val enableWebSearch: Boolean = false,
    val kbName: String = "",
    val error: String? = null,
    // Session history
    val sessions: List<SessionDto> = emptyList(),
    val isLoadingHistory: Boolean = false
)

@HiltViewModel
class AiChatViewModel @Inject constructor(
    private val chatRepository: AiChatRepository,
    private val backendApi: BackendApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiChatUiState())
    val uiState: StateFlow<AiChatUiState> = _uiState.asStateFlow()

    private var streamingJob: Job? = null
    private var streamingMessageIndex: Int = -1

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        streamingJob?.cancel()

        val state = _uiState.value
        val userMsg = ChatMessage(role = "user", content = text)
        val newMessages = state.messages + userMsg
        val assistantPlaceholder = ChatMessage(role = "assistant", content = "", isStreaming = true)
        streamingMessageIndex = newMessages.size
        _uiState.update {
            it.copy(messages = newMessages + assistantPlaceholder, status = ConnectionStatus.CONNECTING, error = null)
        }

        streamingJob = viewModelScope.launch {
            chatRepository.chat(
                message = text,
                sessionId = state.sessionId,
                enableRag = state.enableRag,
                enableWebSearch = state.enableWebSearch,
                kbName = state.kbName
            ).collect { event ->
                when (event) {
                    is ChatEvent.Session -> _uiState.update { it.copy(sessionId = event.sessionId) }
                    is ChatEvent.Status -> _uiState.update { it.copy(status = ConnectionStatus.STREAMING, statusMessage = event.message) }
                    is ChatEvent.Stream -> _uiState.update { state ->
                        val msgs = state.messages.toMutableList()
                        if (streamingMessageIndex < msgs.size) {
                            val current = msgs[streamingMessageIndex]
                            msgs[streamingMessageIndex] = current.copy(content = current.content + event.content)
                        }
                        state.copy(messages = msgs, status = ConnectionStatus.STREAMING)
                    }
                    is ChatEvent.Result -> _uiState.update { state ->
                        val msgs = state.messages.toMutableList()
                        if (streamingMessageIndex < msgs.size) {
                            msgs[streamingMessageIndex] = ChatMessage(role = "assistant", content = event.content, isStreaming = false)
                        }
                        state.copy(messages = msgs, status = ConnectionStatus.DONE, statusMessage = "")
                    }
                    is ChatEvent.AppError -> _uiState.update { it.copy(status = ConnectionStatus.ERROR, error = event.message) }
                    else -> Unit
                }
            }
        }
    }

    fun loadSessionHistory() {
        _uiState.update { it.copy(isLoadingHistory = true) }
        viewModelScope.launch {
            try {
                val result = backendApi.listSessions(limit = 30)
                _uiState.update { it.copy(sessions = result.sessions, isLoadingHistory = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoadingHistory = false) }
            }
        }
    }

    fun loadSession(sessionDto: SessionDto) {
        viewModelScope.launch {
            try {
                val full = backendApi.getSession(sessionDto.sessionId)
                // Build messages from session — the API returns a SessionDto without actual messages in list form
                // We start a new session continuing from this session_id
                _uiState.update {
                    AiChatUiState(
                        sessionId = full.sessionId,
                        enableRag = it.enableRag,
                        enableWebSearch = it.enableWebSearch,
                        kbName = it.kbName,
                        messages = listOf(ChatMessage(role = "assistant", content = "Tiếp tục cuộc trò chuyện: \"${full.title ?: full.sessionId}\""))
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            try {
                backendApi.deleteSession(sessionId)
                _uiState.update { it.copy(sessions = it.sessions.filter { s -> s.sessionId != sessionId }) }
            } catch (_: Exception) {}
        }
    }

    fun toggleRag() = _uiState.update { it.copy(enableRag = !it.enableRag) }
    fun toggleWebSearch() = _uiState.update { it.copy(enableWebSearch = !it.enableWebSearch) }
    fun setKbName(name: String) = _uiState.update { it.copy(kbName = name) }
    fun clearError() = _uiState.update { it.copy(error = null) }
    fun newSession() {
        streamingJob?.cancel()
        _uiState.update { AiChatUiState(enableRag = it.enableRag, enableWebSearch = it.enableWebSearch, kbName = it.kbName) }
    }

    override fun onCleared() {
        super.onCleared()
        streamingJob?.cancel()
    }
}
