package com.carled.app.presentation.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carled.app.presentation.components.*
import com.carled.app.presentation.screens.auth.AuthCodeDialog
import com.carled.app.presentation.theme.ErrorRed
import com.carled.app.presentation.theme.GreenDark
import com.carled.app.presentation.theme.Ink

@Composable
fun ChangePasswordScreen(
    viewModel: ChangePasswordViewModel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    ChangePasswordScreenContent(
        state = uiState,
        onOldPasswordChange = viewModel::onOldPasswordChange,
        onNewPasswordChange = viewModel::onNewPasswordChange,
        onConfirmPasswordChange = viewModel::onConfirmPasswordChange,
        onSubmit = viewModel::submit,
        onClose = onClose,
        onAuthCodeChange = viewModel::onVerificationCodeChange,
        onResendCode = viewModel::resendCode,
        onVerifySubmit = viewModel::confirmChangePassword,
        onDismissVerifyDialog = viewModel::dismissVerifyDialog,
        modifier = modifier
    )
}

@Composable
private fun ChangePasswordScreenContent(
    state: ChangePasswordUiState,
    onOldPasswordChange: (String) -> Unit,
    onNewPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onClose: () -> Unit,
    onAuthCodeChange: (String) -> Unit,
    onResendCode: () -> Unit,
    onVerifySubmit: () -> Unit,
    onDismissVerifyDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier
            .fillMaxSize()
            .background(MirageBackground)
            .systemBarsPadding()
            .padding(8.dp)
    ) {
        WindowFrame(
            title = "Account security",
            onClose = onClose,
            compact = true,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                "Change your password",
                color = Ink,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold
            )

            HardTextField(
                value = state.oldPassword,
                onValueChange = onOldPasswordChange,
                label = "Old password",
                enabled = !state.isLoading,
                isPassword = true,
                imeAction = ImeAction.Next,
                leadingIcon = { KeyGlyph(color = Ink) },
                modifier = Modifier.fillMaxWidth()
            )

            HardTextField(
                value = state.newPassword,
                onValueChange = onNewPasswordChange,
                label = "New password",
                enabled = !state.isLoading,
                isPassword = true,
                imeAction = ImeAction.Next,
                leadingIcon = { KeyGlyph(color = Ink) },
                modifier = Modifier.fillMaxWidth()
            )

            HardTextField(
                value = state.confirmPassword,
                onValueChange = onConfirmPasswordChange,
                label = "Confirm new password",
                enabled = !state.isLoading,
                isPassword = true,
                imeAction = ImeAction.Done,
                onImeAction = onSubmit,
                leadingIcon = { KeyGlyph(color = Ink) },
                modifier = Modifier.fillMaxWidth()
            )

            state.errorMessage?.let { Text(it, color = ErrorRed, fontSize = 13.sp) }
            state.successMessage?.let {
                Text(it, color = GreenDark, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }

            HardButton(
                text = "Change password",
                onClick = onSubmit,
                enabled = state.oldPassword.isNotBlank() &&
                        state.newPassword.isNotBlank() &&
                        state.confirmPassword.isNotBlank() &&
                        !state.isLoading,
                loading = state.isLoading,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .widthIn(min = 160.dp)
            )
        }
    }

    // Changing a password has no authenticated endpoint of its own — the server
    // only exposes the forgot-password/reset-password OTP pair, so confirming
    // here is what actually commits the new password. Dismissing without
    // entering a valid code leaves the old password in place.
    if (state.showVerifyDialog) {
        AuthCodeDialog(
            code = state.verificationCode,
            onCodeChange = onAuthCodeChange,
            onResend = onResendCode,
            onSignUp = onVerifySubmit,
            onDismiss = onDismissVerifyDialog,
            loading = state.isVerifying,
            error = state.verifyError,
            confirmLabel = "Confirm"
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun ChangePasswordScreenPreview() {
    ChangePasswordScreenContent(
        state = ChangePasswordUiState(),
        onOldPasswordChange = {}, onNewPasswordChange = {}, onConfirmPasswordChange = {},
        onSubmit = {}, onClose = {}, onAuthCodeChange = {}, onResendCode = {},
        onVerifySubmit = {}, onDismissVerifyDialog = {}
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun ChangePasswordVerifyDialogPreview() {
    ChangePasswordScreenContent(
        state = ChangePasswordUiState(showVerifyDialog = true),
        onOldPasswordChange = {}, onNewPasswordChange = {}, onConfirmPasswordChange = {},
        onSubmit = {}, onClose = {}, onAuthCodeChange = {}, onResendCode = {},
        onVerifySubmit = {}, onDismissVerifyDialog = {}
    )
}