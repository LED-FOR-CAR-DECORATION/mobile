package com.carled.app.domain.usecase.iam

import com.carled.app.core.common.Resource
import com.carled.app.domain.model.iam.AuthTokens
import com.carled.app.domain.repository.iam.IamRepository

class GoogleLoginUseCase(
    private val iamRepository: IamRepository
) {
    suspend operator fun invoke(idToken: String): Resource<AuthTokens> {
        val trimmedToken = idToken.trim()
        if (trimmedToken.isBlank()) {
            return Resource.Error("Google ID Token không được để trống")
        }
        return iamRepository.googleLogin(trimmedToken)
    }
}
