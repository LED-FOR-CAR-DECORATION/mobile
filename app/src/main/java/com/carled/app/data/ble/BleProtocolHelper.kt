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

    // Flow 2 — Pixel Art / Animation Upload
    const val CMD_PROJ_START: Byte = 0x10
    const val CMD_PROJ_CHUNK: Byte = 0x11
    const val CMD_PROJ_FINISH: Byte = 0x12

    // Canvas Drawing
    const val CMD_DRAW_PIXEL: Byte = 0x13
    const val MAX_DRAW_POINTS = 12
    const val CMD_DRAW_CONFIRMED: Byte = 0x1A
    const val CMD_DRAW_BATCH: Byte = 0x14
    const val CMD_CANVAS_CLEAR: Byte = 0x15

    /**
     * Khung chuẩn 7 bytes: [0xAA, cmd, p1, p2, p3, checksum, 0x55]
     * checksum = cmd XOR p1 XOR p2 XOR p3
     */
    fun buildFrame(cmd: Byte, p1: Int = 0, p2: Int = 0, p3: Int = 0): ByteArray {
        val b1 = (p1 and 0xFF).toByte()
        val b2 = (p2 and 0xFF).toByte()
        val b3 = (p3 and 0xFF).toByte()
        val checksum = (cmd.toInt() xor b1.toInt() xor b2.toInt() xor b3.toInt()).toByte()
        return byteArrayOf(HEADER, cmd, b1, b2, b3, checksum, TAIL)
    }

    fun buildPowerFrame(on: Boolean) = buildFrame(CMD_POWER, if (on) 1 else 0)
    fun buildBrightnessFrame(level: Int) = buildFrame(CMD_BRIGHTNESS, level.coerceIn(0, 255))
    fun buildSpeedFrame(speed: Int) = buildFrame(CMD_SPEED, speed.coerceIn(1, 100))
    fun buildModeFrame(modeIndex: Int) = buildFrame(CMD_MODE, modeIndex)
    fun buildRgbColorFrame(r: Int, g: Int, b: Int) = buildFrame(CMD_COLOR_RGB, r, g, b)
    fun buildSensitivityFrame(sens: Int) = buildFrame(CMD_SENSITIVITY, sens.coerceIn(1, 100))
    fun buildCanvasClearFrame() = buildFrame(CMD_CANVAS_CLEAR, 0, 0, 0)

    /**
     * Draw Live Pixel — Frame 9 bytes:
     * [0xAA, 0x13, x, y, r, g, b, checksum, 0x55]
     * checksum = 0x13 XOR x XOR y XOR r XOR g XOR b
     */
    fun buildDrawPixelFrame(x: Int, y: Int, r: Int, g: Int, b: Int): ByteArray {
        val bx = (x and 0xFF).toByte()
        val by_ = (y and 0xFF).toByte()
        val br = (r and 0xFF).toByte()
        val bg = (g and 0xFF).toByte()
        val bb = (b and 0xFF).toByte()
        val checksum = (CMD_DRAW_PIXEL.toInt()
                xor bx.toInt() xor by_.toInt()
                xor br.toInt() xor bg.toInt() xor bb.toInt()).toByte()
        return byteArrayOf(HEADER, CMD_DRAW_PIXEL, bx, by_, br, bg, bb, checksum, TAIL)
    }

    /**
     * Draw Batch — Gom nhiều điểm chạm vào một gói (chống nghẽn BLE):
     * [0xAA, 0x14, count, x0,y0,r0,g0,b0, ..., xN,yN,rN,gN,bN, checksum, 0x55]
     */
    fun buildBatchDrawFrame(points: List<TouchPoint>): ByteArray {
        require(points.size in 1..MAX_DRAW_POINTS) { "Batch must contain1..12 pixels; never truncate a stroke" }
        val count = points.size
        val payload = ByteArray(3 + count * 5 + 2)
        payload[0] = HEADER
        payload[1] = CMD_DRAW_BATCH
        payload[2] = count.toByte()

        var csum = (CMD_DRAW_BATCH.toInt() and 0xFF) xor (count and 0xFF)
        for (i in 0 until count) {
            val pt = points[i]
            val offset = 3 + i * 5
            payload[offset]     = (pt.x and 0xFF).toByte()
            payload[offset + 1] = (pt.y and 0xFF).toByte()
            payload[offset + 2] = (pt.r and 0xFF).toByte()
            payload[offset + 3] = (pt.g and 0xFF).toByte()
            payload[offset + 4] = (pt.b and 0xFF).toByte()
            for (k in 0..4) {
                csum = csum xor (payload[offset + k].toInt() and 0xFF)
            }
        }
        payload[3 + count * 5]     = (csum and 0xFF).toByte()
        payload[3 + count * 5 + 1] = TAIL
        return payload
    }

    fun buildConfirmedBatchFrame(points: List<TouchPoint>, sequence: Int): ByteArray {
        require(sequence in 0..65535)
        require(points.all { it.x in 0..63 && it.y in 0..31 && listOf(it.r,it.g,it.b).all { c -> c in 0..255 } })
        val legacy = buildBatchDrawFrame(points)
        val frame = ByteArray(legacy.size + 2)
        frame[0] = HEADER; frame[1] = CMD_DRAW_CONFIRMED
        frame[2] = (sequence ushr 8).toByte(); frame[3] = sequence.toByte()
        legacy.copyInto(frame,4,2,legacy.size-2)
        var checksum = 0
        for (i in 1 until frame.size-2) checksum = checksum xor (frame[i].toInt() and 255)
        frame[frame.size-2] = checksum.toByte(); frame[frame.size-1] = TAIL
        return frame
    }

    /**
     * Projection Start — [0xAA, 0x10, frameCount, width, height, delayHigh, delayLow, 0x55]
     */
    fun buildProjectionStartFrame(
        frameCount: Int,
        width: Int = 64,
        height: Int = 32,
        frameDelayMs: Int = 200
    ): ByteArray {
        return byteArrayOf(
            HEADER, CMD_PROJ_START,
            frameCount.toByte(),
            width.toByte(),
            height.toByte(),
            ((frameDelayMs shr 8) and 0xFF).toByte(),
            (frameDelayMs and 0xFF).toByte(),
            TAIL
        )
    }

    /**
     * Projection Chunk — [0xAA, 0x11, chunkIdxH, chunkIdxL, dataLen, <data...>, 0x55]
     */
    fun buildProjectionChunkFrame(chunkIndex: Int, data: ByteArray): ByteArray {
        val dataLen = data.size.coerceAtMost(240)
        val frame = ByteArray(5 + dataLen + 1)
        frame[0] = HEADER
        frame[1] = CMD_PROJ_CHUNK
        frame[2] = ((chunkIndex shr 8) and 0xFF).toByte()
        frame[3] = (chunkIndex and 0xFF).toByte()
        frame[4] = dataLen.toByte()
        data.copyInto(frame, 5, 0, dataLen)
        frame[5 + dataLen] = TAIL
        return frame
    }

    /**
     * Projection Finish — [0xAA, 0x12, activateNow, 0x00, 0x00, checksum, 0x55]
     */
    fun buildProjectionFinishFrame(activateNow: Boolean = true): ByteArray {
        return buildFrame(CMD_PROJ_FINISH, if (activateNow) 1 else 0)
    }

    // ──────────────── JSON Command Builders ────────────────

    fun buildMarqueeJson(
        text: String,
        r: Int = 255, g: Int = 215, b: Int = 0,
        speed: Int = 60,
        size: Int = 1,
        yPos: Int = -1,
        rainbow: Boolean = false,
        scrollMode: String = "scroll_left"
    ): String = """{"type":"marquee","text":"$text","color":{"r":$r,"g":$g,"b":$b},"speed":$speed,"size":$size,"y_pos":$yPos,"mode":"$scrollMode","rainbow":$rainbow}"""

    fun buildWifiConfigJson(ssid: String, password: String): String =
        """{"command":"wifi_config","ssid":"$ssid","password":"$password"}"""

    fun buildCanvasClearJson(): String = """{"command":"canvas_clear"}"""

    fun buildReloadProjectionJson(): String = """{"command":"reload_projection"}"""
}
