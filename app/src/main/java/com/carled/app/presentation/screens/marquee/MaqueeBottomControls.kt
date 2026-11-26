package com.carled.app.presentation.screens.marquee

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carled.app.R
import com.carled.app.presentation.components.MirageInk
import com.carled.app.presentation.components.MirageMuted
import com.carled.app.presentation.components.flatSurface
import com.carled.app.presentation.components.hardSurface
import com.carled.app.presentation.theme.*
import kotlin.math.roundToInt

internal enum class MenuAnchor { TOP, BOTTOM }

@Composable
internal fun MarqueeTitleBar(
    title: String,
    connected: Boolean,
    sending: Boolean,
    showMenuOnTop: Boolean,
    onMenu: () -> Unit,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(Cream)
            .border(2.dp, Ink),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showMenuOnTop) {
            BurgerMenuButton(
                onClick = onMenu,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Box(
            modifier = Modifier
                .padding(start = if (showMenuOnTop) 4.dp else 10.dp)
                .size(16.dp)
                .clip(CircleShape)
                .background(if (connected) Green else Color(0xFF9FB5A5))
                .border(1.5.dp, Ink, CircleShape)
        )

        Text(
            text = title,
            color = Ink,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .padding(start = 8.dp)
                .weight(1f)
        )

        if (sending) {
            CircularProgressIndicator(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(16.dp),
                strokeWidth = 2.dp,
                color = GreenDark
            )
        }

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(48.dp)
                .background(Green)
                .border(1.5.dp, Ink)
                .clickable(onClick = onClose),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "X",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun BurgerMenuButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(30.dp)
            .flatSurface(color = Color.White, borderWidth = 1.5.dp)
            .clickable(onClick = onClick)
            .testTag("marqueeBurgerMenu"),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Menu,
            contentDescription = "Menu",
            tint = Ink,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
internal fun MarqueeBottomBar(
    r: Int,
    g: Int,
    b: Int,
    scrollMode: String,
    speed: Int,
    blinkSpeed: Int,
    isSending: Boolean,
    enabled: Boolean,
    canSend: Boolean,
    sizeLabel: String,
    menuAnchor: MenuAnchor,
    hasProfiles: Boolean,
    editable: Boolean,
    hasAnyProfile: Boolean,
    canUpdateNow: Boolean,
    canCreateNow: Boolean,
    profileNameInput: String,
    fallbackProfileName: String,
    onProfileNameChange: (String) -> Unit,
    onOpenColor: () -> Unit,
    onSelectMode: (String) -> Unit,
    onSpeedChange: (Int) -> Unit,
    onBlinkSpeedChange: (Int) -> Unit,
    onSend: () -> Unit,
    onOpenMenu: () -> Unit,
    onOpenSize: () -> Unit,
    onUpdateProfile: () -> Unit,
    onCreateProfile: () -> Unit,
    onSwitchProfile: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(FieldGrey)
                .border(width = 1.dp, color = MirageInk.copy(alpha = 0.2f))
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BurgerMenuButton(
                onClick = onOpenMenu,
                modifier = Modifier.testTag(
                    if (menuAnchor == MenuAnchor.BOTTOM) "marqueeBottomMenuActive" else "marqueeBottomMenu"
                )
            )

            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(r, g, b))
                    .border(1.5.dp, MirageInk, RoundedCornerShape(8.dp))
                    .clickable(enabled = enabled, onClick = onOpenColor)
                    .testTag("marqueeColorSwatch")
            )

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(28.dp)
                    .background(MirageInk.copy(alpha = 0.25f))
            )

            val modes = listOf("scroll_left", "static", "blink")
            val labels = listOf("Cuộn trái", "Đứng yên", "Nhấp nháy")
            modes.forEachIndexed { idx, modeKey ->
                val isSelected = scrollMode == modeKey
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .alpha(if (enabled) 1f else 0.5f)
                        .then(
                            if (isSelected) Modifier.hardSurface(
                                color = Green,
                                offset = 2.dp,
                                borderWidth = 1.5.dp,
                                shadowColor = GreenDark
                            ) else Modifier.flatSurface(
                                color = Color(0xFFD6D8D4),
                                borderWidth = 1.5.dp
                            )
                        )
                        .clickable(enabled = enabled) { onSelectMode(modeKey) }
                        .padding(horizontal = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = labels[idx],
                        color = if (isSelected) Color.White else Ink,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(38.dp)
                    .alpha(if (canSend) 1f else 0.45f)
                    .hardSurface(
                        color = if (canSend) GreenLight else Cream,
                        offset = 2.dp,
                        borderWidth = 1.5.dp,
                        shadowColor = GreenDark
                    )
                    .clickable(enabled = canSend, onClick = onSend)
                    .testTag("sendMarqueeButton"),
                contentAlignment = Alignment.Center
            ) {
                if (isSending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Ink
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Hiển thị trên LED",
                        tint = Ink,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        if (scrollMode == "scroll_left" || scrollMode == "blink") {
            val isBlink = scrollMode == "blink"
            val value = if (isBlink) blinkSpeed else speed
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(FieldGrey)
                    .border(width = 1.dp, color = MirageInk.copy(alpha = 0.15f))
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (isBlink) "Tốc độ nháy: $value" else "Tốc độ cuộn: $value",
                    color = Ink,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Slider(
                    value = value.toFloat(),
                    onValueChange = {
                        if (isBlink) onBlinkSpeedChange(it.roundToInt())
                        else onSpeedChange(it.roundToInt())
                    },
                    valueRange = 1f..100f,
                    enabled = enabled,
                    modifier = Modifier
                        .weight(1f)
                        .height(28.dp)
                        .testTag(if (isBlink) "blinkSpeedSlider" else "scrollSpeedSlider")
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Cream)
                .border(width = 1.dp, color = MirageInk.copy(alpha = 0.25f))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .clickable(enabled = enabled, onClick = onOpenSize)
                    .border(1.5.dp, Ink, RoundedCornerShape(4.dp))
                    .background(Color.White, RoundedCornerShape(4.dp))
                    .padding(horizontal = 7.dp, vertical = 4.dp)
                    .testTag("canvasSizeBadge")
            ) {
                Text(
                    text = sizeLabel,
                    color = Ink,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    softWrap = false
                )
            }

            if (hasProfiles) {
                IconButton(
                    onClick = onUpdateProfile,
                    enabled = canUpdateNow,
                    modifier = Modifier.size(32.dp).testTag("updateProfileButton")
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_save),
                        contentDescription = "Cập nhật profile",
                        tint = Color.Unspecified,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onCreateProfile,
                    enabled = canCreateNow,
                    modifier = Modifier.size(32.dp).testTag("createProfileButton")
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_edit),
                        contentDescription = "Tạo profile mới",
                        tint = Color.Unspecified,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .height(30.dp)
                        .alpha(if (editable && hasAnyProfile) 1f else 0.5f)
                        .flatSurface(color = Color(0xFFD6D8D4), borderWidth = 1.5.dp)
                        .clickable(enabled = editable && hasAnyProfile, onClick = onSwitchProfile)
                        .padding(horizontal = 8.dp)
                        .testTag("switchProfileButton"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Đổi profile",
                        color = Ink,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            BasicTextField(
                value = profileNameInput,
                onValueChange = onProfileNameChange,
                enabled = hasProfiles && enabled,
                singleLine = true,
                textStyle = TextStyle(
                    color = Ink,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                ),
                cursorBrush = SolidColor(Ink),
                decorationBox = { inner ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Ink.copy(alpha = 0.35f), RoundedCornerShape(4.dp))
                            .background(Color.White.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (profileNameInput.isEmpty()) {
                            Text(
                                text = fallbackProfileName.ifBlank { "Profile Name" },
                                color = MirageMuted,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        inner()
                    }
                },
                modifier = Modifier.weight(1f).testTag("profileNameField")
            )
        }
    }
}