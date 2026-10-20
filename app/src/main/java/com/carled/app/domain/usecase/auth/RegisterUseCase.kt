package com.carled.app.domain.usecase.auth

import com.carled.app.core.common.Resource
import com.carled.app.domain.model.User
import com.carled.app.domain.repository.AuthRepository

class RegisterUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(
        email: String,
        password: String,
        confirmPassword: String,
        acceptTerms: Boolean
    ): Resource<User> {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isBlank()) {
            return Resource.Error("Vui lòng nhập Email hoặc Số điện thoại")
        }
        if (password.isBlank()) {
            return Resource.Error("Vui lòng nhập mật khẩu")
        }
        if (password.length < 6) {
            return Resource.Error("Mật khẩu phải có ít nhất 6 ký tự")
        }
        if (password != confirmPassword) {
            return Resource.Error("Mật khẩu xác nhận không khớp")
        }
        if (!acceptTerms) {
            return Resource.Error("Vui lòng đồng ý với Điều khoản dịch vụ")
        }
        return repository.register(trimmedEmail, password)
    }
}
