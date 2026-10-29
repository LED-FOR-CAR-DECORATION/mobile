package com.carled.app.presentation.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carled.app.core.common.Resource
import com.carled.app.domain.model.User // TODO: adjust to the actual type of LoginUiState.loggedInUser
import com.carled.app.domain.usecase.auth.LoginUseCase
import com.carled.app.domain.usecase.auth.RegisterUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LoginViewModel(
    private val loginUseCase: LoginUseCase,
    private val registerUseCase: RegisterUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    //Mode

    fun toggleMode() = _uiState.update { it.withMode(!it.isRegisterMode) }

    fun setRegisterMode(isRegister: Boolean) = _uiState.update { it.withMode(isRegister) }

    private fun LoginUiState.withMode(isRegister: Boolean) = copy(
        isRegisterMode = isRegister,
        errorMessage = null,
        successMessage = null
    )


    //Input
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


    //Actions

    fun submit(onSuccess: () -> Unit) {
        if (_uiState.value.isRegisterMode) register(onSuccess) else login(onSuccess)
    }

    fun login(onSuccess: () -> Unit) {
        val state = _uiState.value
        authenticate(onSuccess) {
            loginUseCase(state.email, state.password)
        }
    }

    fun register(onSuccess: () -> Unit) {
        val state = _uiState.value
        authenticate(onSuccess, successMessage = REGISTER_SUCCESS_MESSAGE) {
            registerUseCase(state.email, state.password, state.confirmPassword, state.acceptTerms)
        }
    }

    fun loginWithSocial(provider: String, onSuccess: () -> Unit) {
        // TODO: replace with real OAuth before release
        authenticate(onSuccess) {
            loginUseCase("$provider$SOCIAL_EMAIL_SUFFIX", SOCIAL_PLACEHOLDER_PASSWORD)
        }
    }

    // Auth func

    private fun authenticate(
        onSuccess: () -> Unit,
        successMessage: String? = null,
        call: suspend () -> Resource<User>
    ) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, errorMessage = null, successMessage = null)
            }

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
                is Resource.Error -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.message)
                }
                is Resource.Loading -> _uiState.update { it.copy(isLoading = true) }
            }
        }
    }

    private companion object {
        const val REGISTER_SUCCESS_MESSAGE = "Đăng ký tài khoản thành công!"
        const val SOCIAL_EMAIL_SUFFIX = ".user@carled.vn"
        const val SOCIAL_PLACEHOLDER_PASSWORD = "social_pass_123"
    }
}