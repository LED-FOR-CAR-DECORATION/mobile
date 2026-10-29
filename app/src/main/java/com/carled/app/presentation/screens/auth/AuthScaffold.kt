package com.carled.app.presentation.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carled.app.presentation.components.WindowFrame
import com.carled.app.presentation.theme.Cream
import com.carled.app.presentation.theme.Ink

private const val LANDSCAPE_WINDOW_FRACTION = 0.7f
private val PortraitSideMargin = 12.dp

private val PortraitScrim = Brush.verticalGradient(
    listOf(Ink.copy(alpha = 0.4f), Ink.copy(alpha = 0.8f), Ink)
)

private val LandscapeScrim = Brush.verticalGradient(
    listOf(Ink.copy(alpha = 0.6f), Ink.copy(alpha = 0f))
)

@Composable
fun AuthScaffold(
    heroTitle: String = "", // Default to empty so it's optional
    windowTitle: String,
    modifier: Modifier = Modifier,
    onClose: (() -> Unit)? = null,
    hero: @Composable BoxScope.() -> Unit = {},
    footer: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    BoxWithConstraints(modifier.fillMaxSize().background(Cream).imePadding()) {
        if (maxWidth > maxHeight) {
            Box(Modifier.fillMaxSize()) {
                HeroBackdrop(hero, LandscapeScrim)
                if (heroTitle.isNotBlank()) {
                    HeroTitle(heroTitle)
                }
                WindowFrame(
                    title = windowTitle,
                    onClose = onClose,
                    footer = footer,
                    compact = true,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .systemBarsPadding()
                        .fillMaxHeight()
                        .fillMaxWidth(LANDSCAPE_WINDOW_FRACTION),
                    content = content
                )
            }
        } else {
            Box(Modifier.fillMaxSize()) {
                Box(
                    Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxSize()
                ) {
                    HeroBackdrop(hero, PortraitScrim)
                }

                if (heroTitle.isNotBlank()) {
                    HeroTitle(heroTitle)
                }

                WindowFrame(
                    title = windowTitle,
                    onClose = onClose,
                    footer = footer,
                    compact = true,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .systemBarsPadding()
                        .padding(horizontal = PortraitSideMargin, vertical = 16.dp)
                        .fillMaxWidth(),
                    content = content
                )
            }
        }
    }
}

@Composable
private fun BoxScope.HeroBackdrop(
    hero: @Composable BoxScope.() -> Unit,
    scrim: Brush
) {
    hero()
    // Box(Modifier.fillMaxSize().background(scrim)) // Scrim commented out to keep full-color image
}

@Composable
private fun BoxScope.HeroTitle(title: String) {
    Text(
        text = title,
        color = Cream,
        fontSize = 36.sp,
        lineHeight = 40.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .align(Alignment.TopStart)
            .statusBarsPadding()
            .padding(24.dp)
    )
}