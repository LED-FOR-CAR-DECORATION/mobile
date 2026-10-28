package com.carled.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carled.app.presentation.theme.FieldGrey
import com.carled.app.presentation.theme.Ink

@Composable
fun SocialLoginBar(
    onProviderClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    providers: List<String> = listOf("Google", "Facebook", "Apple", "X"),
    enabled: Boolean = true
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Login with", color = Ink, fontSize = 13.sp)
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally)
        ) {
            providers.forEach { name ->
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(FieldGrey)
                        .clickable(enabled = enabled, role = Role.Button, onClickLabel = "Login with $name") {
                            onProviderClick(name)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(name.first().toString(), color = Ink, fontSize = 13.sp)
                }
            }
        }
    }
}

