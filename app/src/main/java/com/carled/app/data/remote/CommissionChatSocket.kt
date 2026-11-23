package com.carled.app.data.remote

import android.util.Log
import com.carled.app.domain.model.commission.CommissionMessageResponse
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
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
 */
class CommissionChatSocket(
    private val client: OkHttpClient,
    private val wsBaseUrl: String, // e.g. "wss://host" or "ws://host", no trailing slash
    private val gson: Gson = Gson()
) {
    private var webSocket: WebSocket? = null
    private val _incoming = MutableSharedFlow<CommissionMessageResponse>(extraBufferCapacity = 16)
    val incoming: SharedFlow<CommissionMessageResponse> = _incoming.asSharedFlow()

    /** One commission room at a time — connecting to a new id replaces any existing connection. */
    fun connect(commissionId: Long, authToken: String) {
        disconnect()

        val request = Request.Builder().url("$wsBaseUrl/ws").build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                val connectFrame = "CONNECT\n" +
                        "accept-version:1.2\n" +
                        "Authorization:Bearer $authToken\n" +
                        "\n\u0000"
                ws.send(connectFrame)
            }

            override fun onMessage(ws: WebSocket, text: String) {
                val frame = parseFrame(text) ?: return
                val (command, headers, body) = frame
                when (command) {
                    "CONNECTED" -> subscribe(ws, commissionId)
                    "MESSAGE" -> if (body.isNotBlank()) {
                        try {
                            _incoming.tryEmit(gson.fromJson(body, CommissionMessageResponse::class.java))
                        } catch (e: Exception) {
                            Log.w("CommissionChatSocket", "Không parse được MESSAGE frame: ${e.message}")
                        }
                    }
                    "ERROR" -> Log.e("CommissionChatSocket", "STOMP ERROR frame: $headers $body")
                }
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                Log.e("CommissionChatSocket", "WebSocket failed: ${t.message}", t)
            }
        })
    }

    private fun subscribe(ws: WebSocket, commissionId: Long) {
        val subscribeFrame = "SUBSCRIBE\n" +
                "id:sub-$commissionId\n" +
                "destination:/topic/commissions/$commissionId\n" +
                "\n\u0000"
        ws.send(subscribeFrame)
    }

    fun disconnect() {
        webSocket?.close(1000, "done")
        webSocket = null
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