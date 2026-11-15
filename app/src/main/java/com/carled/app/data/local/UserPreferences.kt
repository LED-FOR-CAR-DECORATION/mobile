package com.carled.app.data.local

import android.content.Context
import android.content.SharedPreferences

class UserPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("car_led_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_AUTH_TOKEN      = "auth_token"
        private const val KEY_REFRESH_TOKEN   = "refresh_token"
        private const val KEY_USER_ID         = "user_id"
        private const val KEY_USER_EMAIL      = "user_email"
        private const val KEY_LAST_DEVICE_MAC = "last_device_mac"
        private const val KEY_CACHED_DEVICES = "cached_devices_json"
        private const val PREFIX_CACHED_DEVICE_DETAIL = "cached_device_detail_"
    }

    // --- Access token ---


    fun saveCachedDevices(json: String) {
        prefs.edit().putString(KEY_CACHED_DEVICES, json).apply()
    }

    fun getCachedDevices(): String? {
        return prefs.getString(KEY_CACHED_DEVICES, null)
    }

    // --- Device Detail Caching ---
    fun saveCachedDeviceDetail(deviceId: Long, json: String) {
        prefs.edit().putString("$PREFIX_CACHED_DEVICE_DETAIL$deviceId", json).apply()
    }

    fun getCachedDeviceDetail(deviceId: Long): String? {
        return prefs.getString("$PREFIX_CACHED_DEVICE_DETAIL$deviceId", null)
    }
    fun saveAuthToken(token: String?) {
        prefs.edit().putString(KEY_AUTH_TOKEN, token).apply()
    }

    fun getAuthToken(): String? = prefs.getString(KEY_AUTH_TOKEN, null)

    // --- Refresh token ---

    fun saveRefreshToken(token: String) {
        prefs.edit().putString(KEY_REFRESH_TOKEN, token).apply()
    }

    fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH_TOKEN, null)

    // --- User identity ---

    fun saveUser(id: String, email: String) {
        prefs.edit()
            .putString(KEY_USER_ID, id)
            .putString(KEY_USER_EMAIL, email)
            .apply()
    }

    fun getUserEmail(): String? = prefs.getString(KEY_USER_EMAIL, null)
    fun getUserId(): String?    = prefs.getString(KEY_USER_ID, null)

    // --- BLE ---

    fun saveLastDeviceMac(mac: String) {
        prefs.edit().putString(KEY_LAST_DEVICE_MAC, mac).apply()
    }

    fun getLastDeviceMac(): String? = prefs.getString(KEY_LAST_DEVICE_MAC, null)

    // --- Session ---

    /** Wipes all stored credentials and preferences. */
    fun clear() {
        prefs.edit().clear().apply()
    }

}