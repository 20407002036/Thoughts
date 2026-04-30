package com.example.thoughts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

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
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = throwable.message ?: "Unable to sign in right now.",
                        )
                    }
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
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = throwable.message ?: "Unable to create your account right now.",
                        )
                    }
                }
        }
    }

    fun logout() {
        viewModelScope.launch {
            AuthRepository.logout(AuthSessionManager.session.value)
            AuthSessionManager.clearSession()
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