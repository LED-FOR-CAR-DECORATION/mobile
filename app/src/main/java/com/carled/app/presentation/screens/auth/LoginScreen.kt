package com.carled.app.presentation.screens.auth

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carled.app.R
import com.carled.app.presentation.theme.*

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onLoginSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val focusManager = LocalFocusManager.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF14161D))
    ) {
        // 1. Background GameBoy & Retro Graphic
        Image(
            painter = painterResource(id = R.drawable.login_bg),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .align(Alignment.TopCenter)
        )

        // Gradient overlay to seamlessly blend with dark background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x6614161D),
                            Color(0xCC14161D),
                            Color(0xFF14161D)
                        )
                    )
                )
        )

        // 2. Main Scrollable Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Hero Header Text from Figma
            AnimatedContent(
                targetState = uiState.isRegisterMode,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "HeaderTitle"
            ) { isRegister ->
                Column {
                    Text(
                        text = if (isRegister) "Join our\ncommunity\nTODAY." else "Welcome\nback.",
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 38.sp,
                        letterSpacing = (-0.5).sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isRegister) "Tạo tài khoản Car LED để tùy biến kính lái và tham gia cộng đồng" else "Đăng nhập để điều khiển thanh LED Matrix & kho hoạt ảnh xe",
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 3. Retro OS Window Box (LEDSCREEN Figma Frame)
            RetroWindowCard(
                title = if (uiState.isRegisterMode) "Sign up for an account" else "Login",
                onCloseClick = {
                    focusManager.clearFocus()
                }
            ) {
                // Inside Window Form Content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    // Email / Phone Field
                    RetroFieldLabel(text = "Email/Phone")
                    Spacer(modifier = Modifier.height(6.dp))
                    RetroInputField(
                        value = uiState.email,
                        onValueChange = viewModel::onEmailChange,
                        placeholder = "user@carled.vn hoặc SĐT",
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = FigmaRetroDarkGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Password Field
                    RetroFieldLabel(text = "Password")
                    Spacer(modifier = Modifier.height(6.dp))
                    RetroInputField(
                        value = uiState.password,
                        onValueChange = viewModel::onPasswordChange,
                        placeholder = "Nhập mật khẩu",
                        visualTransformation = if (uiState.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.VpnKey,
                                contentDescription = null,
                                tint = FigmaRetroDarkGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = { viewModel.togglePasswordVisibility() },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = if (uiState.isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle password visibility",
                                    tint = FigmaRetroBorder,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = if (uiState.isRegisterMode) ImeAction.Next else ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) },
                            onDone = {
                                focusManager.clearFocus()
                                viewModel.submit(onSuccess = onLoginSuccess)
                            }
                        )
                    )

                    // Confirm Password Field (for Register Mode)
                    AnimatedVisibility(
                        visible = uiState.isRegisterMode,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(16.dp))
                            RetroFieldLabel(text = "Confirm password")
                            Spacer(modifier = Modifier.height(6.dp))
                            RetroInputField(
                                value = uiState.confirmPassword,
                                onValueChange = viewModel::onConfirmPasswordChange,
                                placeholder = "Xác nhận lại mật khẩu",
                                visualTransformation = if (uiState.isConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.VpnKey,
                                        contentDescription = null,
                                        tint = FigmaRetroDarkGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                trailingIcon = {
                                    IconButton(
                                        onClick = { viewModel.toggleConfirmPasswordVisibility() },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (uiState.isConfirmPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = "Toggle confirm password visibility",
                                            tint = FigmaRetroBorder,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        focusManager.clearFocus()
                                        viewModel.submit(onSuccess = onLoginSuccess)
                                    }
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Checkbox & Links
                    if (!uiState.isRegisterMode) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RetroCheckbox(
                                checked = uiState.rememberMe,
                                onCheckedChange = viewModel::onRememberMeChange,
                                label = "Remember me"
                            )
                            Text(
                                text = "Forgot Password?",
                                color = FigmaRetroBorder,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                textDecoration = TextDecoration.Underline,
                                modifier = Modifier.clickable {
                                    // Hint to user for mock
                                    viewModel.onEmailChange("demo@carled.vn")
                                    viewModel.onPasswordChange("123456")
                                }
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            RetroCheckbox(
                                checked = uiState.rememberMe,
                                onCheckedChange = viewModel::onRememberMeChange,
                                label = "Remember me"
                            )
                            RetroCheckbox(
                                checked = uiState.acceptTerms,
                                onCheckedChange = viewModel::onAcceptTermsChange,
                                label = "I have read and accept Term of Service"
                            )
                        }
                    }

                    // Error Message Banner
                    if (uiState.errorMessage != null) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFFFEBEE), RoundedCornerShape(4.dp))
                                .border(1.5.dp, ErrorRed, RoundedCornerShape(4.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = uiState.errorMessage ?: "",
                                color = ErrorRed,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Success Message Banner
                    if (uiState.successMessage != null) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFE8F5E9), RoundedCornerShape(4.dp))
                                .border(1.5.dp, FigmaRetroDarkGreen, RoundedCornerShape(4.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = uiState.successMessage ?: "",
                                color = FigmaRetroDarkGreen,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Retro Neobrutalist Action Button ("Login" / "Sign-up")
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        RetroButton(
                            text = if (uiState.isRegisterMode) "Sign-up" else "Login",
                            isLoading = uiState.isLoading,
                            onClick = {
                                focusManager.clearFocus()
                                viewModel.submit(onSuccess = onLoginSuccess)
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Switch Mode Prompt
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (uiState.isRegisterMode) "Already have an account? " else "No account? ",
                            color = FigmaRetroBorder,
                            fontSize = 13.sp
                        )
                        Text(
                            text = if (uiState.isRegisterMode) "Sign in now!" else "Sign up now!",
                            color = FigmaRetroBorder,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            textDecoration = TextDecoration.Underline,
                            modifier = Modifier.clickable {
                                viewModel.toggleMode()
                            }
                        )
                    }
                }

                // Window Footer ("Login with" + Social Circle Buttons)
                RetroWindowFooter(
                    label = if (uiState.isRegisterMode) "Sign up with" else "Login with",
                    onSocialClick = { provider ->
                        viewModel.loginWithSocial(provider, onSuccess = onLoginSuccess)
                    }
                )
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

/**
 * Retro OS Window Card Container matching Figma #2070:72 / #2075:236
 */
@Composable
private fun RetroWindowCard(
    title: String,
    onCloseClick: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = FigmaRetroCream),
        modifier = Modifier
            .fillMaxWidth()
            .border(2.5.dp, FigmaRetroBorder, RoundedCornerShape(8.dp))
            .shadow(6.dp, RoundedCornerShape(8.dp), spotColor = Color.Black)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Title Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .border(
                        width = 0.dp,
                        color = Color.Transparent
                    )
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Status green dot from Figma #2151:764
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(FigmaRetroDotGreen)
                            .border(1.5.dp, FigmaRetroBorder, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        color = FigmaRetroBorder,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                // Window action buttons (Maximize & Close)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Maximize / Window button
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .border(1.5.dp, FigmaRetroBorder, RoundedCornerShape(2.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .border(1.dp, FigmaRetroBorder)
                        )
                    }

                    // Close button (X)
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .clickable { onCloseClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = FigmaRetroBorder,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            // Separator between title bar and body
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(FigmaRetroBorder)
            )

            // Body
            content()
        }
    }
}

/**
 * Retro Field Label
 */
@Composable
private fun RetroFieldLabel(text: String) {
    Text(
        text = text,
        color = FigmaRetroBorder,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold
    )
}

/**
 * Retro Input Field with Grey Background and Black Border
 */
@Composable
private fun RetroInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(FigmaRetroGrey, RoundedCornerShape(6.dp))
            .border(1.5.dp, FigmaRetroBorder, RoundedCornerShape(6.dp))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leadingIcon != null) {
            leadingIcon()
            Spacer(modifier = Modifier.width(8.dp))
        }

        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart
        ) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    color = Color(0xFF757575),
                    fontSize = 14.sp
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = TextStyle(
                    color = FigmaRetroBorder,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                ),
                cursorBrush = SolidColor(FigmaRetroBorder),
                visualTransformation = visualTransformation,
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions
            )
        }

        if (trailingIcon != null) {
            Spacer(modifier = Modifier.width(6.dp))
            trailingIcon()
        }
    }
}

