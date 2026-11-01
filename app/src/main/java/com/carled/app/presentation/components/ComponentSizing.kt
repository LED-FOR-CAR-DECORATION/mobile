package com.carled.app.presentation.components

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val BASE_WIDTH = 280.dp

private object BaseNavBarSizing {
    val shadowOffset = 8.dp
    val iconBox = 44.dp
    val iconSize = 24.dp
    val labelSize = 8.sp
    val horizontalPadding = 4.dp
    val verticalPadding = 6.dp
    val rowSpacing = 4.dp
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
    val scale = availableWidth / BASE_WIDTH
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