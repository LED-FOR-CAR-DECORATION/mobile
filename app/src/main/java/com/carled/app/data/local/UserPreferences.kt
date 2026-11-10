package com.carled.app.data.local

import android.content.Context
import android.content.SharedPreferences

class UserPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("car_led_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_TOKEN_TYPE = "token_type"
        private const val KEY_TOKEN_EXPIRES_AT = "token_expires_at"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_ACCOUNT_TYPE = "account_type"
        private const val KEY_ACCOUNT_STATUS = "account_status"
        private const val KEY_LAST_DEVICE_MAC = "last_device_mac"
    }

    // Token management
    fun saveAuthToken(token: String?) {
        prefs.edit().putString(KEY_AUTH_TOKEN, token).apply()
    }

    fun getAuthToken(): String? = prefs.getString(KEY_AUTH_TOKEN, null)

    fun saveTokens(
        accessToken: String,
        refreshToken: String,
        tokenType: String = "Bearer",
        expiresInSeconds: Long = 900L
    ) {
        val expiresAt = System.currentTimeMillis() + (expiresInSeconds * 1000L)
        prefs.edit()
            .putString(KEY_AUTH_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .putString(KEY_TOKEN_TYPE, tokenType)
            .putLong(KEY_TOKEN_EXPIRES_AT, expiresAt)
            .apply()
    }

    fun getAccessToken(): String? = getAuthToken()
    fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH_TOKEN, null)
    fun getTokenType(): String = prefs.getString(KEY_TOKEN_TYPE, "Bearer") ?: "Bearer"
    fun getTokenExpiresAt(): Long = prefs.getLong(KEY_TOKEN_EXPIRES_AT, 0L)

    fun clearTokens() {
        prefs.edit()
            .remove(KEY_AUTH_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_TOKEN_TYPE)
            .remove(KEY_TOKEN_EXPIRES_AT)
            .apply()
    }

    // User management
    fun saveUser(id: String, email: String) {
        prefs.edit()
            .putString(KEY_USER_ID, id)
            .putString(KEY_USER_EMAIL, email)
            .apply()
    }

    fun saveUserDetails(
        id: String,
        email: String,
        accountType: String? = null,
        status: String? = null
    ) {
        prefs.edit()
            .putString(KEY_USER_ID, id)
            .putString(KEY_USER_EMAIL, email)
            .putString(KEY_ACCOUNT_TYPE, accountType)
            .putString(KEY_ACCOUNT_STATUS, status)
            .apply()
    }

    fun getUserEmail(): String? = prefs.getString(KEY_USER_EMAIL, null)
    fun getUserId(): String? = prefs.getString(KEY_USER_ID, null)
    fun getAccountType(): String? = prefs.getString(KEY_ACCOUNT_TYPE, null)
    fun getAccountStatus(): String? = prefs.getString(KEY_ACCOUNT_STATUS, null)

    // Hardware/BLE preferences
    fun saveLastDeviceMac(mac: String) {
        prefs.edit().putString(KEY_LAST_DEVICE_MAC, mac).apply()
    }

    fun getLastDeviceMac(): String? = prefs.getString(KEY_LAST_DEVICE_MAC, null)

    fun clear() {
        prefs.edit().clear().apply()
    }
}
