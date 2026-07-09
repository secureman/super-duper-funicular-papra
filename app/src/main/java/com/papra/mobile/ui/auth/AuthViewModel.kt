package com.papra.mobile.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.papra.mobile.data.repository.AuthRepository
import com.papra.mobile.data.repository.AuthResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class AuthMethod { API_KEY, EMAIL_PASSWORD }

data class AuthUiState(
    val serverUrl: String = "",
    val method: AuthMethod = AuthMethod.EMAIL_PASSWORD,
    val email: String = "",
    val password: String = "",
    val apiKey: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val signedIn: Boolean = false,
)

class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun onServerUrlChange(value: String) = _uiState.update { it.copy(serverUrl = value, error = null) }
    fun onMethodChange(method: AuthMethod) = _uiState.update { it.copy(method = method, error = null) }
    fun onEmailChange(value: String) = _uiState.update { it.copy(email = value, error = null) }
    fun onPasswordChange(value: String) = _uiState.update { it.copy(password = value, error = null) }
    fun onApiKeyChange(value: String) = _uiState.update { it.copy(apiKey = value, error = null) }

    fun submit() {
        val state = _uiState.value
        if (state.serverUrl.isBlank()) {
            _uiState.update { it.copy(error = "Enter your Papra server URL") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            authRepository.setServerUrl(state.serverUrl)

            val result = when (state.method) {
                AuthMethod.API_KEY -> authRepository.signInWithApiKey(state.apiKey)
                AuthMethod.EMAIL_PASSWORD -> authRepository.signInWithEmail(state.email, state.password)
            }

            when (result) {
                is AuthResult.Success -> _uiState.update { it.copy(isLoading = false, signedIn = true) }
                is AuthResult.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
            }
        }
    }

    private inline fun MutableStateFlow<AuthUiState>.update(block: (AuthUiState) -> AuthUiState) {
        value = block(value)
    }
}
