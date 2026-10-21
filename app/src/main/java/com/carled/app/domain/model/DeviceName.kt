package com.carled.app.domain.model

/** Complete local name occupies at most 29 bytes in a legacy BLE scan response. */
fun isValidDeviceName(value: String): Boolean {
    val name = value.trim()
    return name.toByteArray(Charsets.UTF_8).size in 1..29 && name.none { it.code < 32 || it.code == 127 }
}
