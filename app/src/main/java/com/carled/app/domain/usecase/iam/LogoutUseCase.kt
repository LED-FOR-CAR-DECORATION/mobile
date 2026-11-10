package com.carled.app.domain.usecase.iam

import com.carled.app.core.common.Resource
import com.carled.app.domain.repository.iam.IamRepository

class LogoutUseCase(
    private val iamRepository: IamRepository
) {
    suspend operator fun invoke(): Resource<Unit> {
        return iamRepository.logout()
    }
}
