package com.carled.app.domain.usecase.auth

import com.carled.app.core.common.Resource
import com.carled.app.domain.model.User
import com.carled.app.domain.repository.AuthRepository

/**
 * PUT /api/users/me only accepts name, phone and avatar — email, credentials
 * and roles can't change here. [name] is required by the endpoint on every
 * call even when the caller only means to change [phone].
 */
class UpdateProfileUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(name: String, phone: String?): Resource<User> {
        if (name.isBlank()) {
            return Resource.Error("Thiếu tên tài khoản")
        }
        if (!phone.isNullOrBlank() && !PHONE_PATTERN.matches(phone)) {
            return Resource.Error("Số điện thoại không hợp lệ (8–15 số, có thể có dấu + ở đầu)")
        }
        return repository.updateProfile(name.trim(), phone?.trim()?.ifBlank { null })
    }

    private companion object {
        val PHONE_PATTERN = Regex("^\\+?[0-9]{8,15}$")
    }
}