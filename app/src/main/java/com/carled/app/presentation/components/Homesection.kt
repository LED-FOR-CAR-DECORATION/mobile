package com.carled.app.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carled.app.presentation.theme.Cream
import com.carled.app.presentation.theme.Green
import com.carled.app.presentation.theme.GreenDark
import com.carled.app.presentation.theme.GreenLight
import com.carled.app.presentation.theme.Ink

private val SectionTitleSize = 18.sp


@Composable
fun HomeSection(
    title: String,
    badge: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier
            .fillMaxWidth()
            .hardSurface(color = Green, shadowColor = GreenDark)
            .padding(12.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, color = Ink, fontSize = SectionTitleSize, fontWeight = FontWeight.ExtraBold)
            if (badge != null) {
                Text(badge, color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(10.dp))
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}

/** "Welcome back, {name}!" card with the nested profile row (avatar · name · email · Edit). */
@Composable
fun WelcomeCard(
    name: String,
    email: String,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    HomeSection(title = "Welcome back, $name!", modifier = modifier) {
        Row(
            Modifier
                .fillMaxWidth()
                .flatSurface(color = Cream)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(GreenLight)
                    .border(HardStyle.Border, Ink, CircleShape),
                contentAlignment = Alignment.Center
            ) { PersonGlyph(size = 26.dp) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(name, color = Ink, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                if (email.isNotBlank()) {
                    Text("Email: $email", color = MirageMuted, fontSize = 13.sp)
                }
                Text(
                    "Edit",
                    color = GreenDark,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable(onClick = onEdit)
                )
            }
        }
    }
}

@Composable
fun QuickStartRow(
    @androidx.annotation.DrawableRes icon: Int,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier
            .fillMaxWidth()
            .flatSurface(color = Cream)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(44.dp).background(Ink, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Image(painterResource(icon), contentDescription = null, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text,
            color = Ink,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        ChevronGlyph()
    }
}

@Composable
fun SavedProfileRow(
    preview: String,
    name: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier
            .fillMaxWidth()
            .flatSurface(color = Cream)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(44.dp).background(Color.Black, RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(preview, color = Green, fontSize = 14.sp, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.width(12.dp))
        Text(
            name,
            color = Ink,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        ChevronGlyph()
    }
}