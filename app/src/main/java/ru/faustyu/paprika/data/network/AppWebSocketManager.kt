package ru.faustyu.paprika.data.network

import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.ConcurrentHashMap

object AppWebSocketManager {
    private val TAG = "AppWS"
    private var webSocket: WebSocket? = null
    private val gson = Gson()
    private val listeners = ConcurrentHashMap<String, (Map<String, Any?>) -> Unit>()
    private val mapType = object : TypeToken<Map<String, Any?>>() {}.type

    var onNewMessage: ((Long?) -> Unit)? = null

    fun connect(token: String, baseUrl: String) {
        if (webSocket != null) return
        val wsUrl = baseUrl.replace("http://", "ws://")
            .replace("https://", "wss://")
            .trimEnd('/') + "/ws"
        val request = Request.Builder()
            .url(wsUrl)
            .addHeader("Authorization", "Bearer $token")
            .build()
        webSocket = OkHttpClient().newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: okhttp3.Response) {
                Log.d(TAG, "Connected")
            }

            override fun onMessage(ws: WebSocket, text: String) {
                try {
                    val msg: Map<String, Any?> = gson.fromJson(text, mapType)
                    val type = msg["type"] as? String
                    val event = msg["event"] as? String
                    if (type == "new_message" || event == "message:new") {
                        val senderId = (msg["sender_id"] as? Double)?.toLong()
                        onNewMessage?.invoke(senderId)
                    }
                    listeners.values.forEach { it(msg) }
                } catch (e: Exception) {
                    Log.e(TAG, "Parse error: $e")
                }
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: okhttp3.Response?) {
                Log.e(TAG, "WebSocket error: $t")
                webSocket = null
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "Closed: $reason")
                webSocket = null
            }
        })
    }

    fun addListener(tag: String, listener: (Map<String, Any?>) -> Unit) {
        listeners[tag] = listener
    }

    fun removeListener(tag: String) {
        listeners.remove(tag)
    }

    fun send(data: Map<String, Any?>) {
        val json = gson.toJson(data)
        if (webSocket?.send(json) == false) {
            Log.e(TAG, "Failed to send: $json")
        }
    }

    fun disconnect() {
        webSocket?.close(1000, "Logout")
        webSocket = null
        listeners.clear()
    }
}
