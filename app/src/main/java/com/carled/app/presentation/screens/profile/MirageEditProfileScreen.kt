package com.carled.app.presentation.screens.profile

import androidx.compose.runtime.Composable

@Composable
fun MirageEditProfileScreen(vm: EditProfileViewModel, back: () -> Unit) {
    EditProfileScreen(viewModel = vm, onClose = back)
}