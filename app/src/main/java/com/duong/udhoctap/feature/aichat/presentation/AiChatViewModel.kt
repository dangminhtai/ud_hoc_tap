package com.duong.udhoctap.feature.aichat.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duong.udhoctap.core.data.repository.AiChatRepository
import com.duong.udhoctap.core.network.BackendApiService
import com.duong.udhoctap.core.network.dto.ChatEvent
import com.duong.udhoctap.core.network.dto.ChatMessage
import com.duong.udhoctap.core.network.dto.FullChatSessionDto
import com.duong.udhoctap.core.network.dto.SessionDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ConnectionStatus { IDLE, CONNECTING, THINKING, STREAMING, DONE, ERROR }

data class AiChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val status: ConnectionStatus = ConnectionStatus.IDLE,
    val statusMessage: String = "",
    val sessionId: String? = null,
    val enableRag: Boolean = false,
    val enableWebSearch: Boolean = false,
    val selectedKbs: Set<String> = setOf("default"),
    val availableKbs: List<com.duong.udhoctap.core.network.dto.KbItemDto> = emptyList(),
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

    init {
        loadAvailableKbs()
    }

    fun loadAvailableKbs() {
        viewModelScope.launch {
            try {
                val kbs = backendApi.listKbs()
                _uiState.update { it.copy(availableKbs = kbs) }
            } catch (_: Exception) {}
        }
    }

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
            // Chuyển Set sang List để gửi qua WebSocket
            chatRepository.chat(
                message = text,
                sessionId = state.sessionId,
                enableRag = state.enableRag,
                enableWebSearch = state.enableWebSearch,
                kbNames = state.selectedKbs.toList()
            ).collect { event ->
                when (event) {
                    is ChatEvent.Session -> _uiState.update { it.copy(sessionId = event.sessionId) }
                    is ChatEvent.Status -> _uiState.update { it.copy(status = ConnectionStatus.THINKING, statusMessage = event.message) }
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
                    is ChatEvent.Sources -> _uiState.update { state ->
                        val msgs = state.messages.toMutableList()
                        if (streamingMessageIndex < msgs.size) {
                            val current = msgs[streamingMessageIndex]
                            msgs[streamingMessageIndex] = current.copy(
                                sources = mapOf(
                                    "rag" to event.rag,
                                    "web" to event.web
                                )
                            )
                        }
                        state.copy(messages = msgs)
                    }
                    is ChatEvent.AppError -> _uiState.update { it.copy(status = ConnectionStatus.ERROR, statusMessage = event.message, error = event.message) }
                    else -> Unit
                }
            }
        }
    }

    fun loadSessionHistory() {
        _uiState.update { it.copy(isLoadingHistory = true) }
        viewModelScope.launch {
            try {
                val result = backendApi.listChatSessions(limit = 30)
                val sessions = result.sessions.map { s ->
                    SessionDto(
                        sessionId = s.sessionId,
                        title = s.title,
                        capability = null,
                        updatedAt = s.updatedAt?.toLong(),
                        createdAt = s.createdAt?.toLong(),
                        messageCount = s.messageCount ?: s.messages.size,
                        status = null
                    )
                }
                _uiState.update { it.copy(sessions = sessions, isLoadingHistory = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoadingHistory = false, error = e.message) }
            }
        }
    }

    fun loadSession(sessionDto: SessionDto) {
        viewModelScope.launch {
            try {
                val full = backendApi.getChatSession(sessionDto.sessionId)
                val settings = full.settings ?: emptyMap()
                val kbNames = (settings["kb_names"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                val enableRag = settings["enable_rag"] as? Boolean ?: false
                val enableWeb = settings["enable_web_search"] as? Boolean ?: false
                val messages = full.messages.map { msg ->
                    ChatMessage(role = msg.role, content = msg.content)
                }
                _uiState.update {
                    AiChatUiState(
                        sessionId = full.sessionId,
                        enableRag = enableRag,
                        enableWebSearch = enableWeb,
                        selectedKbs = kbNames.toSet().ifEmpty { setOf("default") },
                        messages = messages
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
                backendApi.deleteChatSession(sessionId)
                _uiState.update { it.copy(sessions = it.sessions.filter { s -> s.sessionId != sessionId }) }
            } catch (_: Exception) {}
        }
    }

    fun toggleRag() = _uiState.update { it.copy(enableRag = !it.enableRag) }
    fun toggleWebSearch() = _uiState.update { it.copy(enableWebSearch = !it.enableWebSearch) }
    fun toggleKbSelection(name: String) {
        _uiState.update { state ->
            val current = state.selectedKbs.toMutableSet()
            if (current.contains(name)) {
                if (current.size > 1) current.remove(name) // Giữ lại ít nhất 1 cái hoặc cho phép rỗng tùy ý
            } else {
                current.add(name)
            }
            state.copy(selectedKbs = current)
        }
    }
    fun clearError() = _uiState.update { it.copy(error = null) }
    fun newSession() {
        streamingJob?.cancel()
        _uiState.update { AiChatUiState(enableRag = it.enableRag, enableWebSearch = it.enableWebSearch, selectedKbs = it.selectedKbs) }
    }

    override fun onCleared() {
        super.onCleared()
        streamingJob?.cancel()
    }
}
