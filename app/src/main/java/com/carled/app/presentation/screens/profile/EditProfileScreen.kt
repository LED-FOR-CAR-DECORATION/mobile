package com.carled.app.presentation.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carled.app.R
import com.carled.app.presentation.components.*
import com.carled.app.presentation.theme.Cream
import com.carled.app.presentation.theme.ErrorRed
import com.carled.app.presentation.theme.GreenDark
import com.carled.app.presentation.theme.Ink
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Composable
fun EditProfileScreen(
    viewModel: EditProfileViewModel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    EditProfileScreenContent(
        state = uiState,
        onFullNameChange = viewModel::onFullNameChange,
        onPhoneChange = viewModel::onPhoneChange,
        onBirthDateChange = viewModel::onBirthDateChange,
        onSubmit = viewModel::submit,
        onClose = onClose,
        modifier = modifier
    )
}

@Composable
private fun EditProfileScreenContent(
    state: EditProfileUiState,
    onFullNameChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onBirthDateChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier
            .fillMaxSize()
            .background(MirageBackground)
            .systemBarsPadding()
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        WindowFrame(
            title = "Chỉnh sửa thông tin cá nhân",
            onClose = onClose,
            compact = true,
            modifier = Modifier
                .widthIn(max = 360.dp)
                .wrapContentHeight()
        ) {
            Text(
                "Thông tin cá nhân",
                color = Ink,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold
            )

            // Email can't be changed from here, so it's a read-only row, not a text field.
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Email", color = Ink, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Box(
                    Modifier
                        .fillMaxWidth()
                        .flatSurface(color = Cream)
                        .padding(horizontal = 10.dp, vertical = 10.dp)
                ) {
                    Text(state.email, color = MirageMuted, fontSize = 15.sp)
                }
            }

            // Required by PUT /api/users/me on every save — see
            // EditProfileViewModel's doc for why this can't be left out.
            HardTextField(
                value = state.fullName,
                onValueChange = onFullNameChange,
                label = "Họ và tên",
                enabled = !state.isSaving,
                imeAction = ImeAction.Next,
                modifier = Modifier.fillMaxWidth()
            )

            HardTextField(
                value = state.phone,
                onValueChange = onPhoneChange,
                label = "Số điện thoại",
                enabled = !state.isSaving,
                keyboardType = KeyboardType.Phone,
                imeAction = ImeAction.Next,
                modifier = Modifier.fillMaxWidth()
            )

            BirthDateField(
                value = state.birthDate,
                onValueChange = onBirthDateChange,
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth()
            )

            state.errorMessage?.let { Text(it, color = ErrorRed, fontSize = 13.sp) }
            state.successMessage?.let {
                Text(it, color = GreenDark, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }

            HardButton(
                text = "Lưu thay đổi",
                onClick = onSubmit,
                enabled = !state.isSaving,
                loading = state.isSaving,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .widthIn(min = 160.dp)
            )
        }
    }
}

/** dd/MM/yyyy, interpreted in UTC — matches DatePickerState, which also works
 * in UTC millis. Using the same zone on both sides means the day the user
 * taps is exactly the day that gets formatted and stored, with no shift from
 * the device's local timezone. [EditProfileViewModel] parses with the same
 * zone, so the saved string and the validation agree. */
private fun birthDateFormat(): SimpleDateFormat =
    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply {
        isLenient = false
        timeZone = TimeZone.getTimeZone("UTC")
    }

/** Midnight UTC, [yearsAgo] years before today — used to cap the picker at
 * "not in the future" and "at least 13 years old". */
private fun utcMillisYearsAgo(yearsAgo: Int): Long =
    Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        add(Calendar.YEAR, -yearsAgo)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

/**
 * Birth date is picked, not typed. Tapping the field opens a Material
 * DatePickerDialog restricted to dates that are neither in the future nor
 * less than 13 years ago, so an invalid date can't be selected in the first
 * place; [EditProfileViewModel.submit] re-checks the same two rules before
 * saving, since the dialog only guards the UI path.
 *
 * If [value] is already filled in (e.g. loaded from [EditProfileViewModel]),
 * the dialog opens pre-selected on that date; if it's blank, the dialog opens
 * with no date selected and the field shows a placeholder.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BirthDateField(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    var showPicker by remember { mutableStateOf(false) }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Ngày sinh", color = Ink, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        Row(
            Modifier
                .fillMaxWidth()
                .flatSurface(color = Cream)
                .clickable(enabled = enabled) { showPicker = true }
                .padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = value.ifBlank { "dd/MM/yyyy" },
                color = if (value.isBlank()) MirageMuted else Ink,
                fontSize = 15.sp
            )
            Icon(
                painter = painterResource(R.drawable.ic_calendar),
                contentDescription = null,
                tint = Ink
            )
        }
    }

    if (showPicker) {
        // Latest selectable day: today, but no closer than exactly 13 years
        // after the earliest birth date — i.e. the person must be 13+.
        val maxSelectableMillis = remember { utcMillisYearsAgo(13) }
        val maxSelectableYear = remember(maxSelectableMillis) {
            Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                .apply { timeInMillis = maxSelectableMillis }
                .get(Calendar.YEAR)
        }
        val selectableDates = remember(maxSelectableMillis) {
            object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    utcTimeMillis <= maxSelectableMillis

                override fun isSelectableYear(year: Int): Boolean =
                    year <= maxSelectableYear
            }
        }

        // Pre-select the existing date when there is one, so reopening the
        // picker doesn't lose what was already chosen.
        val initialMillis = remember(value) {
            runCatching { value.takeIf { it.isNotBlank() }?.let { birthDateFormat().parse(it)?.time } }
                .getOrNull()
        }
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialMillis,
            selectableDates = selectableDates
        )

        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        onValueChange(birthDateFormat().format(Date(millis)))
                    }
                    showPicker = false
                }) { Text("Chọn") }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("Hủy") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun EditProfileScreenPreview() {
    EditProfileScreenContent(
        state = EditProfileUiState(email = "user@example.com", fullName = "Nguyễn Văn A", phone = "0901234567", birthDate = "01/01/2000"),
        onFullNameChange = {}, onPhoneChange = {}, onBirthDateChange = {}, onSubmit = {}, onClose = {}
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun EditProfileScreenEmptyBirthDatePreview() {
    EditProfileScreenContent(
        state = EditProfileUiState(email = "user@example.com", fullName = "Nguyễn Văn A", phone = "0901234567", birthDate = ""),
        onFullNameChange = {}, onPhoneChange = {}, onBirthDateChange = {}, onSubmit = {}, onClose = {}
    )
}