package com.duong.udhoctap.feature.settings.presentation

import androidx.lifecycle.ViewModel
import com.duong.udhoctap.core.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class SettingsUiState(
    val notificationsEnabled: Boolean = true,
    val reminderHour: Int = 20,
    val darkTheme: Boolean = false,
    val dailyGoal: Int = 20
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            notificationsEnabled = settingsRepository.notificationsEnabled,
            reminderHour = settingsRepository.reminderHour,
            darkTheme = settingsRepository.darkTheme,
            dailyGoal = settingsRepository.dailyGoal
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

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
}
