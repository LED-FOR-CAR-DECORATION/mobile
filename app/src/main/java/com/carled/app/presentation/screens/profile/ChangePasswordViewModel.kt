package com.carled.app.presentation.screens.profile

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
 * There is no authenticated "change password" endpoint on the backend — the
 * only server-side path that changes a password is the public OTP pair
 * (POST /api/auth/forgot-password, POST /api/auth/reset-password), the same
 * one used for the logged-out "forgot password" flow. So this screen reuses
 * that pair instead: submitting the form sends an OTP to [userEmail], the
 * UI pops the same AuthCodeDialog used during registration, and the password
 * is only actually changed once the OTP is verified via [resetPasswordUseCase].
 *
 * Note: the old-password field is kept only to match the designed screen and
 * confirm the user knows their current password — the API has no field for
 * it and never receives it, since reset-password only takes the OTP + new
 * password.
 *
 * If the user dismisses the OTP dialog without entering a valid code,
 * resetPassword is never called, so the password is left unchanged. To try
 * again they resubmit the form, which requests a fresh OTP.
 */
class ChangePasswordViewModel(
    private val userEmail: String,
    private val forgotPasswordUseCase: ForgotPasswordUseCase,
    private val resetPasswordUseCase: ResetPasswordUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChangePasswordUiState())
    val uiState: StateFlow<ChangePasswordUiState> = _uiState.asStateFlow()

    // Input

    fun onOldPasswordChange(value: String) =
        _uiState.update { it.copy(oldPassword = value, errorMessage = null) }

    fun onNewPasswordChange(value: String) =
        _uiState.update { it.copy(newPassword = value, errorMessage = null) }

    fun onConfirmPasswordChange(value: String) =
        _uiState.update { it.copy(confirmPassword = value, errorMessage = null) }

    /** Clears the form — defends against a stale showVerifyDialog=true if this
     * instance is ever reused across visits to the screen. */
    fun reset() {
        _uiState.value = ChangePasswordUiState()
    }

    // Step 1: validate locally, then request an OTP

    fun submit() {
        val state = _uiState.value

        if (state.oldPassword.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Vui lòng nhập mật khẩu hiện tại") }
            return
        }
        if (state.newPassword != state.confirmPassword) {
            _uiState.update { it.copy(errorMessage = "Mật khẩu xác nhận không khớp") }
            return
        }
        if (state.newPassword == state.oldPassword) {
            _uiState.update { it.copy(errorMessage = "Mật khẩu mới phải khác mật khẩu hiện tại") }
            return
        }
        if (!isStrongPassword(state.newPassword)) {
            _uiState.update { it.copy(errorMessage = PASSWORD_RULES_MESSAGE) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }

            when (val result = forgotPasswordUseCase(userEmail)) {
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

            when (val result = forgotPasswordUseCase(userEmail)) {
                is Resource.Success -> _uiState.update { it.copy(isVerifying = false, verifyError = null) }
                is Resource.Error -> _uiState.update { it.copy(isVerifying = false, verifyError = result.message) }
                is Resource.Loading -> _uiState.update { it.copy(isVerifying = true) }
            }
        }
    }

    fun onVerificationCodeChange(code: String) =
        _uiState.update { it.copy(verificationCode = code, verifyError = null) }

    // Step 2: confirm the OTP, which is the call that actually changes the password

    fun confirmChangePassword() {
        val state = _uiState.value

        viewModelScope.launch {
            _uiState.update { it.copy(isVerifying = true, verifyError = null) }

            when (val result = resetPasswordUseCase(
                email = userEmail,
                otp = state.verificationCode,
                newPassword = state.newPassword,
                confirmPassword = state.confirmPassword
            )) {
                is Resource.Success -> _uiState.update {
                    ChangePasswordUiState(successMessage = "Đổi mật khẩu thành công!")
                }
                is Resource.Error -> _uiState.update {
                    it.copy(isVerifying = false, verifyError = result.message)
                }
                is Resource.Loading -> _uiState.update { it.copy(isVerifying = true) }
            }
        }
    }

    /** Closing the OTP step without verifying leaves the password unchanged. */
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