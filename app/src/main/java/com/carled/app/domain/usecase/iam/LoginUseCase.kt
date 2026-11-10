package com.carled.app.domain.usecase.iam

import com.carled.app.core.common.Resource
import com.carled.app.domain.model.iam.AuthTokens
import com.carled.app.domain.repository.iam.IamRepository

class LoginUseCase(
    private val iamRepository: IamRepository
) {
    suspend operator fun invoke(email: String, password: String): Resource<AuthTokens> {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isBlank()) {
            return Resource.Error("Email không được để trống")
        }
        if (password.isBlank()) {
            return Resource.Error("Mật khẩu không được để trống")
        }
        return iamRepository.login(trimmedEmail, password)
    }
}
