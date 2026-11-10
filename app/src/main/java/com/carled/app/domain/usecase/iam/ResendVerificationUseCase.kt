package com.carled.app.domain.usecase.iam

import com.carled.app.core.common.Resource
import com.carled.app.domain.repository.iam.IamRepository

class ResendVerificationUseCase(
    private val iamRepository: IamRepository
) {
    suspend operator fun invoke(email: String): Resource<Unit> {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isBlank()) {
            return Resource.Error("Email không được để trống")
        }
        return iamRepository.resendVerification(trimmedEmail)
    }
}
