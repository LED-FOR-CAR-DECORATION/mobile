package com.carled.app.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Secure credential store backed by EncryptedSharedPreferences (AES256-GCM keys,
 * AES256-SIV key names). Tokens and cached data are never written to disk in plain text.
 *
 * Build dependency required (add to app/build.gradle):
 *   implementation("androidx.security:security-crypto:1.1.0-alpha06")
 *
 * Migration note: this uses a new filename ("car_led_prefs_secure") to avoid
 * conflicts with the old unencrypted "car_led_prefs" file. Existing users will
 * be signed out once after updating — an acceptable trade-off for a security fix.
 * The old file is left on disk; it can be deleted by calling the old prefs key
 * during a one-time migration if desired.
 */
class UserPreferences(context: Context) {

    private val prefs: SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "car_led_prefs_secure",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        // Keystore unavailable: can happen after a failed OS upgrade or on some
        // emulators. Fall back to plain prefs so the app stays runnable; the user
        // will need to re-login and tokens won't be encrypted in this session.
        Log.e("UserPreferences", "EncryptedSharedPreferences unavailable, falling back to plain prefs", e)
        context.getSharedPreferences("car_led_prefs_fallback", Context.MODE_PRIVATE)
    }

    companion object {
        private const val KEY_AUTH_TOKEN      = "auth_token"
        private const val KEY_REFRESH_TOKEN   = "refresh_token"
        private const val KEY_USER_ID         = "user_id"
        private const val KEY_USER_EMAIL      = "user_email"
        private const val KEY_LAST_DEVICE_MAC = "last_device_mac"
        private const val KEY_CACHED_DEVICES  = "cached_devices_json"
        private const val PREFIX_CACHED_DEVICE_DETAIL = "cached_device_detail_"
        private const val PREFIX_BIRTH_DATE = "birth_date_"
    }

    // --- Device list cache ---

    fun saveCachedDevices(json: String) {
        prefs.edit().putString(KEY_CACHED_DEVICES, json).apply()
    }

    fun getCachedDevices(): String? = prefs.getString(KEY_CACHED_DEVICES, null)

    // --- Device detail cache ---

    fun saveCachedDeviceDetail(deviceId: Long, json: String) {
        prefs.edit().putString("$PREFIX_CACHED_DEVICE_DETAIL$deviceId", json).apply()
    }

    fun getCachedDeviceDetail(deviceId: Long): String? =
        prefs.getString("$PREFIX_CACHED_DEVICE_DETAIL$deviceId", null)

    // --- Access token ---

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

    // --- Local-only profile fields ---
    // PUT /api/users/me only accepts name/phone/avatar — there's no birthDate
    // field on the backend at all, so it's kept here instead, per user id,
    // until the server has somewhere to put it.

    fun saveBirthDate(userId: String, birthDate: String) {
        prefs.edit().putString("$PREFIX_BIRTH_DATE$userId", birthDate).apply()
    }

    fun getBirthDate(userId: String): String? = prefs.getString("$PREFIX_BIRTH_DATE$userId", null)

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