package com.carled.app.domain.repository

import com.carled.app.core.common.Resource
import com.carled.app.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    /** Hot flow of the currently authenticated user; emits null when logged out. */
    fun getCurrentUser(): Flow<User?>

    suspend fun login(email: String, password: String): Resource<User>

    /**
     * Creates a new account. Returns the registered email on success so the
     * caller can navigate straight to the email-verification screen.
     * The user is NOT logged in after this call — they must verify their email first.
     */
    suspend fun register(
        email: String,
        password: String,
        confirmPassword: String
    ): Resource<String>

    suspend fun verifyEmail(email: String, otp: String): Resource<Unit>

    suspend fun resendVerification(email: String): Resource<Unit>

    suspend fun forgotPassword(email: String): Resource<Unit>

    suspend fun resetPassword(
        email: String,
        otp: String,
        newPassword: String,
        confirmPassword: String
    ): Resource<Unit>

    suspend fun refreshToken(refreshToken: String): Resource<User>

    suspend fun googleAuth(idToken: String): Resource<User>

    /**
     * Fetches GET /api/users/me for the currently signed-in user and refreshes
     * getCurrentUser()'s emitted value. Useful to call again later (e.g. after
     * editing the profile elsewhere); login() already calls this once internally
     * as a best-effort enrichment, so callers don't need to call it right after login.
     */
    suspend fun getProfile(): Resource<User>

    /** Invalidates the session on the server and clears local credentials. */
    suspend fun logout(): Resource<Unit>

    suspend fun isLoggedIn(): Boolean
}