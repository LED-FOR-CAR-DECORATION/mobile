package com.carled.app.presentation.screens.auth

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import com.carled.app.presentation.components.*

@Composable
fun MirageLoginScreen(vm: LoginViewModel, success: () -> Unit) {
    val state by vm.uiState.collectAsState()
    Column(Modifier.fillMaxSize().background(MirageBackground).systemBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Spacer(Modifier.height(20.dp))
        Text("MIRAGE", Modifier.miragePanel().padding(horizontal = 24.dp, vertical = 10.dp), style = MaterialTheme.typography.titleLarge)
        PixelPreview(modifier = Modifier.fillMaxWidth().height(120.dp))
        MirageCard {
            Text("Chào mừng trở lại", style = MaterialTheme.typography.headlineMedium)
            Text("Đăng nhập để quản lý thiết bị LED của bạn.", color = MirageMuted)
            OutlinedTextField(state.email, vm::onEmailChange, modifier = Modifier.fillMaxWidth(), label = { Text("Email") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), enabled = !state.isLoading)
            OutlinedTextField(state.password, vm::onPasswordChange, modifier = Modifier.fillMaxWidth(), label = { Text("Mật khẩu") }, singleLine = true,
                visualTransformation = if (state.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), enabled = !state.isLoading,
                trailingIcon = { IconButton(onClick = vm::togglePasswordVisibility) { Icon(if (state.isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, if (state.isPasswordVisible) "Ẩn mật khẩu" else "Hiện mật khẩu") } })
            state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            MirageButton(if (state.isLoading) "Đang đăng nhập…" else "Đăng nhập", { vm.login(success) }, Modifier.fillMaxWidth(), enabled = !state.isLoading && state.email.isNotBlank() && state.password.isNotBlank())
        }
        Text("Kết nối thiết bị. Tạo dấu ấn riêng.", color = MirageMuted)
    }
}
