package com.carled.app.domain.model

/** Reads GIF block structure, never mistakes bytes inside compressed data for frames. */
data class GifTimeline(val width: Int, val height: Int, val delays: List<Int>) {
    val durationMs: Int get() = delays.sum()
    fun sampling(): Pair<List<Int>, Int> {
        if (delays.size == 1) return listOf(0) to 200
        fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)
        val tick = delays.reduce(::gcd).coerceAtLeast(20)
        val count = ((durationMs + tick - 1) / tick).coerceIn(2, 16)
        val delay = (durationMs.toDouble() / count).toInt().coerceAtLeast(20)
        return List(count) { it * delay } to delay
    }
    companion object {
        fun isGif(bytes: ByteArray) = bytes.size >= 6 &&
            (String(bytes, 0, 6, Charsets.US_ASCII) == "GIF89a" || String(bytes, 0, 6, Charsets.US_ASCII) == "GIF87a")
        fun parse(bytes: ByteArray): GifTimeline {
            require(isGif(bytes)) { "File không phải GIF hợp lệ" }
            var position = 6
            fun byte(): Int { require(position < bytes.size) { "GIF bị thiếu dữ liệu" }; return bytes[position++].toInt() and 255 }
            fun word() = byte() or (byte() shl 8)
            fun skip(count: Int) { require(count >= 0 && count <= bytes.size - position) { "GIF bị thiếu dữ liệu" }; position += count }
            fun blocks() { while (true) { val length = byte(); if (length == 0) return; skip(length) } }
            val width = word(); val height = word()
            require(width in 1..2048 && height in 1..2048 && width.toLong() * height <= 4_194_304) { "GIF quá lớn (tối đa 2048×2048)" }
            val packed = byte(); skip(2)
            if (packed and 128 != 0) skip(3 * (1 shl ((packed and 7) + 1)))
            val delays = mutableListOf<Int>(); var delay = 100
            while (true) when (byte()) {
                0x21 -> if (byte() == 0xf9) {
                    require(byte() == 4) { "GIF không hợp lệ" }; byte()
                    delay = (word() * 10).let { if (it <= 10) 100 else it }
                    byte(); require(byte() == 0)
                } else blocks()
                0x2c -> {
                    skip(8); val local = byte()
                    if (local and 128 != 0) skip(3 * (1 shl ((local and 7) + 1)))
                    byte(); blocks(); delays += delay; delay = 100
                    require(delays.size <= 500 && delays.sum() <= 30_000) { "GIF quá dài (tối đa 30 giây / 500 khung hình)" }
                }
                0x3b -> { require(delays.isNotEmpty()) { "GIF không có khung hình" }; return GifTimeline(width, height, delays) }
                else -> throw IllegalArgumentException("Cấu trúc GIF không hợp lệ")
            }
        }
    }
}
