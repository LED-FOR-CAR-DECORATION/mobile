package com.carled.app.presentation.screens.auth

import com.carled.app.domain.model.User

data class LoginUiState(
    val isRegisterMode: Boolean = false,
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val rememberMe: Boolean = true,
    val acceptTerms: Boolean = false,
    val isPasswordVisible: Boolean = false,
    val isConfirmPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val loggedInUser: User? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null
)
