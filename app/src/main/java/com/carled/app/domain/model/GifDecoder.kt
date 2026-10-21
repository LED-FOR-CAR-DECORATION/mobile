package com.carled.app.domain.model

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Movie
import java.io.InputStream
import kotlin.math.max
import kotlin.math.min

/**
 * Kết quả trích xuất frame từ file GIF hoặc ảnh.
 */
data class GifFrameInfo(
    val frames: List<Bitmap>,
    val frameDelayMs: Int = 100,
    val isAnimated: Boolean = false
) {
    val first: Bitmap get() = frames.first()
}

/**
 * Trích xuất frames và thời gian delay từ file GIF.
 * Tối ưu hóa bộ nhớ bằng cách downscale frame về kích thước phù hợp ma trận LED (tối đa 128px),
 * tránh tràn RAM (OOM) khi người dùng chọn GIF kích thước lớn.
 */
object GifDecoder {

    private const val MAX_DIMENSION = 128
    private const val MAX_FRAMES = 60
    private const val SCAN_STEP_MS = 10

    @Suppress("DEPRECATION")
    fun decode(inputStream: InputStream): GifFrameInfo? {
        val movie = try {
            Movie.decodeStream(inputStream)
        } catch (_: Exception) {
            null
        } ?: return null

        val duration = movie.duration()
        if (duration <= 0) return null

        val rawW = movie.width()
        val rawH = movie.height()
        if (rawW <= 0 || rawH <= 0) return null

        // Tối ưu kích thước bitmap đích cho LED (tối đa 128px)
        val scale = min(1f, MAX_DIMENSION.toFloat() / max(rawW, rawH))
        val targetW = max(1, (rawW * scale).toInt())
        val targetH = max(1, (rawH * scale).toInt())

        // Quét timeline để phát hiện các thời điểm frame thay đổi
        val frameTimes = mutableListOf<Int>()
        val tempBitmap = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
        val tempCanvas = Canvas(tempBitmap)
        if (scale < 1f) {
            tempCanvas.scale(scale, scale)
        }

        var lastHash = 0L
        for (t in 0 until duration step SCAN_STEP_MS) {
            tempBitmap.eraseColor(0)
            movie.setTime(t)
            movie.draw(tempCanvas, 0f, 0f)

            // Lấy mẫu 9 điểm trên ảnh để kiểm tra thay đổi frame
            var hash = 0L
            val xs = intArrayOf(0, targetW / 2, targetW - 1)
            val ys = intArrayOf(0, targetH / 2, targetH - 1)
            for (y in ys) {
                for (x in xs) {
                    hash = 31 * hash + tempBitmap.getPixel(x, y)
                }
            }

            if (hash != lastHash || frameTimes.isEmpty()) {
                frameTimes.add(t)
                lastHash = hash
            }
        }
        tempBitmap.recycle()

        // Nếu chỉ có 1 frame duy nhất thì đây là GIF tĩnh
        if (frameTimes.size <= 1) return null

        // Tính delay trung bình giữa các frame (ms)
        val frameDelay = (duration / frameTimes.size).coerceIn(20, 1000)

        // Render các frame thực tế
        val frames = mutableListOf<Bitmap>()
        val step = if (frameTimes.size > MAX_FRAMES) frameTimes.size / MAX_FRAMES else 1

        for (i in frameTimes.indices step step) {
            if (frames.size >= MAX_FRAMES) break
            val frameBitmap = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
            val frameCanvas = Canvas(frameBitmap)
            if (scale < 1f) {
                frameCanvas.scale(scale, scale)
            }
            movie.setTime(frameTimes[i])
            movie.draw(frameCanvas, 0f, 0f)
            frames.add(frameBitmap)
        }

        return if (frames.size > 1) {
            GifFrameInfo(frames, frameDelay, isAnimated = true)
        } else {
            null
        }
    }
}
