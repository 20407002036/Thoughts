package com.example.thoughts

import kotlinx.serialization.Serializable

@Serializable
data class AuthLoginRequest(
    val email: String,
    val password: String,
)

@Serializable
data class AuthSignUpRequest(
    val email: String,
    val password: String,
    val displayName: String? = null,
)

@Serializable
data class AuthRefreshRequest(
    val refreshToken: String,
)

@Serializable
data class AuthLogoutRequest(
    val accessToken: String? = null,
    val refreshToken: String? = null,
)

data class AuthSession(
    val accessToken: String,
    val refreshToken: String? = null,
    val userId: String? = null,
    val email: String? = null,
    val displayName: String? = null,
)

data class AuthUiState(
    val displayName: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)