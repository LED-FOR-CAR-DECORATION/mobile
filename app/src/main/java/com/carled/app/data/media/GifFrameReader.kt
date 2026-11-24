package com.carled.app.data.media

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Movie
import com.carled.app.domain.model.GifTimeline

/** Software canvas supports GIF compositing on Android 8+, including disposal. */
@Suppress("DEPRECATION")
object GifFrameReader {
    fun read(bytes: ByteArray): Pair<List<Bitmap>, Int> {
        val timeline = GifTimeline.parse(bytes)
        val movie = requireNotNull(Movie.decodeByteArray(bytes, 0, bytes.size)) { "Không giải mã được GIF" }
        val (times, delay) = timeline.sampling()
        val scale = minOf(1f, 512f / maxOf(timeline.width, timeline.height))
        val width = (timeline.width * scale).toInt().coerceAtLeast(1)
        val height = (timeline.height * scale).toInt().coerceAtLeast(1)
        val frames = times.map { time ->
            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { bitmap ->
                val canvas = Canvas(bitmap)
                canvas.scale(width.toFloat() / timeline.width, height.toFloat() / timeline.height)
                // Movie treats an exact frame-end timestamp as the preceding
                // frame. Sample just inside the next interval, not its boundary.
                movie.setTime((time + 1).coerceAtMost((movie.duration() - 1).coerceAtLeast(0)))
                movie.draw(canvas, 0f, 0f)
            }
        }
        return frames to delay
    }
}
