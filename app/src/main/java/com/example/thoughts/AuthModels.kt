package com.example.thoughts

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class AuthLoginRequest(
    val email: String,
    val password: String,
)

@Serializable
data class AuthSignUpRequest(
    val email: String,
    val password: String,
    @SerialName("full_name")
    val displayName: String,
)

@Serializable
data class AuthRefreshRequest(
    @SerialName("refresh_token")
    val refreshToken: String,
)

@Serializable
class AuthLogoutRequest

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