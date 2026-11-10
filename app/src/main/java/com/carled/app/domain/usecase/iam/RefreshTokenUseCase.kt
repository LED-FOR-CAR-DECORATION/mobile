package com.carled.app.domain.usecase.iam

import com.carled.app.core.common.Resource
import com.carled.app.domain.model.iam.AuthTokens
import com.carled.app.domain.repository.iam.IamRepository

class RefreshTokenUseCase(
    private val iamRepository: IamRepository
) {
    suspend operator fun invoke(): Resource<AuthTokens> {
        return iamRepository.refreshToken()
    }
}
