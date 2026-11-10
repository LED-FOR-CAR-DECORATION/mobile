package com.carled.app.domain.usecase.iam

import com.carled.app.core.common.Resource
import com.carled.app.domain.repository.iam.IamRepository

class VerifyEmailUseCase(
    private val iamRepository: IamRepository
) {
    suspend operator fun invoke(email: String, otp: String): Resource<String> {
        val trimmedEmail = email.trim()
        val trimmedOtp = otp.trim()
        if (trimmedEmail.isBlank()) {
            return Resource.Error("Email không được để trống")
        }
        if (trimmedOtp.length != 6) {
            return Resource.Error("Mã OTP phải có đúng 6 chữ số")
        }
        return iamRepository.verifyEmail(trimmedEmail, trimmedOtp)
    }
}
