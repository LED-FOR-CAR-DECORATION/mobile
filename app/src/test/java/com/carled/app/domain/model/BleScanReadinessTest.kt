package com.carled.app.domain.model

import org.junit.Assert.*
import org.junit.Test

class BleScanReadinessTest {
    @Test fun android10WithPermissionButLocationOffCannotReceiveScanResults() {
        assertEquals(BleScanBlocker.LOCATION_OFF, bleScanBlocker(29, true, true, true, false))
    }
    @Test fun android10IsReadyAfterEnablingLocation() {
        assertNull(bleScanBlocker(29, true, true, true, true))
    }
    @Test fun android12DoesNotRequireLocationForNearbyDeviceScan() {
        assertNull(bleScanBlocker(31, true, true, true, false))
    }
    @Test fun missingPermissionIsNotReportedAsAnEmptyScan() {
        assertEquals(BleScanBlocker.PERMISSION, bleScanBlocker(29, false, true, true, true))
    }
    @Test fun disabledBluetoothIsActionableBeforeScanning() {
        assertEquals(BleScanBlocker.BLUETOOTH_OFF, bleScanBlocker(29, true, true, false, true))
    }
}
