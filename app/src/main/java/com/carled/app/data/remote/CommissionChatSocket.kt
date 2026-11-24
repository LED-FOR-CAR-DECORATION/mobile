package com.carled.app.data.remote

import android.util.Log
import com.carled.app.domain.model.commission.CommissionMessageResponse
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

/**
 * Minimal STOMP-over-WebSocket client, hand-rolled on top of OkHttp's native
 * WebSocket support rather than pulling in a dedicated STOMP library — this
 * app has no STOMP dependency today, and the subset of the protocol actually
 * needed here (CONNECT, SUBSCRIBE, receiving MESSAGE frames on one topic at a
 * time, no transactions/acks/multiple subscriptions) is small enough to
 * hand-roll safely. If the project later needs more STOMP features, swap
 * this for a real library instead of growing this class further.
 *
 * Frame format per the STOMP spec: "COMMAND\nheader:value\n\nBODY\u0000".
 *
 * FIX (silent live-chat drops): a dead/idled TCP connection was never
 * detected or recovered from.
 *  - The owning OkHttpClient now needs a pingInterval set (see
 *    ServiceLocator.okHttpClient) so OkHttp actually notices a half-dead
 *    socket instead of sitting on it forever.
 *  - This class now retries with capped exponential backoff on any
 *    onFailure, or on onClosed that we didn't ask for ourselves, and
 *    re-sends CONNECT + SUBSCRIBE from scratch on every reconnect — STOMP
 *    has no frame-level resume, so a dropped connection always needs a full
 *    re-handshake. Callers (CommissionViewModel) still do a REST refresh on
 *    screen entry, which backfills anything missed while the socket was down.
 */
class CommissionChatSocket(
    private val client: OkHttpClient,
    private val wsBaseUrl: String, // e.g. "wss://host" or "ws://host", no trailing slash
    private val gson: Gson = Gson()
) {
    private var webSocket: WebSocket? = null
    private val _incoming = MutableSharedFlow<CommissionMessageResponse>(extraBufferCapacity = 16)
    val incoming: SharedFlow<CommissionMessageResponse> = _incoming.asSharedFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var currentCommissionId: Long? = null
    private var currentAuthToken: String? = null
    private var reconnectAttempt = 0
    private var reconnectJob: Job? = null
    private var intentionalDisconnect = false

    /** One commission room at a time — connecting to a new id replaces any existing connection. */
    fun connect(commissionId: Long, authToken: String) {
        Log.d("CommissionChatSocket", "connect() called for commission=$commissionId")
        currentCommissionId = commissionId
        currentAuthToken = authToken
        intentionalDisconnect = false
        reconnectAttempt = 0
        reconnectJob?.cancel()
        openSocket(commissionId, authToken)
    }

    private fun openSocket(commissionId: Long, authToken: String) {
        webSocket?.close(1000, "reconnecting")
        webSocket = null

        val request = Request.Builder().url("$wsBaseUrl/ws").build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                Log.d("CommissionChatSocket", "WS upgraded, sending CONNECT (commission=$commissionId)")
                reconnectAttempt = 0
                val host = wsBaseUrl.substringAfter("://")
                val connectFrame = "CONNECT\n" +
                        "accept-version:1.2\n" +
                        "host:$host\n" +
                        "Authorization:Bearer $authToken\n" +
                        "\n\u0000"
                ws.send(connectFrame)
            }

            override fun onMessage(ws: WebSocket, text: String) {
                val frame = parseFrame(text) ?: return
                val (command, headers, body) = frame
                when (command) {
                    "CONNECTED" -> {
                        Log.d("CommissionChatSocket", "STOMP CONNECTED, subscribing to commission=$commissionId")
                        subscribe(ws, commissionId)
                    }
                    "MESSAGE" -> if (body.isNotBlank()) {
                        try {
                            val parsed = gson.fromJson(body, CommissionMessageResponse::class.java)
                            Log.d("CommissionChatSocket", "Live MESSAGE frame received: messageId=${parsed.messageId} commissionId=${parsed.commissionId}")
                            _incoming.tryEmit(parsed)
                        } catch (e: Exception) {
                            Log.w("CommissionChatSocket", "Không parse được MESSAGE frame: ${e.message}")
                        }
                    }
                    "ERROR" -> Log.e("CommissionChatSocket", "STOMP ERROR frame: $headers $body")
                }
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                Log.e("CommissionChatSocket", "WebSocket failed: ${t.message}", t)
                scheduleReconnect()
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                if (!intentionalDisconnect) {
                    Log.w("CommissionChatSocket", "WebSocket closed unexpectedly: $code $reason")
                    scheduleReconnect()
                }
            }
        })
    }

    /** Capped exponential backoff: 1s, 2s, 4s, 8s, 16s, 30s, 30s, ... */
    private fun scheduleReconnect() {
        if (intentionalDisconnect) return
        val commissionId = currentCommissionId ?: return
        val authToken = currentAuthToken ?: return

        reconnectJob?.cancel()
        val delayMs = minOf(30_000L, 1_000L * (1L shl minOf(reconnectAttempt, 5)))
        reconnectAttempt++
        reconnectJob = scope.launch {
            delay(delayMs)
            if (!intentionalDisconnect) openSocket(commissionId, authToken)
        }
    }

    private fun subscribe(ws: WebSocket, commissionId: Long) {
        val subscribeFrame = "SUBSCRIBE\n" +
                "id:sub-$commissionId\n" +
                "destination:/topic/commissions/$commissionId\n" +
                "\n\u0000"
        ws.send(subscribeFrame)
        Log.d("CommissionChatSocket", "SUBSCRIBE sent for /topic/commissions/$commissionId")
    }

    fun disconnect() {
        intentionalDisconnect = true
        reconnectJob?.cancel()
        webSocket?.close(1000, "done")
        webSocket = null
        currentCommissionId = null
        currentAuthToken = null
    }

    /** Parses one raw STOMP frame into (command, headers, body). Null if malformed. */
    private fun parseFrame(raw: String): Triple<String, Map<String, String>, String>? {
        val trimmed = raw.trimEnd('\u0000')
        val headerBodySplit = trimmed.indexOf("\n\n")
        if (headerBodySplit == -1) return null
        val headerPart = trimmed.substring(0, headerBodySplit)
        val body = trimmed.substring(headerBodySplit + 2)
        val lines = headerPart.split("\n")
        val command = lines.firstOrNull() ?: return null
        val headers = lines.drop(1).mapNotNull { line ->
            val idx = line.indexOf(':')
            if (idx == -1) null else line.substring(0, idx) to line.substring(idx + 1)
        }.toMap()
        return Triple(command, headers, body)
    }
}