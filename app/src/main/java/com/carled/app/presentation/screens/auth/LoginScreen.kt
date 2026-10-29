package com.carled.app.presentation.screens.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carled.app.R
import com.carled.app.presentation.components.HardButton
import com.carled.app.presentation.components.HardCheckbox
import com.carled.app.presentation.components.HardTextField
import com.carled.app.presentation.components.KeyGlyph
import com.carled.app.presentation.components.PersonGlyph
import com.carled.app.presentation.components.SocialLoginBar
import com.carled.app.presentation.theme.Cream
import com.carled.app.presentation.theme.ErrorRed
import com.carled.app.presentation.theme.GreenDark
import com.carled.app.presentation.theme.Ink

private val SocialProviders = listOf("Google", "Apple", "Facebook", "Guest")

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onLoginSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    val submit: () -> Unit = {
        focusManager.clearFocus()
        viewModel.submit(onSuccess = onLoginSuccess)
    }

    LoginScreenContent(
        state = uiState,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onConfirmPasswordChange = viewModel::onConfirmPasswordChange,
        onRememberMeChange = viewModel::onRememberMeChange,
        onAcceptTermsChange = viewModel::onAcceptTermsChange,
        onSubmit = submit,
        onForgotPasswordClick = {
            viewModel.onEmailChange("demo@carled.vn")
            viewModel.onPasswordChange("123456")
        },
        onToggleMode = viewModel::toggleMode,
        onSocialClick = { provider ->
            viewModel.loginWithSocial(provider, onSuccess = onLoginSuccess)
        },
        onClose = { focusManager.clearFocus() },
        modifier = modifier
    )
}

@Composable
private fun LoginScreenContent(
    state: LoginUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onRememberMeChange: (Boolean) -> Unit,
    onAcceptTermsChange: (Boolean) -> Unit,
    onSubmit: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    onToggleMode: () -> Unit,
    onSocialClick: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val moveFocusDown: () -> Unit = { focusManager.moveFocus(FocusDirection.Down) }
    val passwordIsLastField = !state.isRegisterMode

    AuthScaffold(
        heroTitle = if (state.isRegisterMode) "Join our\ncommunity\nTODAY." else "Welcome\nback.",
        windowTitle = if (state.isRegisterMode) "Sign up for an account" else "Login",
        modifier = modifier,
        onClose = onClose,
        hero = { LoginHero() },
        footer = {
            SocialLoginBar(
                onProviderClick = onSocialClick,
                providers = SocialProviders,
                enabled = !state.isLoading
            )
        }
    ) {
        HardTextField(
            value = state.email,
            onValueChange = onEmailChange,
            label = "Email/Phone",
            enabled = !state.isLoading,
            keyboardType = KeyboardType.Email,
            onImeAction = moveFocusDown,
            leadingIcon = { PersonGlyph() },
            modifier = Modifier.fillMaxWidth()
        )

        HardTextField(
            value = state.password,
            onValueChange = onPasswordChange,
            label = "Password",
            enabled = !state.isLoading,
            isPassword = true,
            imeAction = if (passwordIsLastField) ImeAction.Done else ImeAction.Next,
            onImeAction = if (passwordIsLastField) onSubmit else moveFocusDown,
            leadingIcon = { KeyGlyph(color = Ink) },
            modifier = Modifier.fillMaxWidth()
        )

        if (state.isRegisterMode) {
            HardTextField(
                value = state.confirmPassword,
                onValueChange = onConfirmPasswordChange,
                label = "Confirm password",
                enabled = !state.isLoading,
                isPassword = true,
                imeAction = ImeAction.Done,
                onImeAction = onSubmit,
                leadingIcon = { KeyGlyph(color = Ink) },
                modifier = Modifier.fillMaxWidth()
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                HardCheckbox(
                    checked = state.rememberMe,
                    onCheckedChange = onRememberMeChange,
                    label = "Remember me",
                    enabled = !state.isLoading
                )
                HardCheckbox(
                    checked = state.acceptTerms,
                    onCheckedChange = onAcceptTermsChange,
                    label = "I have read and accept Term of Service",
                    enabled = !state.isLoading
                )
            }
        } else {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                HardCheckbox(
                    checked = state.rememberMe,
                    onCheckedChange = onRememberMeChange,
                    label = "Remember me",
                    enabled = !state.isLoading
                )
                Text(
                    "Forgot Password?",
                    color = Ink,
                    fontSize = 14.sp,
                    modifier = Modifier.clickable(onClick = onForgotPasswordClick)
                )
            }
        }

        state.errorMessage?.let { Text(it, color = ErrorRed, fontSize = 13.sp) }
        state.successMessage?.let {
            Text(it, color = GreenDark, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }

        HardButton(
            text = if (state.isRegisterMode) "Sign-up" else "Login",
            onClick = onSubmit,
            loading = state.isLoading,
            textColor = Cream,
            shadowOffset = 0.dp,
            modifier = Modifier.align(Alignment.CenterHorizontally).fillMaxWidth(0.5f).widthIn(max = 160.dp)
        )

        Text(
            text = if (state.isRegisterMode) "Already have an account? Sign in now!" else "No account? Sign up now!",
            color = Ink,
            fontSize = 12.sp,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .clickable(onClick = onToggleMode)
        )
    }
}

@Composable
private fun BoxScope.LoginHero() {
    Image(
        painter = painterResource(id = R.drawable.login_bg),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize()
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun LoginScreenPreview() {
    LoginScreenContent(
        state = LoginUiState(errorMessage = "Wrong email or password"),
        onEmailChange = {}, onPasswordChange = {}, onConfirmPasswordChange = {},
        onRememberMeChange = {}, onAcceptTermsChange = {}, onSubmit = {},
        onForgotPasswordClick = {}, onToggleMode = {}, onSocialClick = {}, onClose = {}
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun RegisterScreenPreview() {
    LoginScreenContent(
        state = LoginUiState(isRegisterMode = true),
        onEmailChange = {}, onPasswordChange = {}, onConfirmPasswordChange = {},
        onRememberMeChange = {}, onAcceptTermsChange = {}, onSubmit = {},
        onForgotPasswordClick = {}, onToggleMode = {}, onSocialClick = {}, onClose = {}
    )
}