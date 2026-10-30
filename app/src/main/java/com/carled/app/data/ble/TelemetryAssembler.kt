package com.carled.app.data.ble

/** Frames complete JSON objects across BLE MTU fragments, preserving UTF-8. */
class TelemetryAssembler {
    private val bytes=java.io.ByteArrayOutputStream()
    private var depth=0
    private var quoted=false
    private var escaped=false
    fun clear() { bytes.reset(); depth=0; quoted=false; escaped=false }
    fun accept(fragment: ByteArray): List<String> {
        val messages=mutableListOf<String>()
        for(byte in fragment) {
            val value=byte.toInt() and 255
            if(bytes.size()==0 && value!='{'.code) continue
            bytes.write(value)
            if(bytes.size()>4096) { clear(); continue }
            if(quoted) {
                if(escaped) escaped=false else if(value=='\\'.code) escaped=true else if(value=='"'.code) quoted=false
            } else when(value) {
                '"'.code -> quoted=true
                '{'.code -> depth++
                '}'.code -> depth--
            }
            if(depth==0 && !quoted) { messages.add(String(bytes.toByteArray(),Charsets.UTF_8)); clear() }
        }
        return messages
    }
}
