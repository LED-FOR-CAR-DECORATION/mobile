package com.carled.app.presentation.screens.canvas

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Environment
import android.provider.MediaStore
import com.carled.app.domain.model.PixelDocument
import java.io.OutputStream

enum class ExportFormat { PNG, JPEG }

object ImageExportHelper {
    fun exportDocument(
        context: Context,
        document: PixelDocument,
        filename: String,
        format: ExportFormat,
        scaleFactor: Int = 16 // Scales up tiny pixel art so it's sharp and visible
    ): Boolean {
        val width = document.width
        val height = document.height
        val pixels = document.composite()

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val argbPixels = IntArray(pixels.size) { i ->
            val p = pixels[i]
            if (p == 0) 0xFF18241C.toInt() else p // Background color for transparent pixels
        }
        bitmap.setPixels(argbPixels, 0, width, 0, 0, width, height)

        // Scale up bitmap for high-res export
        val scaledBitmap = if (scaleFactor > 1) {
            Bitmap.createScaledBitmap(bitmap, width * scaleFactor, height * scaleFactor, false)
        } else {
            bitmap
        }

        val name = filename.ifBlank { "pixel_art" }
        val mimeType = if (format == ExportFormat.PNG) "image/png" else "image/jpeg"
        val extension = if (format == ExportFormat.PNG) "png" else "jpg"
        val fullName = "$name.$extension"

        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fullName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/CarLED")
        }

        val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues) ?: return false

        return try {
            val stream: OutputStream = resolver.openOutputStream(imageUri) ?: return false
            stream.use {
                if (format == ExportFormat.PNG) {
                    scaledBitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                } else {
                    scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 90, it)
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}