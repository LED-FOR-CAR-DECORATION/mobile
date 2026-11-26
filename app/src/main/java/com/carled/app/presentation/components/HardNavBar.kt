package com.carled.app.presentation.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.carled.app.R
import com.carled.app.presentation.theme.*

private enum class NavTab(val label: String, @DrawableRes val icon: Int) {
    HOME("Homepage", R.drawable.ic_home),
    GALLERY("Kho ảnh", R.drawable.ic_gallery),
    DEVICES("Quản lý thiết bị", R.drawable.ic_settings),
    COMMISSION("Commission", R.drawable.ic_com),
    PROFILE("Hồ sơ", R.drawable.ic_profile)
}

private const val COMMISSION_ASPECT_RATIO = 52f / 28f
private val SHADOW_OFFSET = 8.dp
private val ICON_CORNER = RoundedCornerShape(10.dp)
private val CARD_CORNER = RoundedCornerShape(HardStyle.CornerRadius)

@Composable
fun HardNavBar(
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .padding(end = SHADOW_OFFSET, bottom = SHADOW_OFFSET)
    ) {
        val sizing = remember(maxWidth) { sizingFor(maxWidth) }

        Box(
            Modifier
                .matchParentSize()
                .offset(x = SHADOW_OFFSET, y = SHADOW_OFFSET)
                .background(GreenDark, CARD_CORNER)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(GreenLight, CARD_CORNER)
                .border(HardStyle.Border, Ink, CARD_CORNER)
                .padding(horizontal = sizing.horizontalPadding, vertical = sizing.verticalPadding),
            horizontalArrangement = Arrangement.spacedBy(sizing.rowSpacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavTab.entries.forEachIndexed { index, tab ->
                val selected = selectedIndex == index
                val iconModifier = if (tab == NavTab.COMMISSION) {
                    Modifier
                        .width((sizing.iconSize * COMMISSION_ASPECT_RATIO).coerceAtMost(sizing.iconBox - 4.dp))
                        .height(sizing.iconSize)
                } else {
                    Modifier.size(sizing.iconSize)
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(role = Role.Tab, onClickLabel = tab.label) {
                            onTabSelected(index)
                        }
                        .padding(top = 4.dp, bottom = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(sizing.rowSpacing)
                ) {
                    Box(
                        modifier = Modifier
                            .size(sizing.iconBox)
                            .clip(ICON_CORNER)
                            .background(if (selected) Green else Color.Transparent)
                            .then(
                                if (selected) Modifier.border(HardStyle.Border, Ink, ICON_CORNER)
                                else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(tab.icon),
                            contentDescription = tab.label,
                            modifier = iconModifier
                        )
                    }

                    Text(
                        text = tab.label,
                        color = Ink,
                        fontSize = sizing.labelSize,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}