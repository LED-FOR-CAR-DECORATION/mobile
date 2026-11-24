package com.carled.app.domain.model

import com.carled.app.data.ble.TelemetryJsonAssembler
import com.carled.app.data.remote.dto.ApiResponse
import com.carled.app.data.remote.dto.FirmwareCheck
import com.carled.app.data.remote.dto.FirmwareSession
import com.carled.app.data.repository.PendingFirmware
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.junit.Assert.*
import org.junit.Test

class FirmwareUpdateTest {
    private val pending = PendingFirmware(12, "1.2.0", "AA:BB:CC:DD:EE:FF", "AABBCCDDEEFF", "r1")
    private val healthy = TelemetryEvent.OtaInfo("r2", "AABBCCDDEEFF", "1.2.0", "a".repeat(32), true, false, true)

    @Test fun onlyReportsHealthyTargetOnTheSameDevice() {
        assertTrue(pending.isConfirmedBy(healthy))
        assertFalse(pending.isConfirmedBy(healthy.copy(healthy = false)))
        assertFalse(pending.isConfirmedBy(healthy.copy(busy = true)))
        assertFalse(pending.isConfirmedBy(healthy.copy(version = "1.1.0")))
        assertFalse(pending.isConfirmedBy(healthy.copy(deviceId = "001122334455")))
    }

    @Test fun rollbackCanBeConfirmedWithoutComparingVersionOrder() {
        val rollback = pending.copy(version = "1.1.0")
        assertTrue(rollback.isConfirmedBy(healthy.copy(version = "1.1.0")))
    }

    @Test fun readsWrappedBackendContractAndRollbackFlag() {
        val json = """{"success":true,"data":{"deviceId":45,"currentVersion":"1.2.0","currentVersionStatus":"DEPRECATED","updateAvailable":true,"isRollback":true,"targetFirmware":{"firmwareId":11,"version":"1.1.0","changelog":"Recovery","fileSizeBytes":1048576,"sha256":"abc"}}}"""
        val type = object : TypeToken<ApiResponse<FirmwareCheck>>() {}.type
        val result = Gson().fromJson<ApiResponse<FirmwareCheck>>(json, type).data
        assertTrue(result.isRollback)
        assertEquals(11L, result.targetFirmware!!.firmwareId)
        assertEquals("1.1.0", result.targetFirmware.version)
    }

    @Test fun sessionDebugOutputDoesNotContainCapability() {
        val token = "a".repeat(64)
        val session = Gson().fromJson(
            """{"sessionId":31,"token":"$token","expiresAt":"2026-11-24T10:10:00Z","firmware":{"firmwareId":12,"version":"1.2.0","fileSizeBytes":1048576,"sha256":"abc"}}""",
            FirmwareSession::class.java
        )
        assertEquals(12L, session.firmware.firmwareId)
        assertFalse(session.toString().contains(token))
    }

    @Test fun reassemblesEveryByteOfLargeOtaInfoAndPreservesUtf8() {
        val json = """{"event":"ota_info","request_id":"r1","device_id":"AABBCCDDEEFF","version":"1.2.0","nonce":"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa","wifi":true,"configured":true,"busy":false,"healthy":true,"hardware":"ESP32","name":"Đèn xe có dấu { }","nested":{"value":1}}"""
        val assembler = TelemetryJsonAssembler()
        val messages = json.toByteArray().flatMap { assembler.accept(byteArrayOf(it), 1) }
        assertEquals(listOf(json), messages)
    }

    @Test fun acceptsConsecutiveObjectsWithEscapedQuotes() {
        val first = """{"name":"quote \" { remains inside string"}"""
        val second = """{"event":"ota_status","state":"rebooting","progress":100}"""
        assertEquals(listOf(first, second), TelemetryJsonAssembler().accept((first + second).toByteArray(), 1))
    }

    @Test fun discardsPartialJsonAfterDisconnectOrTimeout() {
        val assembler = TelemetryJsonAssembler()
        assembler.accept("""{"event":""".toByteArray(), 1)
        val complete = """{"event":"ota_info"}"""
        assertEquals(listOf(complete), assembler.accept(complete.toByteArray(), 3_001))
        assembler.accept("""{"event":""".toByteArray(), 3_002)
        assembler.reset()
        assertEquals(listOf(complete), assembler.accept(complete.toByteArray(), 3_003))
    }
}
