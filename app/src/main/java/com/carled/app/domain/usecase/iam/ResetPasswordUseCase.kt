package com.carled.app.domain.usecase.iam

import com.carled.app.core.common.Resource
import com.carled.app.domain.model.iam.PasswordResetResult
import com.carled.app.domain.repository.iam.IamRepository

class ResetPasswordUseCase(
    private val iamRepository: IamRepository
) {
    suspend operator fun invoke(
        email: String,
        otp: String,
        newPassword: String,
        confirmPassword: String? = null
    ): Resource<PasswordResetResult> {
        val trimmedEmail = email.trim()
        val trimmedOtp = otp.trim()
        if (trimmedEmail.isBlank()) {
            return Resource.Error("Email không được để trống")
        }
        if (trimmedOtp.length != 6) {
            return Resource.Error("Mã OTP phải có đúng 6 chữ số")
        }
        if (newPassword.length < 8) {
            return Resource.Error("Mật khẩu mới phải có ít nhất 8 ký tự")
        }
        if (confirmPassword != null && newPassword != confirmPassword) {
            return Resource.Error("Mật khẩu xác nhận không khớp")
        }
        return iamRepository.resetPassword(trimmedEmail, trimmedOtp, newPassword, confirmPassword)
    }
}
