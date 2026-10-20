package com.carled.app.domain.model

import kotlin.math.*

enum class PhotoFit { STRETCH, COVER, CONTAIN }

object PhotoRasterizer {
    fun process(source: IntArray, sourceWidth: Int, sourceHeight: Int, fit: PhotoFit = PhotoFit.STRETCH,
        brightness: Int = 100, contrast: Int = 100, dither: Boolean = false): PixelDocument {
        require(sourceWidth > 0 && sourceHeight > 0 && source.size == sourceWidth * sourceHeight)
        val w = 64; val h = 32
        val scale = if (fit == PhotoFit.COVER) max(w.toFloat() / sourceWidth, h.toFloat() / sourceHeight)
            else min(w.toFloat() / sourceWidth, h.toFloat() / sourceHeight)
        val dw = if (fit == PhotoFit.STRETCH) w.toFloat() else sourceWidth * scale
        val dh = if (fit == PhotoFit.STRETCH) h.toFloat() else sourceHeight * scale
        val ox = (w - dw) / 2; val oy = (h - dh) / 2
        val rgb = Array(3) { FloatArray(w * h) }
        for (y in 0 until h) for (x in 0 until w) {
            val sx = floor((x + .5f - ox) * sourceWidth / dw).toInt()
            val sy = floor((y + .5f - oy) * sourceHeight / dh).toInt()
            if (sx !in 0 until sourceWidth || sy !in 0 until sourceHeight) continue
            val pixel = source[sy * sourceWidth + sx]; val alpha = (pixel ushr 24) / 255f
            if (alpha == 0f) continue
            for (c in 0..2) {
                val raw = ((pixel ushr ((2 - c) * 8)) and 255) * alpha
                rgb[c][y * w + x] = (((raw - 128) * contrast.coerceIn(40, 220) / 100f + 128) * brightness.coerceIn(40, 220) / 100f).coerceIn(0f, 255f)
            }
        }
        val output = IntArray(w * h)
        for (y in 0 until h) for (x in 0 until w) {
            val index = y * w + x
            var pixel = 0xff000000.toInt()
            for (c in 0..2) {
                val old = rgb[c][index].coerceIn(0f, 255f)
                val levels = if (c == 1) 63 else 31
                val quantized = if (dither) (old / 255 * levels).roundToInt() * (255f / levels) else old
                pixel = pixel or (quantized.roundToInt().coerceIn(0, 255) shl ((2 - c) * 8))
                if (dither) {
                    val error = old - quantized
                    fun spread(nx: Int, ny: Int, fraction: Float) {
                        if (nx in 0 until w && ny in 0 until h) rgb[c][ny * w + nx] += error * fraction
                    }
                    spread(x + 1, y, 7f / 16); spread(x - 1, y + 1, 3f / 16); spread(x, y + 1, 5f / 16); spread(x + 1, y + 1, 1f / 16)
                }
            }
            output[index] = pixel
        }
        return PixelDocument(w, h, output.toList())
    }
}
