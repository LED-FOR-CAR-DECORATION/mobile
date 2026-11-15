package com.carled.app.presentation.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carled.app.core.common.Resource
import com.carled.app.domain.model.User
import com.carled.app.domain.usecase.auth.GoogleAuthUseCase
import com.carled.app.domain.usecase.auth.LoginUseCase
import com.carled.app.domain.usecase.auth.RegisterUseCase
import com.carled.app.domain.usecase.auth.ResendVerificationUseCase
import com.carled.app.domain.usecase.auth.VerifyEmailUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LoginViewModel(
    private val loginUseCase: LoginUseCase,
    private val registerUseCase: RegisterUseCase,
    private val verifyEmailUseCase: VerifyEmailUseCase,
    private val resendVerificationUseCase: ResendVerificationUseCase,
    private val googleAuthUseCase: GoogleAuthUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    // Mode

    fun toggleMode() = _uiState.update { it.withMode(!it.isRegisterMode) }

    fun setRegisterMode(isRegister: Boolean) = _uiState.update { it.withMode(isRegister) }

    private fun LoginUiState.withMode(isRegister: Boolean) = copy(
        isRegisterMode = isRegister,
        errorMessage = null,
        successMessage = null
    )

    // Input

    fun onEmailChange(newEmail: String) =
        _uiState.update { it.copy(email = newEmail, errorMessage = null) }

    fun onPasswordChange(newPassword: String) =
        _uiState.update { it.copy(password = newPassword, errorMessage = null) }

    fun onConfirmPasswordChange(newConfirmPassword: String) =
        _uiState.update { it.copy(confirmPassword = newConfirmPassword, errorMessage = null) }

    fun onRememberMeChange(checked: Boolean) =
        _uiState.update { it.copy(rememberMe = checked) }

    fun onAcceptTermsChange(checked: Boolean) =
        _uiState.update { it.copy(acceptTerms = checked, errorMessage = null) }

    fun togglePasswordVisibility() =
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }

    fun toggleConfirmPasswordVisibility() =
        _uiState.update { it.copy(isConfirmPasswordVisible = !it.isConfirmPasswordVisible) }

    // Actions

    fun submit(onSuccess: () -> Unit) {
        if (_uiState.value.isRegisterMode) register(onSuccess) else login(onSuccess)
    }

    fun login(onSuccess: () -> Unit) {
        val state = _uiState.value
        authenticate(onSuccess) {
            loginUseCase(state.email, state.password)
        }
    }

    fun loginWithGoogle(idToken: String, onSuccess: () -> Unit) {
        authenticate(onSuccess) {
            googleAuthUseCase(idToken)
        }
    }

    fun register(onSuccess: () -> Unit) {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, errorMessage = null, successMessage = null)
            }

            when (val result = registerUseCase(
                email = state.email,
                password = state.password,
                confirmPassword = state.confirmPassword,
                acceptTerms = state.acceptTerms
            )) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            showVerifyDialog = true,
                            verificationEmail = result.data,
                            verificationCode = "",
                            verifyError = null
                        )
                    }
                }
                is Resource.Error -> {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = result.message)
                    }
                }
                is Resource.Loading -> {
                    _uiState.update { it.copy(isLoading = true) }
                }
            }
        }
    }

    fun loginWithSocial(provider: String, onSuccess: () -> Unit) {
        authenticate(onSuccess) {
            loginUseCase("$provider$SOCIAL_EMAIL_SUFFIX", SOCIAL_PLACEHOLDER_PASSWORD)
        }
    }

    // Email verification (AuthCodeDialog)

    fun onAuthCodeChange(newCode: String) =
        _uiState.update { it.copy(verificationCode = newCode, verifyError = null) }

    fun submitVerifyEmail() {
        val state = _uiState.value
        val email = state.verificationEmail

        viewModelScope.launch {
            _uiState.update { it.copy(isVerifying = true, verifyError = null) }

            when (val result = verifyEmailUseCase(email, state.verificationCode)) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(
                            isVerifying = false,
                            showVerifyDialog = false,
                            isRegisterMode = false,
                            email = email,
                            password = "",
                            confirmPassword = "",
                            verificationCode = "",
                            verifyError = null,
                            successMessage = VERIFY_SUCCESS_MESSAGE
                        )
                    }
                }
                is Resource.Error -> {
                    _uiState.update {
                        it.copy(isVerifying = false, verifyError = result.message)
                    }
                }
                is Resource.Loading -> {
                    _uiState.update { it.copy(isVerifying = true) }
                }
            }
        }
    }

    fun resendVerificationCode() {
        val email = _uiState.value.verificationEmail

        viewModelScope.launch {
            _uiState.update { it.copy(isVerifying = true, verifyError = null) }

            when (val result = resendVerificationUseCase(email)) {
                is Resource.Success -> {
                    _uiState.update { it.copy(isVerifying = false, verifyError = null) }
                }
                is Resource.Error -> {
                    _uiState.update {
                        it.copy(isVerifying = false, verifyError = result.message)
                    }
                }
                is Resource.Loading -> {
                    _uiState.update { it.copy(isVerifying = true) }
                }
            }
        }
    }

    fun dismissVerifyDialog() = _uiState.update {
        it.copy(showVerifyDialog = false, verificationCode = "", verifyError = null)
    }

    // Auth func (for login / OAuth)

    private fun authenticate(
        onSuccess: () -> Unit,
        successMessage: String? = null,
        call: suspend () -> Resource<User>
    ) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, errorMessage = null, successMessage = null)
            }

            val state = _uiState.value

            when (val result = call()) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            loggedInUser = result.data,
                            successMessage = successMessage
                        )
                    }
                    onSuccess()
                }
                is Resource.Error -> {
                    val errorMessage = result.message ?: ""
                    if (errorMessage.contains("ACCOUNT_NOT_VERIFIED", ignoreCase = true) ||
                        errorMessage.contains("not verified", ignoreCase = true)
                    ) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                showVerifyDialog = true,
                                verificationEmail = state.email,
                                verificationCode = "",
                                verifyError = null,
                                errorMessage = null
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(isLoading = false, errorMessage = result.message)
                        }
                    }
                }
                is Resource.Loading -> _uiState.update { it.copy(isLoading = true) }
            }
        }
    }

    private companion object {
        const val VERIFY_SUCCESS_MESSAGE = "Xác thực email thành công! Vui lòng đăng nhập."
        const val SOCIAL_EMAIL_SUFFIX = ".user@carled.vn"
        const val SOCIAL_PLACEHOLDER_PASSWORD = "social_pass_123"
    }
}