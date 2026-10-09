package com.carled.app.data.local

import android.content.Context
import android.content.SharedPreferences

class UserPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("car_led_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_LAST_DEVICE_MAC = "last_device_mac"
    }

    fun saveAuthToken(token: String?) {
        prefs.edit().putString(KEY_AUTH_TOKEN, token).apply()
    }

    fun getAuthToken(): String? = prefs.getString(KEY_AUTH_TOKEN, null)

    fun saveUser(id: String, email: String) {
        prefs.edit()
            .putString(KEY_USER_ID, id)
            .putString(KEY_USER_EMAIL, email)
            .apply()
    }

    fun getUserEmail(): String? = prefs.getString(KEY_USER_EMAIL, null)
    fun getUserId(): String? = prefs.getString(KEY_USER_ID, null)

    fun saveLastDeviceMac(mac: String) {
        prefs.edit().putString(KEY_LAST_DEVICE_MAC, mac).apply()
    }

    fun getLastDeviceMac(): String? = prefs.getString(KEY_LAST_DEVICE_MAC, null)

    fun clear() {
        prefs.edit().clear().apply()
    }
}
