package com.carled.app.domain.usecase.auth

import com.carled.app.core.common.Resource
import com.carled.app.domain.repository.AuthRepository

/**
 * Requests a 6-digit email OTP for a password reset/change.
 * Public endpoint server-side — the account doesn't need to already be
 * logged in, and the server returns the same acknowledgement whether or
 * not the email exists (no account-enumeration leak).
 */
class ForgotPasswordUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(email: String): Resource<Unit> {
        if (email.isBlank()) {
            return Resource.Error("Email không được để trống")
        }
        return repository.forgotPassword(email.trim())
    }
}