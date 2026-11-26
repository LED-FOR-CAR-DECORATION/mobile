package com.carled.app.presentation.screens.profile

data class EditProfileUiState(
    val email: String = "",
    val fullName: String = "",
    val phone: String = "",
    val birthDate: String = "",
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)