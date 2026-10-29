package com.carled.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carled.app.presentation.theme.Cream
import com.carled.app.presentation.theme.ErrorRed
import com.carled.app.presentation.theme.FieldGrey
import com.carled.app.presentation.theme.Green
import com.carled.app.presentation.theme.GreenDark
import com.carled.app.presentation.theme.Ink

object HardStyle {
    val Border = 1.5.dp
    val CardOffset = 6.dp
    val SmallOffset = 4.dp
}

fun Modifier.hardSurface(
    color: Color = Green,
    offset: Dp = HardStyle.CardOffset
): Modifier = this
    .padding(end = offset, bottom = offset)   // reserves room, no manual padding in parents
    .background(color)
    .border(HardStyle.Border, Ink)

@Composable
fun HardButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    filled: Boolean = true,
    textColor: Color = Ink,
    shadowOffset: Dp = 0.dp   // pass 0.dp for a flat button
) {
    Box(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.5f)
            .hardSurface(color = if (filled) Green else Cream, offset = shadowOffset)
            .clickable(enabled = enabled && !loading, role = Role.Button, onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        if (loading) {
            CircularProgressIndicator(Modifier.size(18.dp), color = textColor, strokeWidth = 2.dp)
        } else {
            Text(text, color = textColor, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun HardCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Row(
        modifier.toggleable(
            value = checked,
            enabled = enabled,
            role = Role.Checkbox,
            onValueChange = onCheckedChange
        ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            Modifier
                .size(14.dp)
                .background(if (checked) Green else FieldGrey)
                .border(1.dp, Ink),
            contentAlignment = Alignment.Center
        ) {
            if (checked) CheckGlyph()
        }
        Text(label, color = Ink, fontSize = 11.sp)
    }
}

@Composable
fun HardTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    enabled: Boolean = true,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: () -> Unit = {},
    leadingIcon: (@Composable () -> Unit)? = null
) {
    var passwordVisible by remember { mutableStateOf(false) }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(FieldGrey)
                .then(if (error != null) Modifier.border(1.5.dp, ErrorRed) else Modifier)
                .padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            leadingIcon?.invoke()
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                enabled = enabled,
                singleLine = true,
                textStyle = TextStyle(color = Ink, fontSize = 15.sp),
                cursorBrush = SolidColor(Ink),
                visualTransformation =
                    if (isPassword && !passwordVisible) PasswordVisualTransformation()
                    else VisualTransformation.None,
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (isPassword) KeyboardType.Password else keyboardType,
                    imeAction = imeAction
                ),
                keyboardActions = KeyboardActions(onAny = { onImeAction() })
            )
            if (isPassword) {
                Text(
                    text = if (passwordVisible) "Hide" else "Show",
                    color = GreenDark,
                    fontSize = 12.sp,
                    modifier = Modifier.clickable { passwordVisible = !passwordVisible }
                )
            }
        }
        if (error != null) Text(error, color = ErrorRed, fontSize = 12.sp)
    }
}

@Composable
fun SocialLoginBar(
    onProviderClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    providers: List<String> = listOf("Google", "Facebook", "Apple", "X"),
    enabled: Boolean = true
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Login with", color = Ink, fontSize = 13.sp)
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally)
        ) {
            providers.forEach { name ->
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(FieldGrey)
                        .clickable(enabled = enabled, role = Role.Button, onClickLabel = "Login with $name") {
                            onProviderClick(name)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(name.first().toString(), color = Ink, fontSize = 13.sp)
                }
            }
        }
    }
}