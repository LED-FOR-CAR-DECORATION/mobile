package com.carled.app.presentation.theme

import androidx.compose.ui.graphics.Color

//Main figma color pallete
val Green      = Color(0xFF7FC89E)   // cards, window bars, buttons, footers
val GreenLight = Color(0xFF86D095)   // highlights, selected states
val GreenDark  = Color(0xFF6D9A73)   // shadows, close button, secondary text
val Cream      = Color(0xFFFFFDF7)   // page / window background
val Ink        = Color(0xFF23261F)   // text, borders, dark backgrounds
val FieldGrey  = Color(0xFFD9D9D9)   // text fields, social buttons
val ErrorRed   = Color(0xFFB3261E)

// ─────────────────────────────────────────────────────────────
// Legacy: dark neon theme. Delete each one once nothing uses it.
// (Android Studio: Code > Inspect Code, or Analyze > Run Inspection by Name > "Unused declaration")
// ─────────────────────────────────────────────────────────────
val DarkBackground = Color(0xFF0F111A)
val SurfaceDark = Color(0xFF181B2A)
val SurfaceLightDark = Color(0xFF24293E)

val PrimaryCyan = Color(0xFF00D2FF)
val PrimaryCyanVariant = Color(0xFF0891B2)
val AccentPurple = Color(0xFF7928CA)
val AccentPink = Color(0xFFEC4899)
val NeonGreen = Color(0xFF00F076)

val DivoomGold = Color(0xFFFFA726)
val DivoomCard = Color(0xFF181B2A)
val DivoomCardSecondary = Color(0xFF22263A)
val DivoomTextMuted = Color(0xFF8C93AD)
val DivoomBorder = Color(0xFF2B3048)
val DivoomDanger = Color(0xFFFF3366)

val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFF8C93AD)

val FigmaRetroGreen = GreenLight
val FigmaRetroMint = GreenLight          // was 0x89D095, not in the palette (looks like a typo of 86D095)
val FigmaRetroDarkGreen = GreenDark
val FigmaRetroDotGreen = Green
val FigmaRetroCream = Cream
val FigmaRetroGrey = FieldGrey
val FigmaRetroBorder = Ink               // was 0x1E1E1E, palette ink is 23261F
val ErrorRed2 = ErrorRed