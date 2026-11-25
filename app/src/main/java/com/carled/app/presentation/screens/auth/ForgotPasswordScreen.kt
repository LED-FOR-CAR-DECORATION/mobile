package com.carled.app.presentation.screens.auth

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.carled.app.presentation.components.HardButton
import com.carled.app.presentation.components.HardTextField
import com.carled.app.presentation.components.KeyGlyph
import com.carled.app.presentation.components.PersonGlyph
import com.carled.app.presentation.components.WindowFrame
import com.carled.app.presentation.theme.ErrorRed
import com.carled.app.presentation.theme.Ink

/**
 * Shown as a full-screen overlay Dialog on top of LoginScreen, the same way
 * AuthCodeDialog overlays on top of it during registration — no nav-graph
 * entry required. The server has no "forgot password" verb beyond the OTP
 * pair (forgot-password -> reset-password), so this collects the new
 * password up front and only commits it once [AuthCodeDialog]'s code is
 * confirmed.
 */
@Composable
fun ForgotPasswordScreen(
    viewModel: ForgotPasswordViewModel,
    onDismiss: () -> Unit,
    onPasswordResetSuccess: (email: String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        WindowFrame(
            title = "Reset password",
            onClose = onDismiss,
            compact = true,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                "Enter your email to get a reset code, then choose a new password.",
                color = Ink,
                fontSize = 13.sp,
                modifier = Modifier.fillMaxWidth()
            )

            HardTextField(
                value = uiState.email,
                onValueChange = viewModel::onEmailChange,
                label = "Email",
                enabled = !uiState.isLoading,
                keyboardType = KeyboardType.Email,
                leadingIcon = { PersonGlyph() },
                modifier = Modifier.fillMaxWidth()
            )

            HardTextField(
                value = uiState.newPassword,
                onValueChange = viewModel::onNewPasswordChange,
                label = "New password",
                enabled = !uiState.isLoading,
                isPassword = true,
                imeAction = ImeAction.Next,
                leadingIcon = { KeyGlyph(color = Ink) },
                modifier = Modifier.fillMaxWidth()
            )

            HardTextField(
                value = uiState.confirmPassword,
                onValueChange = viewModel::onConfirmPasswordChange,
                label = "Confirm new password",
                enabled = !uiState.isLoading,
                isPassword = true,
                imeAction = ImeAction.Done,
                onImeAction = viewModel::submit,
                leadingIcon = { KeyGlyph(color = Ink) },
                modifier = Modifier.fillMaxWidth()
            )

            uiState.errorMessage?.let { Text(it, color = ErrorRed, fontSize = 13.sp) }

            HardButton(
                text = "Send reset code",
                onClick = viewModel::submit,
                enabled = uiState.email.isNotBlank() &&
                        uiState.newPassword.isNotBlank() &&
                        uiState.confirmPassword.isNotBlank() &&
                        !uiState.isLoading,
                loading = uiState.isLoading,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .widthIn(min = 160.dp)
            )
        }
    }

    if (uiState.showVerifyDialog) {
        AuthCodeDialog(
            code = uiState.verificationCode,
            onCodeChange = viewModel::onVerificationCodeChange,
            onResend = viewModel::resendCode,
            onSignUp = { viewModel.confirmReset(onSuccess = onPasswordResetSuccess) },
            onDismiss = viewModel::dismissVerifyDialog,
            loading = uiState.isVerifying,
            error = uiState.verifyError,
            confirmLabel = "Confirm"
        )
    }
}