package com.carled.app.presentation.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carled.app.core.common.Resource
import com.carled.app.domain.usecase.auth.ForgotPasswordUseCase
import com.carled.app.domain.usecase.auth.ResetPasswordUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Logged-out password reset. Same OTP pair as the account-security
 * ChangePasswordViewModel (forgot-password -> reset-password) — the account
 * isn't known yet here, so email is part of the form instead of being
 * supplied by the caller.
 */
class ForgotPasswordViewModel(
    private val forgotPasswordUseCase: ForgotPasswordUseCase,
    private val resetPasswordUseCase: ResetPasswordUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ForgotPasswordUiState())
    val uiState: StateFlow<ForgotPasswordUiState> = _uiState.asStateFlow()

    fun onEmailChange(value: String) =
        _uiState.update { it.copy(email = value, errorMessage = null) }

    fun onNewPasswordChange(value: String) =
        _uiState.update { it.copy(newPassword = value, errorMessage = null) }

    fun onConfirmPasswordChange(value: String) =
        _uiState.update { it.copy(confirmPassword = value, errorMessage = null) }

    /** Clears the form — call when the sheet is reopened after a previous attempt. */
    fun reset() {
        _uiState.value = ForgotPasswordUiState()
    }

    // Step 1: validate locally (so we don't burn an OTP request on an obviously
    // bad password), then request the OTP.
    fun submit() {
        val state = _uiState.value
        val email = state.email.trim()

        if (email.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Vui lòng nhập email") }
            return
        }
        if (state.newPassword != state.confirmPassword) {
            _uiState.update { it.copy(errorMessage = "Mật khẩu xác nhận không khớp") }
            return
        }
        if (!isStrongPassword(state.newPassword)) {
            _uiState.update { it.copy(errorMessage = PASSWORD_RULES_MESSAGE) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            when (val result = forgotPasswordUseCase(email)) {
                is Resource.Success -> _uiState.update {
                    it.copy(isLoading = false, showVerifyDialog = true, verificationCode = "", verifyError = null)
                }
                is Resource.Error -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.message)
                }
                is Resource.Loading -> _uiState.update { it.copy(isLoading = true) }
            }
        }
    }

    fun resendCode() {
        viewModelScope.launch {
            _uiState.update { it.copy(isVerifying = true, verifyError = null) }

            when (val result = forgotPasswordUseCase(_uiState.value.email.trim())) {
                is Resource.Success -> _uiState.update { it.copy(isVerifying = false, verifyError = null) }
                is Resource.Error -> _uiState.update { it.copy(isVerifying = false, verifyError = result.message) }
                is Resource.Loading -> _uiState.update { it.copy(isVerifying = true) }
            }
        }
    }

    fun onVerificationCodeChange(code: String) =
        _uiState.update { it.copy(verificationCode = code, verifyError = null) }

    // Step 2: confirming the OTP is what actually resets the password server-side.
    fun confirmReset(onSuccess: (email: String) -> Unit) {
        val state = _uiState.value
        val email = state.email.trim()

        viewModelScope.launch {
            _uiState.update { it.copy(isVerifying = true, verifyError = null) }

            when (val result = resetPasswordUseCase(
                email = email,
                otp = state.verificationCode,
                newPassword = state.newPassword,
                confirmPassword = state.confirmPassword
            )) {
                is Resource.Success -> {
                    _uiState.update { it.copy(isVerifying = false) }
                    onSuccess(email)
                }
                is Resource.Error -> _uiState.update {
                    it.copy(isVerifying = false, verifyError = result.message)
                }
                is Resource.Loading -> _uiState.update { it.copy(isVerifying = true) }
            }
        }
    }

    /** Closing without verifying leaves the old password in place. */
    fun dismissVerifyDialog() = _uiState.update {
        it.copy(showVerifyDialog = false, verificationCode = "", verifyError = null)
    }

    private fun isStrongPassword(password: String): Boolean =
        password.length >= 8 &&
                password.any { it.isUpperCase() } &&
                password.any { it.isLowerCase() } &&
                password.any { it.isDigit() } &&
                password.any { it in SPECIAL_CHARS } &&
                password.none { it.isWhitespace() }

    private companion object {
        const val SPECIAL_CHARS = "!@#\$%^&*()_+-=[]{}|;':\",./<>?"
        const val PASSWORD_RULES_MESSAGE =
            "Mật khẩu phải có ít nhất 8 ký tự, gồm chữ hoa, chữ thường, chữ số và ký tự đặc biệt"
    }
}