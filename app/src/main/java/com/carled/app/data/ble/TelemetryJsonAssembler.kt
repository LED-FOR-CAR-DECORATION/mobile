package com.carled.app.data.ble

import java.io.ByteArrayOutputStream

/** Reassembles JSON objects split across BLE notifications, preserving UTF-8 bytes. */
class TelemetryJsonAssembler {
    private val buffer = ByteArrayOutputStream()
    private var depth = 0
    private var quoted = false
    private var escaped = false
    private var lastChunkAt = 0L

    @Synchronized
    fun accept(bytes: ByteArray, now: Long = System.nanoTime() / 1_000_000): List<String> {
        if (now - lastChunkAt > 2_000) reset()
        lastChunkAt = now
        val messages = mutableListOf<String>()
        for (byte in bytes) {
            val char = byte.toInt().toChar()
            if (depth == 0 && char != '{') continue
            buffer.write(byte.toInt())
            if (buffer.size() > 16_384) {
                reset()
                continue
            }
            if (quoted) {
                when {
                    escaped -> escaped = false
                    char == '\\' -> escaped = true
                    char == '"' -> quoted = false
                }
            } else {
                when (char) {
                    '"' -> quoted = true
                    '{' -> depth++
                    '}' -> depth--
                }
                if (depth == 0) {
                    messages += buffer.toString(Charsets.UTF_8.name())
                    reset()
                }
            }
        }
        return messages
    }

    @Synchronized
    fun reset() {
        buffer.reset()
        depth = 0
        quoted = false
        escaped = false
    }
}
