package com.carled.app.data.ble

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.carled.app.domain.model.BleScanBlocker
import com.carled.app.domain.model.bleScanBlocker

fun bleScanPermissions(): Array<String> = if (Build.VERSION.SDK_INT >= 31)
    arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
else arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)

fun readBleScanBlocker(context: Context): BleScanBlocker? {
    val adapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
    val allowed = bleScanPermissions().all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }
    val enabled = if (allowed) try { adapter?.isEnabled == true } catch (_: SecurityException) { false } else false
    val locationEnabled = if (Build.VERSION.SDK_INT >= 31) true else {
        val location = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        location != null && LocationManagerCompat.isLocationEnabled(location)
    }
    return bleScanBlocker(Build.VERSION.SDK_INT, allowed, adapter != null, enabled, locationEnabled)
}
