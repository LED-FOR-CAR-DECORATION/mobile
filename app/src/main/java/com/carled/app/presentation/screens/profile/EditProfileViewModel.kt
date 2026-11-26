package com.carled.app.presentation.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carled.app.core.common.Resource
import com.carled.app.data.local.UserPreferences
import com.carled.app.domain.model.User
import com.carled.app.domain.usecase.auth.UpdateProfileUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import java.util.regex.Pattern

/**
 * Backs "Chỉnh sửa thông tin cá nhân".
 *
 * PUT /api/users/me only accepts name, phone and avatar — email, credentials
 * and roles can't change here. [UpdateProfileUseCase] rejects a blank name
 * client-side ("Thiếu tên tài khoản") because the endpoint requires it on
 * every save, even a save that's only really changing phone or birth date.
 *
 * Earlier this screen re-sent [User.fullName] unchanged without exposing it
 * for editing. That silently failed whenever the in-memory [User] had an
 * empty name — e.g. right after a cold start, before
 * [com.carled.app.domain.repository.AuthRepository.getProfile] has resolved,
 * [com.carled.app.data.repository.AuthRepositoryImpl.buildRestoredUser]
 * rebuilds the session with `fullName = ""`. So the name is now a real,
 * required field on this form instead, which both fixes that failure mode
 * and lets the user actually correct their name if it's wrong/missing.
 *
 * - shows email read-only (from [user], never sent back);
 * - lets the user edit full name, which is required and round-trips through
 *   the real API;
 * - lets the user edit phone, which round-trips through the real API too,
 *   but is optional;
 * - lets the user edit birth date, kept in [userPreferences] (keyed by
 *   [User.id]) until the backend has somewhere to put it — also optional.
 *
 * If filled in, phone and birth date are each validated before the save is
 * attempted:
 *  - phone must match a Vietnamese mobile number;
 *  - birth date must be a real, non-future date, and the person must be at
 *    least 13 years old.
 *
 * Birth date strings are always dd/MM/yyyy and are parsed/compared in UTC so
 * the stored string lines up exactly with the date the Compose DatePicker
 * (which also works in UTC millis) shows the user — this avoids off-by-one
 * day bugs near the device's local midnight.
 */
