package com.carled.app.presentation.screens.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carled.app.core.common.Resource
import com.carled.app.core.di.ServiceLocator
import com.carled.app.data.local.PixelDraft
import com.carled.app.data.media.GifFrameDecoder
import com.carled.app.domain.model.CustomerOwnedCharacterResponse
import com.carled.app.domain.model.PhotoFit
import com.carled.app.domain.model.PhotoRasterizer
import com.carled.app.domain.model.TelemetryEvent
import com.carled.app.domain.model.ble.ConnectionState
import com.carled.app.domain.model.scrollFrames
import com.carled.app.domain.repository.CharacterRepository
import com.carled.app.domain.usecase.ble.BleUseCases
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

/** One-shot status for a "Quick projection" send — shown inline in the Picture detail dialog. */
sealed class QuickProjectionStatus {
    object Idle : QuickProjectionStatus()
    object Connecting : QuickProjectionStatus()   // checking the LED is connected
    object Downloading : QuickProjectionStatus()  // fetching the GIF bytes
    object Decoding : QuickProjectionStatus()     // decoding frames / rasterizing to 64x32
    object Sending : QuickProjectionStatus()      // streaming frames to the LED over BLE
    object WaitingAck : QuickProjectionStatus()   // waiting for firmware confirmation
    object Success : QuickProjectionStatus()
    data class Error(val message: String) : QuickProjectionStatus()

    val isBusy: Boolean
        get() = this is Connecting || this is Downloading || this is Decoding || this is Sending || this is WaitingAck
}

internal class ContentLibraryViewModel(
    private val repository: CharacterRepository = ServiceLocator.characterRepository,
    private val ble: BleUseCases = ServiceLocator.bleUseCases,
    private val httpClient: OkHttpClient = ServiceLocator.plainHttpClient
) : ViewModel() {

    private val _ownedCharacters = MutableStateFlow<Resource<List<CustomerOwnedCharacterResponse>>>(Resource.Loading)
    val ownedCharacters: StateFlow<Resource<List<CustomerOwnedCharacterResponse>>> = _ownedCharacters.asStateFlow()

    private val _quickProjection = MutableStateFlow<QuickProjectionStatus>(QuickProjectionStatus.Idle)
    val quickProjection: StateFlow<QuickProjectionStatus> = _quickProjection.asStateFlow()

    private var quickProjectionJob: Job? = null

    init { loadOwnedCharacters() }

    fun loadOwnedCharacters() {
        viewModelScope.launch {
            _ownedCharacters.value = Resource.Loading
            _ownedCharacters.value = repository.getOwnedCharacters()
        }
    }

    /** Call when the detail dialog closes, so a stale status doesn't linger for the next open. */
    fun resetQuickProjection() {
        quickProjectionJob?.cancel()
        _quickProjection.value = QuickProjectionStatus.Idle
    }

    fun quickProjectCharacter(character: CustomerOwnedCharacterResponse) {
        startQuickProjection {
            _quickProjection.value = QuickProjectionStatus.Downloading
            val bytes = downloadBytes(character.previewGifUrl)

            _quickProjection.value = QuickProjectionStatus.Decoding
            val frameDelay = character.frameDelayMs.coerceIn(1, 65535)
            val frames = withContext(Dispatchers.Default) {
                GifFrameDecoder.decodeFrames(bytes, frameDelay)
                    .map { frame -> PhotoRasterizer.process(frame.pixels, frame.width, frame.height, PhotoFit.COVER) }
            }
            val combined = withContext(Dispatchers.Default) {
                frames.map { it.rgb565() }.reduce { acc, f -> acc + f }
            }

            sendToLed(
                rgb565Data = combined,
                width = 64,
                height = 32,
                frameCount = frames.size,
                frameDelayMs = frameDelay
            )
        }
    }

    fun quickProjectDraft(draft: PixelDraft) {
        startQuickProjection {
            _quickProjection.value = QuickProjectionStatus.Decoding
            val frames = withContext(Dispatchers.Default) { draft.document.scrollFrames() }
            val combined = withContext(Dispatchers.Default) {
                frames.map { it.rgb565() }.reduce { acc, f -> acc + f }
            }

            sendToLed(
                rgb565Data = combined,
                width = draft.document.width,
                height = draft.document.height,
                frameCount = frames.size,
                frameDelayMs = if (frames.size > 1) 80 else 200
            )
        }
    }

    private fun startQuickProjection(block: suspend () -> Unit) {
        if (_quickProjection.value.isBusy) return

        quickProjectionJob?.cancel()
        quickProjectionJob = viewModelScope.launch {
            _quickProjection.value = QuickProjectionStatus.Connecting
            if (ble.getConnectionState().value !is ConnectionState.Connected) {
                _quickProjection.value = QuickProjectionStatus.Error("Kết nối thiết bị LED trước khi chiếu.")
                return@launch
            }
            try {
                coroutineScope {
                    // Cancels the whole operation the moment the LED drops mid-flight,
                    // instead of letting a write fail deep inside bleManager and surface
                    // as an opaque exception — mirrors StudioViewModel's generation guard.
                    val watcher = launch {
                        ble.getConnectionState().first { it !is ConnectionState.Connected }
                        this@coroutineScope.cancel(CancellationException("LED bị ngắt kết nối"))
                    }
                    try {
                        block()
                    } finally {
                        watcher.cancel()
                    }
                }
            } catch (e: TimeoutCancellationException) {
                _quickProjection.value =
                    QuickProjectionStatus.Error("Chưa nhận xác nhận từ LED. Kiểm tra kết nối trước khi thử lại.")
            } catch (e: CancellationException) {
                if (ble.getConnectionState().value !is ConnectionState.Connected) {
                    _quickProjection.value =
                        QuickProjectionStatus.Error("Mất kết nối LED trong khi chiếu. Kết nối lại và thử lại.")
                } else {
                    throw e
                }
            } catch (e: Exception) {
                _quickProjection.value = QuickProjectionStatus.Error(e.message ?: "Không thể chiếu lên LED.")
            }
        }
    }

    private suspend fun sendToLed(rgb565Data: ByteArray, width: Int, height: Int, frameCount: Int, frameDelayMs: Int) {
        _quickProjection.value = QuickProjectionStatus.Sending
        coroutineScope {
            val ack = async(start = CoroutineStart.UNDISPATCHED) {
                withTimeout(30_000) {
                    ble.getTelemetryEvent().first { it is TelemetryEvent.UploadComplete || it is TelemetryEvent.UploadError }
                }
            }
            ble.uploadProjection(rgb565Data, width, height, frameCount, frameDelayMs)
            _quickProjection.value = QuickProjectionStatus.WaitingAck
            when (val event = ack.await()) {
                is TelemetryEvent.UploadComplete -> _quickProjection.value = QuickProjectionStatus.Success
                is TelemetryEvent.UploadError -> _quickProjection.value =
                    QuickProjectionStatus.Error("LED báo lỗi: ${event.error}")
                else -> Unit
            }
        }
    }

    private suspend fun downloadBytes(url: String): ByteArray = withTimeout(20_000) {
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder().url(url).build()
                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) error("Không tải được ảnh (lỗi ${response.code}).")
                    response.body?.bytes() ?: error("Phản hồi ảnh trống.")
                }
            } catch (e: IOException) {
                error("Không kết nối được tới máy chủ để tải ảnh. Kiểm tra mạng và thử lại.")
            }
        }
    }
}