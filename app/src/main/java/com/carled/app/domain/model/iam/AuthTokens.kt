package com.carled.app.domain.model.iam

/**
 * Domain entity đại diện cho cặp JWT Token của phiên đăng nhập.
 */
data class AuthTokens(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer",
    val expiresIn: Long = 900L
)