class EditProfileViewModel(
    private val user: User,
    private val updateProfileUseCase: UpdateProfileUseCase,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        EditProfileUiState(
            email = user.email,
            fullName = user.fullName.orEmpty(),
            phone = user.phone.orEmpty(),
            birthDate = userPreferences.getBirthDate(user.id).orEmpty()
        )
    )
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    // Set once the person edits anything, so a later syncFromUpstream() call
    // (triggered by the upstream User changing — e.g. a deferred getProfile()
    // finally filling in name/phone after a cold start) knows not to
    // overwrite an in-progress, not-yet-saved edit. Cleared back to false
    // right after a successful save, since at that point the upstream User
    // is about to catch up to exactly what's on screen anyway.
    private var isDirty = false

    fun onFullNameChange(value: String) {
        isDirty = true
        _uiState.update { it.copy(fullName = value, errorMessage = null, successMessage = null) }
    }

    fun onPhoneChange(value: String) {
        isDirty = true
        _uiState.update { it.copy(phone = value, errorMessage = null, successMessage = null) }
    }

    fun onBirthDateChange(value: String) {
        isDirty = true
        _uiState.update { it.copy(birthDate = value, errorMessage = null, successMessage = null) }
    }

    /**
     * Applies the latest email/fullName/phone from the upstream [user] (e.g.
     * [MirageHomeScreen]'s `LaunchedEffect(user)` calling this whenever the
     * app's current-user flow emits — including the deferred profile fetch
     * that fills in a cold-started session's blank placeholder name/phone).
     *
     * No-ops while [isDirty] is true, so this can never clobber text the
     * person has typed but not yet saved. Birth date is untouched here — it
     * isn't part of [User] at all; it only ever comes from [userPreferences].
     */
    fun syncFromUpstream(user: User) {
        if (isDirty) return
        _uiState.update {
            it.copy(
                email = user.email,
                fullName = user.fullName.orEmpty(),
                phone = user.phone.orEmpty()
            )
        }
    }

    fun submit() {
        val state = _uiState.value

        if (state.fullName.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Vui lòng nhập họ và tên") }
            return
        }

        if (state.birthDate.isNotBlank()) {
            val birthDateError = validateBirthDate(state.birthDate)
            if (birthDateError != null) {
                _uiState.update { it.copy(errorMessage = birthDateError) }
                return
            }
        }

        if (state.phone.isNotBlank() && !PHONE_PATTERN.matcher(state.phone).matches()) {
            _uiState.update { it.copy(errorMessage = "Số điện thoại không hợp lệ (VD: 0901234567)") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null, successMessage = null) }

            when (val result = updateProfileUseCase(
                name = state.fullName.trim(),
                phone = state.phone.ifBlank { null }
            )) {
                is Resource.Success -> {
                    userPreferences.saveBirthDate(user.id, state.birthDate)
                    isDirty = false
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            fullName = result.data.fullName.orEmpty(),
                            phone = result.data.phone.orEmpty(),
                            successMessage = "Đã cập nhật thông tin cá nhân!"
                        )
                    }
                }
                is Resource.Error -> _uiState.update { it.copy(isSaving = false, errorMessage = result.message) }
                is Resource.Loading -> _uiState.update { it.copy(isSaving = true) }
            }
        }
    }

    /** Returns a user-facing error message, or null if [birthDate] is valid. */
    private fun validateBirthDate(birthDate: String): String? {
        if (!DATE_PATTERN.matcher(birthDate).matches()) {
            return "Ngày sinh không hợp lệ, định dạng dd/MM/yyyy"
        }

        val birthMillis = runCatching { dateFormat().parse(birthDate)?.time }.getOrNull()
            ?: return "Ngày sinh không hợp lệ, định dạng dd/MM/yyyy"

        val nowMillis = System.currentTimeMillis()
        if (birthMillis > nowMillis) {
            return "Ngày sinh không được ở trong tương lai"
        }
        if (!isAtLeast13YearsOld(birthMillis, nowMillis)) {
            return "Bạn phải từ 13 tuổi trở lên"
        }
        return null
    }

    private fun isAtLeast13YearsOld(birthMillis: Long, nowMillis: Long): Boolean {
        val utc = TimeZone.getTimeZone("UTC")
        val birth = Calendar.getInstance(utc).apply { timeInMillis = birthMillis }
        val now = Calendar.getInstance(utc).apply { timeInMillis = nowMillis }

        var age = now.get(Calendar.YEAR) - birth.get(Calendar.YEAR)
        val hadBirthdayThisYear =
            now.get(Calendar.MONTH) > birth.get(Calendar.MONTH) ||
                    (now.get(Calendar.MONTH) == birth.get(Calendar.MONTH) &&
                            now.get(Calendar.DAY_OF_MONTH) >= birth.get(Calendar.DAY_OF_MONTH))
        if (!hadBirthdayThisYear) age--

        return age >= MIN_AGE_YEARS
    }

    /** dd/MM/yyyy, interpreted in UTC to match the DatePicker's UTC millis. */
    private fun dateFormat(): SimpleDateFormat =
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply {
            isLenient = false
            timeZone = TimeZone.getTimeZone("UTC")
        }

    private companion object {
        val DATE_PATTERN: Pattern = Pattern.compile("^\\d{2}/\\d{2}/\\d{4}$")

        // Vietnamese mobile numbers: 0 or +84, then a valid carrier-prefix
        // digit (3/5/7/8/9), then 8 more digits — e.g. 0901234567.
        val PHONE_PATTERN: Pattern = Pattern.compile("^(0|\\+84)(3|5|7|8|9)\\d{8}$")

        const val MIN_AGE_YEARS = 13
    }
}