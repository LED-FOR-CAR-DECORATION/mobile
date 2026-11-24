package com.carled.app.presentation.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.SubcomposeAsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.carled.app.core.di.ServiceLocator
import com.carled.app.presentation.theme.Cream
import com.carled.app.presentation.theme.Ink

@Composable
fun RemoteThumbnail(url: String?, label: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val gifImageLoader = remember(context) {
        ImageLoader.Builder(context)
            .okHttpClient { ServiceLocator.plainHttpClient }
            .components {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
    }

    Box(
        modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Cream),
        contentAlignment = Alignment.Center
    ) {
        if (url.isNullOrBlank()) {
            Icon(
                imageVector = Icons.Default.BrokenImage,
                contentDescription = "$label (không tải được)",
                tint = Ink
            )
        } else {
            val request = remember(context, url) {
                ImageRequest.Builder(context)
                    .data(url)
                    .crossfade(true)
                    .build()
            }
            SubcomposeAsyncImage(
                model = request,
                imageLoader = gifImageLoader,
                contentDescription = label,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Ink, modifier = Modifier.size(20.dp))
                    }
                },
                error = {
                    Icon(
                        imageVector = Icons.Default.BrokenImage,
                        contentDescription = "$label (không tải được)",
                        tint = Ink
                    )
                }
            )
        }
    }
}