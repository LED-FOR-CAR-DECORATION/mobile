package com.carled.app.domain.usecase.auth

import com.carled.app.core.common.Resource
import com.carled.app.domain.model.User
import com.carled.app.domain.repository.AuthRepository

class GoogleAuthUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke(idToken: String): Resource<User> {
        return authRepository.googleAuth(idToken)
    }
}