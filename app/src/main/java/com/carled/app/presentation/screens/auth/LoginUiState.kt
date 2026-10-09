package com.carled.app.presentation.screens.auth

import com.carled.app.domain.model.User

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val loggedInUser: User? = null,
    val errorMessage: String? = null
)
