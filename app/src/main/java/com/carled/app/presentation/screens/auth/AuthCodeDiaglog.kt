package com.carled.app.presentation.screens.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.carled.app.presentation.components.HardButton
import com.carled.app.presentation.components.HardTextField
import com.carled.app.presentation.components.PersonGlyph
import com.carled.app.presentation.components.WindowFrame
import com.carled.app.presentation.theme.GreenDark
import com.carled.app.presentation.theme.Ink

@Composable
fun AuthCodeDialog(
    code: String,
    onCodeChange: (String) -> Unit,
    onResend: () -> Unit,
    onSignUp: () -> Unit,
    onDismiss: () -> Unit,
    loading: Boolean = false,
    error: String? = null
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        WindowFrame(
            title = "Authentication",
            onClose = onDismiss,
            compact = true,
            modifier = Modifier.widthIn(max = 420.dp),
            footer = {
                Text(
                    "Please check your email for an authentication code .",
                    color = Ink,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        ) {
            HardTextField(
                value = code,
                onValueChange = onCodeChange,
                label = "Authcode",
                enabled = !loading,
                error = error,
                leadingIcon = { PersonGlyph() },
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                "Resend code",
                color = GreenDark,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.End,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !loading, onClick = onResend)
            )

            HardButton(
                text = if (loading) "Đang xác thực…" else "Sign up",
                onClick = onSignUp,
                enabled = code.isNotBlank() && !loading,
                loading = loading,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .widthIn(min = 160.dp)
            )
        }
    }
}