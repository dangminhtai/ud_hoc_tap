package com.duong.udhoctap.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duong.udhoctap.core.data.repository.KnowledgeBaseRepository
import com.duong.udhoctap.core.data.repository.SettingsRepository
import com.duong.udhoctap.core.network.BackendApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ConnectionStatus { IDLE, TESTING, OK, ERROR }

data class SettingsUiState(
    // App
    val notificationsEnabled: Boolean = true,
    val reminderHour: Int = 20,
    val darkTheme: Boolean = false,
    val dailyGoal: Int = 20,
    // Backend
    val backendUrl: String = SettingsRepository.DEFAULT_BACKEND_URL,
    val connectionStatus: ConnectionStatus = ConnectionStatus.IDLE,
    // AI
    val defaultKbName: String = "",
    val availableKbs: List<String> = emptyList(),
    val defaultEnableRag: Boolean = false,
    val defaultEnableWebSearch: Boolean = false,
    // Language
    val language: String = "vi"
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val kbRepository: KnowledgeBaseRepository,
    private val backendApi: BackendApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            notificationsEnabled = settingsRepository.notificationsEnabled,
            reminderHour = settingsRepository.reminderHour,
            darkTheme = settingsRepository.darkTheme,
            dailyGoal = settingsRepository.dailyGoal,
            backendUrl = settingsRepository.backendUrl,
            defaultKbName = settingsRepository.defaultKbName,
            defaultEnableRag = settingsRepository.defaultEnableRag,
            defaultEnableWebSearch = settingsRepository.defaultEnableWebSearch,
            language = settingsRepository.language
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadKbs()
    }

    private fun loadKbs() {
        viewModelScope.launch {
            val kbs = kbRepository.listKbs().map { it.name }
            _uiState.value = _uiState.value.copy(availableKbs = kbs)
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        settingsRepository.notificationsEnabled = enabled
        _uiState.value = _uiState.value.copy(notificationsEnabled = enabled)
    }

    fun setReminderHour(hour: Int) {
        settingsRepository.reminderHour = hour
        _uiState.value = _uiState.value.copy(reminderHour = hour)
    }

    fun setDarkTheme(dark: Boolean) {
        settingsRepository.darkTheme = dark
        _uiState.value = _uiState.value.copy(darkTheme = dark)
    }

    fun setDailyGoal(goal: Int) {
        settingsRepository.dailyGoal = goal
        _uiState.value = _uiState.value.copy(dailyGoal = goal)
    }

    fun setBackendUrl(url: String) {
        settingsRepository.backendUrl = url
        _uiState.value = _uiState.value.copy(backendUrl = url, connectionStatus = ConnectionStatus.IDLE)
    }

    fun testConnection() {
        _uiState.value = _uiState.value.copy(connectionStatus = ConnectionStatus.TESTING)
        viewModelScope.launch {
            val status = try {
                backendApi.getSystemStatus()
                ConnectionStatus.OK
            } catch (e: Exception) {
                ConnectionStatus.ERROR
            }
            _uiState.value = _uiState.value.copy(connectionStatus = status)
        }
    }

    fun setDefaultKb(name: String) {
        settingsRepository.defaultKbName = name
        _uiState.value = _uiState.value.copy(defaultKbName = name)
        viewModelScope.launch {
            try { kbRepository.setDefault(name) } catch (_: Exception) {}
        }
    }

    fun setDefaultEnableRag(enabled: Boolean) {
        settingsRepository.defaultEnableRag = enabled
        _uiState.value = _uiState.value.copy(defaultEnableRag = enabled)
    }

    fun setDefaultEnableWebSearch(enabled: Boolean) {
        settingsRepository.defaultEnableWebSearch = enabled
        _uiState.value = _uiState.value.copy(defaultEnableWebSearch = enabled)
    }

    fun setLanguage(lang: String) {
        settingsRepository.language = lang
        _uiState.value = _uiState.value.copy(language = lang)
    }
}
