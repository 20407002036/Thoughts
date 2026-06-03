package com.example.thoughts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.thoughts.ui.events.UiEvent
import com.example.thoughts.ui.popup.PopupKind
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _uiEvents = MutableSharedFlow<UiEvent>(extraBufferCapacity = 8)
    val uiEvents = _uiEvents.asSharedFlow()

    fun updateDisplayName(value: String) {
        _uiState.update { it.copy(displayName = value, errorMessage = null) }
    }

    fun updateEmail(value: String) {
        _uiState.update { it.copy(email = value, errorMessage = null) }
    }

    fun updatePassword(value: String) {
        _uiState.update { it.copy(password = value, errorMessage = null) }
    }

    fun updateConfirmPassword(value: String) {
        _uiState.update { it.copy(confirmPassword = value, errorMessage = null) }
    }

    fun login() {
        val current = _uiState.value
        val validationError = validateLogin(current.email, current.password)
        if (validationError != null) {
            _uiState.update { it.copy(errorMessage = validationError) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            AuthRepository.login(current.email, current.password)
                .onSuccess { session ->
                    AuthSessionManager.saveSession(session)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = null,
                            password = "",
                            confirmPassword = "",
                        )
                    }

                    _uiEvents.tryEmit(
                        UiEvent.Toast(
                            message = "Welcome back!",
                            kind = PopupKind.Success,
                        )
                    )
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = throwable.message ?: "Unable to sign in right now.",
                        )
                    }

                    _uiEvents.tryEmit(
                        UiEvent.Toast(
                            message = throwable.message ?: "Authentication failed",
                            kind = PopupKind.Error,
                        )
                    )
                }
        }
    }

    fun signUp() {
        val current = _uiState.value
        val validationError = validateSignUp(
            displayName = current.displayName,
            email = current.email,
            password = current.password,
            confirmPassword = current.confirmPassword,
        )
        if (validationError != null) {
            _uiState.update { it.copy(errorMessage = validationError) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            AuthRepository.signUp(
                email = current.email,
                password = current.password,
                displayName = current.displayName,
            )
                .onSuccess { session ->
                    AuthSessionManager.saveSession(session)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = null,
                            password = "",
                            confirmPassword = "",
                        )
                    }

                    _uiEvents.tryEmit(
                        UiEvent.Toast(
                            message = "Account created successfully!",
                            kind = PopupKind.Success,
                        )
                    )
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = throwable.message ?: "Unable to create your account right now.",
                        )
                    }

                    _uiEvents.tryEmit(
                        UiEvent.Toast(
                            message = throwable.message ?: "Account creation failed",
                            kind = PopupKind.Error,
                        )
                    )
                }
        }
    }

    fun logout() {
        viewModelScope.launch {
            val currentSession = AuthSessionManager.session.value
            AuthSessionManager.clearSession()

            val result = AuthRepository.logout(currentSession)
            _uiEvents.tryEmit(
                UiEvent.Toast(
                    message = if (result.isSuccess) {
                        "Signed out"
                    } else {
                        "Signed out locally; couldn't reach server"
                    },
                    kind = if (result.isSuccess) PopupKind.Success else PopupKind.Error,
                )
            )

            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = null,
                    password = "",
                    confirmPassword = "",
                )
            }
        }
    }

    private fun validateLogin(email: String, password: String): String? {
        if (email.isBlank()) return "Enter your email address."
        if (!email.contains("@")) return "Enter a valid email address."
        if (password.length < 6) return "Use at least 6 characters for your password."
        return null
    }

    private fun validateSignUp(
        displayName: String,
        email: String,
        password: String,
        confirmPassword: String,
    ): String? {
        if (displayName.isBlank()) return "Enter your name."
        if (email.isBlank()) return "Enter your email address."
        if (!email.contains("@")) return "Enter a valid email address."
        if (password.length < 6) return "Use at least 6 characters for your password."
        if (password != confirmPassword) return "Passwords do not match."
        return null
    }
}