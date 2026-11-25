package com.carled.app.presentation.screens.auth

data class ForgotPasswordUiState(
    val email: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,

    // Email-OTP step (forgot-password -> reset-password), same mechanism as
    // ChangePasswordViewModel, just with the email collected here instead of
    // coming from an already-logged-in user.
    val showVerifyDialog: Boolean = false,
    val verificationCode: String = "",
    val isVerifying: Boolean = false,
    val verifyError: String? = null
)