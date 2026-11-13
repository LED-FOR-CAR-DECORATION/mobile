package com.carled.app.domain.usecase.auth

import com.carled.app.core.common.Resource
import com.carled.app.domain.repository.AuthRepository

class RegisterUseCase(private val repository: AuthRepository) {


    suspend operator fun invoke(
        email: String,
        password: String,
        confirmPassword: String,
        acceptTerms: Boolean
    ): Resource<String> {
        val trimmedEmail = email.trim()

        if (trimmedEmail.isBlank()) {
            return Resource.Error("Vui lòng nhập Email hoặc Số điện thoại")
        }
        if (password.isBlank()) {
            return Resource.Error("Vui lòng nhập mật khẩu")
        }
        if (password.length < 8) {
            return Resource.Error("Mật khẩu phải có ít nhất 8 ký tự")
        }
        if (!password.any { it.isUpperCase() }) {
            return Resource.Error("Mật khẩu phải chứa ít nhất một chữ hoa")
        }
        if (!password.any { it.isLowerCase() }) {
            return Resource.Error("Mật khẩu phải chứa ít nhất một chữ thường")
        }
        if (!password.any { it.isDigit() }) {
            return Resource.Error("Mật khẩu phải chứa ít nhất một chữ số")
        }
        if (!password.any { it in SPECIAL_CHARS }) {
            return Resource.Error("Mật khẩu phải chứa ít nhất một ký tự đặc biệt (!@#\$%^&*...)")
        }
        if (password.any { it.isWhitespace() }) {
            return Resource.Error("Mật khẩu không được chứa khoảng trắng")
        }
        if (password != confirmPassword) {
            return Resource.Error("Mật khẩu xác nhận không khớp")
        }
        if (!acceptTerms) {
            return Resource.Error("Vui lòng đồng ý với Điều khoản dịch vụ")
        }

        return repository.register(trimmedEmail, password, confirmPassword)
    }

    companion object {
        private const val SPECIAL_CHARS = "!@#\$%^&*()_+-=[]{}|;':\",./<>?"
    }
}