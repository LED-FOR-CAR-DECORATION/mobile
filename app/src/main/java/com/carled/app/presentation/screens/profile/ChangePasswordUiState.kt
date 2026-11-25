package com.carled.app.presentation.screens.profile

data class ChangePasswordUiState(
    val oldPassword: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,

    // Email-OTP step (the server has no "change while logged in" endpoint —
    // every password change goes through forgot-password -> reset-password).
    val showVerifyDialog: Boolean = false,
    val verificationCode: String = "",
    val isVerifying: Boolean = false,
    val verifyError: String? = null
)