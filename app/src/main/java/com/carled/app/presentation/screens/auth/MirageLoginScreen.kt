package com.carled.app.presentation.screens.auth

import androidx.compose.runtime.Composable

@Composable
fun MirageLoginScreen(vm: LoginViewModel, success: () -> Unit) {
    LoginScreen(viewModel = vm, onLoginSuccess = success)
}