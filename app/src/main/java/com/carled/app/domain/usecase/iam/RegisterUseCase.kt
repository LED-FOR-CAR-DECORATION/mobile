package com.carled.app.domain.usecase.iam

import com.carled.app.core.common.Resource
import com.carled.app.domain.model.iam.RegisterResult
import com.carled.app.domain.repository.iam.IamRepository

class RegisterUseCase(
    private val iamRepository: IamRepository
) {
    suspend operator fun invoke(
        email: String,
        password: String,
        confirmPassword: String
    ): Resource<RegisterResult> {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isBlank()) {
            return Resource.Error("Email không được để trống")
        }
        if (password.length < 8) {
            return Resource.Error("Mật khẩu phải chứa ít nhất 8 ký tự")
        }
        if (password != confirmPassword) {
            return Resource.Error("Mật khẩu xác nhận không khớp")
        }
        return iamRepository.register(trimmedEmail, password, confirmPassword)
    }
}
