package com.duong.udhoctap.feature.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duong.udhoctap.core.network.BackendApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val username: String = "",
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isLoggedIn: Boolean = false,
    val accessToken: String? = null,
    val authMode: AuthMode = AuthMode.LOGIN
)

enum class AuthMode {
    LOGIN, SIGNUP
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val backendApi: BackendApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun setUsername(username: String) {
        _uiState.value = _uiState.value.copy(username = username)
    }

    fun setEmail(email: String) {
        _uiState.value = _uiState.value.copy(email = email)
    }

    fun setPassword(password: String) {
        _uiState.value = _uiState.value.copy(password = password)
    }

    fun setAuthMode(mode: AuthMode) {
        _uiState.value = _uiState.value.copy(
            authMode = mode,
            error = null,
            username = "",
            email = "",
            password = ""
        )
    }

    fun login() {
        if (!validateLoginInput()) return

        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)

                // TODO: Call backend API when implemented
                // val response = backendApi.login(LoginRequest(
                //     username = _uiState.value.username,
                //     password = _uiState.value.password
                // ))

                // For now, simulate successful login
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isLoggedIn = true,
                    accessToken = "mock_token_${System.currentTimeMillis()}",
                    error = null
                )

                // TODO: Store token securely in SharedPreferences or EncryptedSharedPreferences
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Login failed"
                )
            }
        }
    }

    fun signup() {
        if (!validateSignupInput()) return

        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)

                // TODO: Call backend API when implemented
                // val response = backendApi.signup(SignupRequest(
                //     username = _uiState.value.username,
                //     email = _uiState.value.email,
                //     password = _uiState.value.password
                // ))

                // For now, simulate successful signup
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isLoggedIn = true,
                    accessToken = "mock_token_${System.currentTimeMillis()}",
                    error = null
                )

                // TODO: Store token securely
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Signup failed"
                )
            }
        }
    }

    fun logout() {
        _uiState.value = AuthUiState()
        // TODO: Remove token from secure storage
    }

    private fun validateLoginInput(): Boolean {
        val state = _uiState.value
        return when {
            state.username.isBlank() -> {
                _uiState.value = state.copy(error = "Username is required")
                false
            }
            state.password.isBlank() -> {
                _uiState.value = state.copy(error = "Password is required")
                false
            }
            else -> true
        }
    }

    private fun validateSignupInput(): Boolean {
        val state = _uiState.value
        return when {
            state.username.isBlank() -> {
                _uiState.value = state.copy(error = "Username is required")
                false
            }
            state.email.isBlank() -> {
                _uiState.value = state.copy(error = "Email is required")
                false
            }
            !state.email.contains("@") -> {
                _uiState.value = state.copy(error = "Invalid email address")
                false
            }
            state.password.isBlank() -> {
                _uiState.value = state.copy(error = "Password is required")
                false
            }
            state.password.length < 6 -> {
                _uiState.value = state.copy(error = "Password must be at least 6 characters")
                false
            }
            else -> true
        }
    }
}
