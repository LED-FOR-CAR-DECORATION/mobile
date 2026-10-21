package com.carled.app.data.ble

import com.carled.app.domain.model.TelemetryEvent
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicInteger

/** One outstanding packet; retry absolute pixel writes safely when a packet/notification is lost. */
class ConfirmedCanvasSender(private val events: SharedFlow<TelemetryEvent>,
    private val write: (ByteArray) -> Unit, private val timeoutMs: Long = 800, private val retryDelayMs: Long = 40) {
    private val mutex = Mutex()
    private val sequences = AtomicInteger(kotlin.random.Random.nextInt(65536))
    suspend fun send(points: List<TouchPoint>) = mutex.withLock {
        for (chunk in points.chunked(BleProtocolHelper.MAX_DRAW_POINTS)) {
            sendPacket(chunk)
        }
    }
    suspend fun clear() = mutex.withLock { sendPacket(emptyList()) }
    private suspend fun sendPacket(chunk: List<TouchPoint>) {
            val sequence = sequences.getAndIncrement() and 65535
            val frame = if(chunk.isEmpty()) BleProtocolHelper.buildFrame(BleProtocolHelper.CMD_DRAW_CONFIRMED,sequence ushr 8,sequence and 255,0)
                else BleProtocolHelper.buildConfirmedBatchFrame(chunk,sequence)
            var applied = false
            var lastError: Exception? = null
            for (attempt in 0 until 3) {
                applied = coroutineScope {
                    val ack = async(start=CoroutineStart.UNDISPATCHED) {
                        withTimeoutOrNull(timeoutMs) { events.first { it is TelemetryEvent.CanvasBatchApplied && it.sequence == sequence } }
                    }
                    try { write(frame);ack.await()!=null }
                    catch(e:CancellationException) { throw e }
                    catch(e:Exception) { lastError=e;false }
                    finally { ack.cancel() }
                }
                if(applied) break
                delay(retryDelayMs)
            }
            check(applied) { "LED chưa xác nhận nét vẽ. Cần firmware hỗ trợ LIVE mới (0x1A). ${lastError?.message ?: "Kiểm tra kết nối hoặc nạp firmware mới."}" }
    }
}
