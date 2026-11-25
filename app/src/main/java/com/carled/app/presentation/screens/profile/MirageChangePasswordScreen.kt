package com.carled.app.presentation.screens.profile

import androidx.compose.runtime.Composable

@Composable
fun MirageChangePasswordScreen(vm: ChangePasswordViewModel, back: () -> Unit) {
    ChangePasswordScreen(viewModel = vm, onClose = back)
}