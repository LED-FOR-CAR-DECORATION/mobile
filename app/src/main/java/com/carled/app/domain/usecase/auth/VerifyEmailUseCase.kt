package com.carled.app.domain.usecase.auth

import com.carled.app.core.common.Resource
import com.carled.app.domain.repository.AuthRepository

class VerifyEmailUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(email: String, otp: String): Resource<Unit> {
        if (email.isBlank()) {
            return Resource.Error("Email không được để trống")
        }
        if (otp.isBlank()) {
            return Resource.Error("Vui lòng nhập mã xác thực")
        }
        return repository.verifyEmail(email.trim(), otp.trim())
    }
}