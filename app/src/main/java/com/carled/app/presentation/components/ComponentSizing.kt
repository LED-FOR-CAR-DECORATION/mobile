package com.carled.app.presentation.components

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val BASE_WIDTH = 280.dp

private object BaseNavBarSizing {
    val shadowOffset = 5.dp
    val iconBox = 32.dp
    val iconSize = 18.dp
    val labelSize = 7.sp
    val horizontalPadding = 4.dp
    val verticalPadding = 4.dp
    val rowSpacing = 2.dp
}

data class NavBarSizing(
    val shadowOffset: Dp,
    val iconBox: Dp,
    val iconSize: Dp,
    val labelSize: TextUnit,
    val horizontalPadding: Dp,
    val verticalPadding: Dp,
    val rowSpacing: Dp
)

fun sizingFor(availableWidth: Dp): NavBarSizing {
    val scale = (availableWidth / BASE_WIDTH).coerceAtMost(1.4f)
    return NavBarSizing(
        shadowOffset = BaseNavBarSizing.shadowOffset * scale,
        iconBox = BaseNavBarSizing.iconBox * scale,
        iconSize = BaseNavBarSizing.iconSize * scale,
        labelSize = (BaseNavBarSizing.labelSize.value * scale).sp,
        horizontalPadding = BaseNavBarSizing.horizontalPadding * scale,
        verticalPadding = BaseNavBarSizing.verticalPadding * scale,
        rowSpacing = BaseNavBarSizing.rowSpacing * scale
    )
}