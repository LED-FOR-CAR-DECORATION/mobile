package com.carled.app.presentation.screens.commission

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.carled.app.core.di.ServiceLocator
import com.carled.app.presentation.components.MirageButton
import com.carled.app.presentation.components.WindowFrame
import com.carled.app.presentation.theme.Cream
import com.carled.app.presentation.theme.Ink
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.IOException

private sealed class RemotePreviewState {
    object Loading : RemotePreviewState()
    data class Loaded(val bitmap: ImageBitmap) : RemotePreviewState()
    object Failed : RemotePreviewState()
    object Expired : RemotePreviewState()
}

private sealed class EnlargeSource {
    data class Remote(val bitmap: ImageBitmap) : EnlargeSource()
    data class Local(val uri: Uri) : EnlargeSource()
}

@Composable
internal fun LocalImagePreview(uri: Uri, label: String, modifier: Modifier) {
    val resolver = LocalContext.current.contentResolver
    val bitmap by produceState<ImageBitmap?>(null, uri) {
        value = withContext(Dispatchers.IO) { decodeDownsampled(resolver, uri, maxDimension = 512) }
    }
    val preview = bitmap
    var enlarged by remember { mutableStateOf(false) }

    if (preview == null) {
        Box(modifier, contentAlignment = Alignment.Center) {
            Icon(Icons.Default.AddPhotoAlternate, label, tint = Ink)
        }
    } else {
        Image(
            preview,
            label,
            modifier
                .clip(CardShape)
                .background(Cream)
                .clickable(onClickLabel = "Xem ảnh lớn") { enlarged = true },
            contentScale = ContentScale.Fit
        )
    }

    if (enlarged) {
        EnlargedImageDialog(EnlargeSource.Local(uri), label) { enlarged = false }
    }
}

@Composable
internal fun RemoteImagePreview(
    url: String?,
    label: String,
    modifier: Modifier,
    onExpired: (() -> Unit)? = null
) {
    val currentOnExpired by rememberUpdatedState(onExpired)
    var enlarged by remember { mutableStateOf(false) }
    val state by produceState<RemotePreviewState>(RemotePreviewState.Loading, url) {
        value = withContext(Dispatchers.IO) {
            if (url.isNullOrBlank()) {
                Log.w("RemoteImagePreview", "Skipped fetch: null/blank url for $label")
                return@withContext RemotePreviewState.Failed
            }
            try {
                val request = Request.Builder().url(url).build()
                ServiceLocator.plainHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.e("RemoteImagePreview", "Fetch failed: HTTP ${response.code} for $url")
                        if (response.code == 401 || response.code == 403) {
                            currentOnExpired?.invoke()
                            return@use RemotePreviewState.Expired
                        }
                        return@use RemotePreviewState.Failed
                    }
                    val bytes = response.body?.bytes()
                    val bitmap = bytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
                    if (bitmap != null) {
                        RemotePreviewState.Loaded(bitmap.asImageBitmap())
                    } else {
                        Log.e("RemoteImagePreview", "Decode returned null for $url")
                        RemotePreviewState.Failed
                    }
                }
            } catch (e: Exception) {
                Log.e("RemoteImagePreview", "Fetch threw for $url", e)
                RemotePreviewState.Failed
            }
        }
    }

    when (val s = state) {
        is RemotePreviewState.Loading, is RemotePreviewState.Expired -> Box(modifier, contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Ink, modifier = Modifier.size(20.dp))
        }
        is RemotePreviewState.Failed -> Box(modifier, contentAlignment = Alignment.Center) {
            Icon(Icons.Default.AddPhotoAlternate, "$label (không tải được)", tint = Ink)
        }
        is RemotePreviewState.Loaded -> Image(
            s.bitmap,
            label,
            modifier
                .clip(CardShape)
                .background(Cream)
                .clickable(onClickLabel = "Xem ảnh lớn") { enlarged = true },
            contentScale = ContentScale.Fit
        )
    }

    if (enlarged) {
        val loadedBitmap = (state as? RemotePreviewState.Loaded)?.bitmap
        if (loadedBitmap != null) {
            EnlargedImageDialog(EnlargeSource.Remote(loadedBitmap), label) { enlarged = false }
        }
    }
}

@Composable
private fun EnlargedImageDialog(source: EnlargeSource, label: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val resolver = context.contentResolver
    val scope = rememberCoroutineScope()
    var saving by remember { mutableStateOf(false) }

    val bitmap by produceState<ImageBitmap?>(
        initialValue = (source as? EnlargeSource.Remote)?.bitmap,
        key1 = source
    ) {
        if (source is EnlargeSource.Local) {
            value = withContext(Dispatchers.IO) { decodeDownsampled(resolver, source.uri, maxDimension = 2048) }
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        WindowFrame(
            label,
            Modifier.fillMaxWidth().fillMaxHeight(0.85f),
            onClose = onDismiss,
            compact = true,
            fillContent = true,
            footer = {
                val current = bitmap
                MirageButton(
                    if (saving) "Đang lưu…" else "Tải ảnh xuống",
                    {
                        if (!saving && current != null) {
                            saving = true
                            scope.launch(Dispatchers.IO) {
                                val ok = saveImageToGallery(context, current.asAndroidBitmap(), label)
                                withContext(Dispatchers.Main) {
                                    saving = false
                                    Toast.makeText(
                                        context,
                                        if (ok) "Đã lưu ảnh vào thư viện" else "Không lưu được ảnh. Thử lại sau.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    },
                    Modifier.fillMaxWidth()
                )
            }
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                val current = bitmap
                if (current == null) {
                    CircularProgressIndicator(color = Ink)
                } else {
                    val trueRatio = remember(current) { current.width.toFloat() / current.height.toFloat() }
                    Image(
                        current,
                        label,
                        Modifier.fillMaxWidth().aspectRatio(trueRatio).clip(CardShape),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }
    }
}

private fun decodeDownsampled(resolver: ContentResolver, uri: Uri, maxDimension: Int): ImageBitmap? = try {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    val options = BitmapFactory.Options()
    while (maxOf(bounds.outWidth, bounds.outHeight) / options.inSampleSize.coerceAtLeast(1) > maxDimension) {
        options.inSampleSize = options.inSampleSize.coerceAtLeast(1) * 2
    }
    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options)?.asImageBitmap() }
} catch (_: IOException) {
    null
} catch (_: SecurityException) {
    null
}

private fun saveImageToGallery(context: Context, bitmap: Bitmap, label: String): Boolean {
    val safeName = label.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_').ifBlank { "image" }
    val displayName = "carled_${safeName}_${System.currentTimeMillis()}.jpg"
    return try {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/CarLed")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false
        val wrote = resolver.openOutputStream(uri)?.use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
        } ?: false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        }
        wrote
    } catch (e: Exception) {
        Log.e("ImageDownload", "Save failed for $displayName", e)
        false
    }
}