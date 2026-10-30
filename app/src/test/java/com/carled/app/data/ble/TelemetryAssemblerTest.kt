package com.carled.app.data.ble

import org.junit.Assert.*
import org.junit.Test

class TelemetryAssemblerTest {
    @Test fun assemblesSmallMtuPacketsAndUtf8SplitAtEveryByte() {
        val json="""{"event":"ota_info","name":"Đèn xe","nested":{"value":"}\\\"{"}}"""
        val assembler=TelemetryAssembler()
        val messages=json.toByteArray(Charsets.UTF_8).flatMap { assembler.accept(byteArrayOf(it)) }
        assertEquals(listOf(json),messages)
    }
    @Test fun disconnectDiscardsPartialMessageAndHandlesMultipleMessages() {
        val assembler=TelemetryAssembler()
        assembler.accept("{\"event\":".toByteArray()); assembler.clear()
        assertEquals(listOf("{}","{\"event\":\"ota_status\"}"),assembler.accept("{}{\"event\":\"ota_status\"}".toByteArray()))
    }
}
