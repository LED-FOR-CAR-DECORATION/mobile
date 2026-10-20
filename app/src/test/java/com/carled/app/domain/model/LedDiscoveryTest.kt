package com.carled.app.domain.model

import org.junit.Assert.*
import org.junit.Test

class LedDiscoveryTest {
    @Test fun recognizesFirmwareNameEvenWhenScanResponseOmitsUuid() {
        assertTrue(isLedAdvertisement("CarLED-Ambient", false))
        assertTrue(isLedAdvertisement("carled-garage", false))
    }
    @Test fun recognizesUnnamedOrRenamedLedByServiceUuid() {
        assertTrue(isLedAdvertisement(null, true))
        assertTrue(isLedAdvertisement("My matrix", true))
    }
    @Test fun excludesOtherBluetoothAndGenericEsp32Devices() {
        listOf(null, "", "ESP32", "ESP32-Thermometer", "Galaxy M20", "OPPO Enco Buds", "MyCarLEDspeaker").forEach {
            assertFalse("Unrelated device: $it", isLedAdvertisement(it, false))
        }
    }
}
