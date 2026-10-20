package com.carled.app.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable

private val MirageColorScheme = lightColorScheme(
    primary = Color(0xff165d3a),
    primaryContainer = Color(0xff89c89b),
    secondary = Color(0xff165d3a),
    secondaryContainer = Color(0xffd7eddd),
    onSecondaryContainer = Color(0xff111111),
    surfaceVariant = Color(0xffeef5ef),
    onSurfaceVariant = Color(0xff555555),
    background = Color(0xfffdfdfd),
    surface = Color.White,
    onPrimary = Color.White,
    onPrimaryContainer = Color(0xff111111),
    onBackground = Color(0xff111111),
    onSurface = Color(0xff111111),
    error = Color(0xffb3261e)
)

@Composable
fun CarLedTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = MirageColorScheme,
        typography = Typography,
        content = content
    )
}
