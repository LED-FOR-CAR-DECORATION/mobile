package com.carled.app.data.ble

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat

/**
 * Permission(s) needed to read the phone's currently-connected Wi-Fi SSID.
 * Android has gated SSID access behind location permission since API 27 (an
 * SSID can reveal physical location); API 33 adds NEARBY_WIFI_DEVICES as an
 * alternative. Both are requested together so whichever the OS version
 * actually honors is covered — see AndroidManifest note below this file.
 */
fun wifiSsidPermissions(): Array<String> = if (Build.VERSION.SDK_INT >= 33) {
    arrayOf(Manifest.permission.NEARBY_WIFI_DEVICES, Manifest.permission.ACCESS_FINE_LOCATION)
} else {
    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
}

private fun hasWifiSsidPermission(context: Context): Boolean =
    wifiSsidPermissions().any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }

/**
 * SSID of the Wi-Fi network the phone is currently connected to, or null if
 * the phone isn't on Wi-Fi, permission is missing, or (pre-API 31) location
 * services are off — all conditions under which Android itself withholds the
 * real SSID and returns a placeholder instead.
 *
 * HARD LIMIT: this can only ever return the network NAME. There is no
 * Android API, at any permission level, that exposes a saved network's
 * password back to an app — that's an OS security boundary, not something
 * this function is missing. The user must still type the Wi-Fi password.
 */
fun currentWifiSsid(context: Context): String? {
    if (!hasWifiSsidPermission(context)) return null

    if (Build.VERSION.SDK_INT < 31) {
        val location = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        if (location == null || !LocationManagerCompat.isLocationEnabled(location)) return null
    }

    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        ?: return null
    val network = connectivityManager.activeNetwork ?: return null
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return null

    val wifiInfo: WifiInfo? = if (Build.VERSION.SDK_INT >= 29) {
        capabilities.transportInfo as? WifiInfo
    } else {
        @Suppress("DEPRECATION")
        (context.getSystemService(Context.WIFI_SERVICE) as? WifiManager)?.connectionInfo
    }

    val rawSsid = wifiInfo?.ssid ?: return null
    if (rawSsid.isBlank() || rawSsid == WifiManager.UNKNOWN_SSID) return null
    return rawSsid.trim('"')
}