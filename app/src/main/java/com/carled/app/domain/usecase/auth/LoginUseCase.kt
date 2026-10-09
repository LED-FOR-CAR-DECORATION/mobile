package com.carled.app.domain.usecase.auth

import com.carled.app.core.common.Resource
import com.carled.app.domain.model.User
import com.carled.app.domain.repository.AuthRepository

class LoginUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(email: String, password: String): Resource<User> {
        if (email.isBlank() || password.isBlank()) {
            return Resource.Error("Email và mật khẩu không được để trống")
        }
        return repository.login(email.trim(), password)
    }
}
