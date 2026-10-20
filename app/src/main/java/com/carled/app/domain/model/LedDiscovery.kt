package com.carled.app.domain.model

/** UUID is authoritative; the firmware's CarLED name is a scan-response fallback. */
fun isLedAdvertisement(name: String?, hasLedService: Boolean): Boolean =
    hasLedService || name?.startsWith("CarLED", ignoreCase = true) == true
