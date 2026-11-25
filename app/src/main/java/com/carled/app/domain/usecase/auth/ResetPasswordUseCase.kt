package com.carled.app.domain.usecase.auth

import com.carled.app.core.common.Resource
import com.carled.app.domain.repository.AuthRepository

/**
 * Completes a password change/reset using the OTP sent by [ForgotPasswordUseCase].
 * This is the only server-side path that actually changes the password — the
 * server validates the OTP against Redis and revokes all existing sessions.
 */
class ResetPasswordUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(
        email: String,
        otp: String,
        newPassword: String,
        confirmPassword: String
    ): Resource<Unit> {
        if (otp.isBlank()) {
            return Resource.Error("Vui lòng nhập mã xác thực")
        }
        if (newPassword.length < 8) {
            return Resource.Error("Mật khẩu phải có ít nhất 8 ký tự")
        }
        if (!newPassword.any { it.isUpperCase() }) {
            return Resource.Error("Mật khẩu phải chứa ít nhất một chữ hoa")
        }
        if (!newPassword.any { it.isLowerCase() }) {
            return Resource.Error("Mật khẩu phải chứa ít nhất một chữ thường")
        }
        if (!newPassword.any { it.isDigit() }) {
            return Resource.Error("Mật khẩu phải chứa ít nhất một chữ số")
        }
        if (!newPassword.any { it in SPECIAL_CHARS }) {
            return Resource.Error("Mật khẩu phải chứa ít nhất một ký tự đặc biệt (!@#\$%^&*...)")
        }
        if (newPassword.any { it.isWhitespace() }) {
            return Resource.Error("Mật khẩu không được chứa khoảng trắng")
        }
        if (newPassword != confirmPassword) {
            return Resource.Error("Mật khẩu xác nhận không khớp")
        }
        return repository.resetPassword(email.trim(), otp.trim(), newPassword, confirmPassword)
    }

    private companion object {
        const val SPECIAL_CHARS = "!@#\$%^&*()_+-=[]{}|;':\",./<>?"
    }
}