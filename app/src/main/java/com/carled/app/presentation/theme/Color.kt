package com.carled.app.presentation.theme

import androidx.compose.ui.graphics.Color

val Green = Color(0xFF7FC89E)       // cards, window bars, buttons, footers
val GreenLight = Color(0xFF86D095)  // highlights, selected states
val GreenDark = Color(0xFF6D9A73)   // shadows, close button, secondary text
val Cream = Color(0xFFFFFDF7)       // page / window background
val Ink = Color(0xFF23261F)         // text, borders, dark backgrounds
val FieldGrey = Color(0xFFD9D9D9)   // text fields, social buttons
val ErrorRed = Color(0xFFB3261E)

@Deprecated("Use GreenLight", ReplaceWith("GreenLight"))
val FigmaRetroGreen = GreenLight

@Deprecated("Use GreenLight", ReplaceWith("GreenLight"))
val FigmaRetroMint = GreenLight

@Deprecated("Use GreenDark", ReplaceWith("GreenDark"))
val FigmaRetroDarkGreen = GreenDark

@Deprecated("Use Green", ReplaceWith("Green"))
val FigmaRetroDotGreen = Green

@Deprecated("Use Cream", ReplaceWith("Cream"))
val FigmaRetroCream = Cream

@Deprecated("Use FieldGrey", ReplaceWith("FieldGrey"))
val FigmaRetroGrey = FieldGrey

@Deprecated("Use Ink", ReplaceWith("Ink"))
val FigmaRetroBorder = Ink

@Deprecated("Use ErrorRed", ReplaceWith("ErrorRed"))
val ErrorRed2 = ErrorRed

private const val LEGACY = "Legacy non-Figma color. Migrate to the Figma palette."

@Deprecated(LEGACY) val DarkBackground = Color(0xFF0F111A)
@Deprecated(LEGACY) val SurfaceDark = Color(0xFF181B2A)
@Deprecated(LEGACY) val SurfaceLightDark = Color(0xFF24293E)

@Deprecated(LEGACY) val PrimaryCyan = Color(0xFF00D2FF)
@Deprecated(LEGACY) val PrimaryCyanVariant = Color(0xFF0891B2)
@Deprecated(LEGACY) val AccentPurple = Color(0xFF7928CA)
@Deprecated(LEGACY) val AccentPink = Color(0xFFEC4899)
@Deprecated(LEGACY) val NeonGreen = Color(0xFF00F076)

@Deprecated(LEGACY) val DivoomGold = Color(0xFFFFA726)
@Deprecated(LEGACY) val DivoomCard = Color(0xFF181B2A)
@Deprecated(LEGACY) val DivoomCardSecondary = Color(0xFF22263A)
@Deprecated(LEGACY) val DivoomTextMuted = Color(0xFF8C93AD)
@Deprecated(LEGACY) val DivoomBorder = Color(0xFF2B3048)
@Deprecated(LEGACY) val DivoomDanger = Color(0xFFFF3366)

@Deprecated(LEGACY) val TextPrimary = Color(0xFFFFFFFF)
@Deprecated(LEGACY) val TextSecondary = Color(0xFF8C93AD)