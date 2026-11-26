package com.carled.app.presentation.screens.commission

import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.carled.app.domain.model.commission.CommissionSummaryResponse
import com.carled.app.presentation.theme.Cream
import com.carled.app.presentation.theme.Green
import com.carled.app.presentation.theme.Ink
import java.text.NumberFormat
import java.util.Locale

internal const val MAX_REFERENCES = 5
internal val CardShape = RoundedCornerShape(3.dp)
internal const val ImagePreviewAspectRatio = 4f / 3f
internal val ImagePickerRequest = PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)

internal fun formatVnd(amount: Double): String =
    NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN")).format(amount) + " VND"

internal fun formatVndAmount(amount: Long): String =
    NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN")).format(amount) + " VND"

@Composable
internal fun CommissionPanel(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Green)
            .border(2.dp, Ink)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SectionTitle(title)
        content()
    }
}

@Composable
internal fun SectionTitle(title: String, modifier: Modifier = Modifier) {
    Text(title, modifier, color = Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
}

@Composable
internal fun CommissionField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    multiline: Boolean = false,
    numeric: Boolean = false,
    readOnly: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label, color = Ink) },
        readOnly = readOnly,
        singleLine = !multiline,
        minLines = if (multiline) 4 else 1,
        keyboardOptions = if (numeric) KeyboardOptions(keyboardType = KeyboardType.Number) else KeyboardOptions.Default,
        shape = CardShape,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Cream,
            unfocusedContainerColor = Cream,
            focusedTextColor = Ink,
            unfocusedTextColor = Ink,
            focusedBorderColor = Ink,
            unfocusedBorderColor = Ink
        )
    )
}

@Composable
internal fun RequestCard(commission: CommissionSummaryResponse, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Cream)
            .border(1.dp, Ink)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text("#${commission.commissionId} · ${commission.title}", color = Ink, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(formatVnd(commission.totalPrice), Modifier.weight(1f), color = Ink, style = MaterialTheme.typography.bodySmall)
            Text(commission.status, color = Ink, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
    }
}