package com.carled.app

import com.carled.app.domain.model.isValidDeviceName
import com.carled.app.domain.model.isLedAdvertisement
import org.junit.Assert.*
import org.junit.Test

class DeviceNameTest {
    @Test fun allowsDefaultAndCustomNames() {
        assertTrue(isValidDeviceName("CarLED-Ambient"))
        assertTrue(isValidDeviceName("Xe của tôi"))
        assertTrue(isValidDeviceName("A".repeat(29)))
    }
    @Test fun limitsUtf8BytesRatherThanCharacterCount() {
        assertFalse(isValidDeviceName("A".repeat(30)))
        assertTrue(isValidDeviceName("ế".repeat(9)))
        assertFalse(isValidDeviceName("ế".repeat(10)))
    }
    @Test fun rejectsEmptyAndControlCharacters() {
        assertFalse(isValidDeviceName("   "))
        assertFalse(isValidDeviceName("Xe\nLED"))
        assertFalse(isValidDeviceName("Xe\u0000LED"))
    }
    @Test fun customNameStillDiscoverableByLedService() {
        assertTrue(isLedAdvertisement("Xe của tôi", true))
        assertFalse(isLedAdvertisement("Tai nghe", false))
    }
}
