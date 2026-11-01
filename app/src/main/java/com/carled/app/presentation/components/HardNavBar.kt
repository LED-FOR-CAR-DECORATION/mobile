package com.carled.app.presentation.components


import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.carled.app.R
import com.carled.app.presentation.theme.*

private enum class NavTab(val label: String, @DrawableRes val icon: Int) {
    HOME("Home", R.drawable.ic_home),
    GALLERY("Gallery", R.drawable.ic_gallery),
    PROFILE("Profile", R.drawable.ic_profile),
    DEVICES("My Devices", R.drawable.ic_settings)
}



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
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            NavTab.entries.forEachIndexed { index, tab ->
                val selected = selectedIndex == index

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
                            modifier = Modifier.size(sizing.iconSize)
                        )
                    }

                    Text(
                        text = tab.label,
                        color = Ink,
                        fontSize = sizing.labelSize,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