/**
 * Retro Checkbox with custom square aesthetic
 */
@Composable
private fun RetroCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String
) {
    Row(
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onCheckedChange(!checked) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .background(if (checked) FigmaRetroDarkGreen else FigmaRetroGrey, RoundedCornerShape(2.dp))
                .border(1.5.dp, FigmaRetroBorder, RoundedCornerShape(2.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (checked) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            color = FigmaRetroBorder,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Neobrutalist Retro Button from Figma
 */
@Composable
private fun RetroButton(
    text: String,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    // 3D Neobrutalist Shadow Box
    Box(
        modifier = Modifier
            .wrapContentSize()
            .clickable(enabled = !isLoading) { onClick() }
    ) {
        // Shadow layer
        Box(
            modifier = Modifier
                .offset(x = 3.dp, y = 3.dp)
                .width(160.dp)
                .height(44.dp)
                .background(FigmaRetroBorder, RoundedCornerShape(4.dp))
        )

        // Main button surface
        Box(
            modifier = Modifier
                .width(160.dp)
                .height(44.dp)
                .background(FigmaRetroGreen, RoundedCornerShape(4.dp))
                .border(2.5.dp, FigmaRetroBorder, RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = FigmaRetroBorder,
                    strokeWidth = 2.5.dp,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Text(
                    text = text,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

/**
 * Retro Window Footer from Figma
 */
@Composable
private fun RetroWindowFooter(
    label: String,
    onSocialClick: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(FigmaRetroBorder)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(FigmaRetroMint)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Label Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(3.dp))
                    .background(FigmaRetroDarkGreen)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = label,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // 4 Social Circular Buttons from Figma #2075:135
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SocialCircleButton(text = "G", onClick = { onSocialClick("Google") })
                SocialCircleButton(text = "", onClick = { onSocialClick("Apple") })
                SocialCircleButton(text = "f", onClick = { onSocialClick("Facebook") })
                SocialCircleButton(text = "★", onClick = { onSocialClick("Guest") })
            }
        }
    }
}

@Composable
private fun SocialCircleButton(
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(Color.White)
            .border(1.5.dp, FigmaRetroBorder, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = FigmaRetroBorder,
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}
