package com.carled.app.data.media

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Movie
import com.carled.app.domain.model.GifTimeline

@Suppress("DEPRECATION")
object GifFrameDecoder {

    private const val MAX_FRAMES = 255

    data class DecodedFrame(val pixels: IntArray, val width: Int, val height: Int)

    fun decodeFrames(bytes: ByteArray, frameDelayMs: Int): List<DecodedFrame> {
        val movie = Movie.decodeByteArray(bytes, 0, bytes.size)
            ?: error("Không đọc được định dạng GIF.")

        val useTimeline = GifTimeline.isGif(bytes)
        val timeline = if (useTimeline) runCatching { GifTimeline.parse(bytes) }.getOrNull() else null

        val width = (timeline?.width ?: movie.width()).coerceAtLeast(1)
        val height = (timeline?.height ?: movie.height()).coerceAtLeast(1)

        val times: List<Int> = if (timeline != null) {
            val (sampledTimes, _) = timeline.sampling()
            sampledTimes
        } else {
            val duration = movie.duration()
            val step = frameDelayMs.coerceAtLeast(1)
            if (duration <= 0) listOf(0) else (0 until duration step step).toList()
        }

        val cappedTimes = if (times.size <= MAX_FRAMES) {
            times
        } else {
            val strideFrames = (times.size + MAX_FRAMES - 1) / MAX_FRAMES
            times.filterIndexed { index, _ -> index % strideFrames == 0 }.take(MAX_FRAMES)
        }

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val scaleX = width.toFloat() / movie.width().coerceAtLeast(1)
        val scaleY = height.toFloat() / movie.height().coerceAtLeast(1)
        val pixelBuffer = IntArray(width * height)

        return cappedTimes.map { time ->
            val seekTime = if (useTimeline) {
                (time + 1).coerceAtMost((movie.duration() - 1).coerceAtLeast(0))
            } else {
                time
            }
            movie.setTime(seekTime)
            bitmap.eraseColor(0)
            canvas.save()
            canvas.scale(scaleX, scaleY)
            movie.draw(canvas, 0f, 0f)
            canvas.restore()
            bitmap.getPixels(pixelBuffer, 0, width, 0, 0, width, height)
            DecodedFrame(pixelBuffer.copyOf(), width, height)
        }
    }
}