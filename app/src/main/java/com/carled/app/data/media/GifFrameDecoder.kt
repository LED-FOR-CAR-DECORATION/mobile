package com.carled.app.data.media

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Movie

/**
 * Decodes an animated GIF's frames into ARGB pixel arrays using Android's
 * built-in [Movie] API — no extra Gradle dependency needed.
 *
 * [Movie] doesn't expose per-frame durations, only a total [Movie.duration].
 * That's fine here: the LED projection protocol only has a single
 * frameDelayMs for the whole animation anyway (see BleProtocolHelper's
 * Projection Start frame), so frames are sampled at that same fixed step
 * across the GIF's timeline rather than reproducing its exact per-frame
 * timing.
 *
 * [Movie] has been deprecated since API 29, but it is not removed on any
 * shipping Android version and remains the only public API that supports
 * seeking a decoded GIF to an arbitrary timestamp and rendering that frame —
 * its replacement, [android.graphics.ImageDecoder]'s AnimatedImageDrawable,
 * only exposes a self-driven play/pause/callback model with no equivalent
 * seek-to-time method, so it can't reproduce this fixed-step sampling
 * without a much larger rewrite (driving its own Choreographer-style loop
 * and capturing whatever frame happens to be showing). Suppressed below
 * rather than worked around for that reason; revisit if Movie is ever
 * actually removed.
 */
@Suppress("DEPRECATION")
object GifFrameDecoder {

    /** BLE firmware's animation frame-count ceiling (see ConfirmedCanvasSender / uploadAnimation's require()). */
    private const val MAX_FRAMES = 255

    data class DecodedFrame(val pixels: IntArray, val width: Int, val height: Int)

    /**
     * @param frameDelayMs sampling step in ms across the GIF's duration.
     * A static (non-animated) GIF returns a single frame regardless of this value.
     */
    fun decodeFrames(bytes: ByteArray, frameDelayMs: Int): List<DecodedFrame> {
        val movie = Movie.decodeByteArray(bytes, 0, bytes.size)
            ?: error("Không đọc được định dạng GIF.")

        val width = movie.width().coerceAtLeast(1)
        val height = movie.height().coerceAtLeast(1)
        val duration = movie.duration()
        val step = frameDelayMs.coerceAtLeast(1)

        val rawTimes = if (duration <= 0) listOf(0) else (0 until duration step step).toList()
        val times = if (rawTimes.size <= MAX_FRAMES) {
            rawTimes
        } else {
            // Evenly thin the sample points down to the firmware's limit rather
            // than just truncating the tail of the animation.
            val strideFrames = (rawTimes.size + MAX_FRAMES - 1) / MAX_FRAMES
            rawTimes.filterIndexed { index, _ -> index % strideFrames == 0 }.take(MAX_FRAMES)
        }

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val pixelBuffer = IntArray(width * height)

        return times.map { time ->
            movie.setTime(time)
            bitmap.eraseColor(0)
            movie.draw(canvas, 0f, 0f)
            bitmap.getPixels(pixelBuffer, 0, width, 0, 0, width, height)
            DecodedFrame(pixelBuffer.copyOf(), width, height)
        }
    }
}