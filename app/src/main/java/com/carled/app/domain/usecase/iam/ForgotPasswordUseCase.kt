package com.carled.app.domain.usecase.iam

import com.carled.app.core.common.Resource
import com.carled.app.domain.model.iam.PasswordResetResult
import com.carled.app.domain.repository.iam.IamRepository

class ForgotPasswordUseCase(
    private val iamRepository: IamRepository
) {
    suspend operator fun invoke(email: String): Resource<PasswordResetResult> {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isBlank()) {
            return Resource.Error("Email không được để trống")
        }
        return iamRepository.forgotPassword(trimmedEmail)
    }
}
