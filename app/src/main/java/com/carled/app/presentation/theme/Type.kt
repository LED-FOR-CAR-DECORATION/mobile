package com.carled.app.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.Font
import com.carled.app.R
import androidx.compose.ui.unit.sp

private val plexsan = FontFamily(
    Font(R.font.plexsanthin, FontWeight.Thin),
    Font(R.font.plexsanextralight, FontWeight.ExtraLight),
    Font(R.font.plexsanlight, FontWeight.Light),
    Font(R.font.plexsanregular, FontWeight.Normal),
    Font(R.font.plexsanmedium, FontWeight.Medium),
    Font(R.font.plexsansemibold, FontWeight.SemiBold),
    Font(R.font.plexsanbold, FontWeight.Bold)
)

val Typography = Typography(
    headlineMedium = TextStyle(fontFamily = plexsan, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 36.sp),
    headlineSmall = TextStyle(fontFamily = plexsan, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 32.sp),
    titleLarge = TextStyle(
        fontFamily = plexsan,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = plexsan,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = plexsan,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = plexsan,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelSmall = TextStyle(
        fontFamily = plexsan,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)