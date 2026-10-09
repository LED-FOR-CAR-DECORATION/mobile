package com.carled.app.data.ble

data class TouchPoint(val x: Int, val y: Int, val r: Int, val g: Int, val b: Int)

/**
 * Bộ tạo gói tin nhị phân giao tiếp với ESP32 Car LED.
 * Khung chuẩn 7 bytes có XOR Checksum: [0xAA, cmd, p1, p2, p3, checksum, 0x55]
 */
object BleProtocolHelper {
    private const val HEADER: Byte = 0xAA.toByte()
    private const val TAIL: Byte = 0x55.toByte()

    const val CMD_POWER: Byte = 0x01
    const val CMD_BRIGHTNESS: Byte = 0x02
    const val CMD_SPEED: Byte = 0x03
    const val CMD_MODE: Byte = 0x04
    const val CMD_COLOR_RGB: Byte = 0x05
    const val CMD_SENSITIVITY: Byte = 0x06
    const val CMD_DRAW_PIXEL: Byte = 0x13
    const val CMD_DRAW_BATCH: Byte = 0x14

    fun buildFrame(cmd: Byte, p1: Int = 0, p2: Int = 0, p3: Int = 0): ByteArray {
        val b1 = (p1 and 0xFF).toByte()
        val b2 = (p2 and 0xFF).toByte()
        val b3 = (p3 and 0xFF).toByte()
        val checksum = (cmd.toInt() xor b1.toInt() xor b2.toInt() xor b3.toInt()).toByte()
        return byteArrayOf(
            HEADER,
            cmd,
            b1,
            b2,
            b3,
            checksum,
            TAIL
        )
    }

    fun buildPowerFrame(on: Boolean) = buildFrame(CMD_POWER, if (on) 1 else 0)
    fun buildBrightnessFrame(level: Int) = buildFrame(CMD_BRIGHTNESS, level.coerceIn(0, 255))
    fun buildSpeedFrame(speed: Int) = buildFrame(CMD_SPEED, speed.coerceIn(1, 100))
    fun buildModeFrame(modeIndex: Int) = buildFrame(CMD_MODE, modeIndex)
    fun buildRgbColorFrame(r: Int, g: Int, b: Int) = buildFrame(CMD_COLOR_RGB, r, g, b)
    fun buildSensitivityFrame(sens: Int) = buildFrame(CMD_SENSITIVITY, sens.coerceIn(1, 100))

    fun buildBatchDrawFrame(points: List<TouchPoint>): ByteArray {
        val count = points.size.coerceAtMost(15)
        val payload = ByteArray(3 + count * 5 + 2)
        payload[0] = HEADER
        payload[1] = CMD_DRAW_BATCH
        payload[2] = count.toByte()

        var csum = (CMD_DRAW_BATCH.toInt() xor count).toByte()
        for (i in 0 until count) {
            val pt = points[i]
            val offset = 3 + i * 5
            payload[offset] = (pt.x and 0xFF).toByte()
            payload[offset + 1] = (pt.y and 0xFF).toByte()
            payload[offset + 2] = (pt.r and 0xFF).toByte()
            payload[offset + 3] = (pt.g and 0xFF).toByte()
            payload[offset + 4] = (pt.b and 0xFF).toByte()
            for (k in 0..4) {
                csum = (csum.toInt() xor payload[offset + k].toInt()).toByte()
            }
        }
        payload[3 + count * 5] = csum
        payload[3 + count * 5 + 1] = TAIL
        return payload
    }
}
