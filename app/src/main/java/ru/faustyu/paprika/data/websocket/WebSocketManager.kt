package ru.faustyu.paprika.data.websocket

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
import ru.faustyu.paprika.util.Constants
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sealed class representing WebSocket events
 */
sealed class WebSocketEvent {
    data class Message(val text: String) : WebSocketEvent()
    object Connected : WebSocketEvent()
    object Disconnected : WebSocketEvent()
    data class Error(val exception: Throwable) : WebSocketEvent()
}

/**
 * WebSocket manager with automatic reconnection and WSS support
 */
@Singleton
class WebSocketManager @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    private var webSocket: WebSocket? = null
    private var currentUrl: String? = null
    private var shouldReconnect = false
    private var reconnectAttempts = 0
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    
    private val _events = MutableSharedFlow<WebSocketEvent>()
    val events: SharedFlow<WebSocketEvent> = _events.asSharedFlow()
    
    /**
     * Connect to WebSocket with token
     * Automatically uses WSS for https URLs
     */
    fun connect(baseUrl: String, token: String) {
        val wsUrl = baseUrl
            .replace("http://", "ws://")
            .replace("https://", "wss://")
            .removeSuffix("/") + "/ws?token=$token"
        
        currentUrl = wsUrl
        shouldReconnect = true
        reconnectAttempts = 0
        
        Log.d(TAG, "Connecting to WebSocket: $wsUrl")
        connectInternal(wsUrl)
    }
    
    /**
     * Internal connection method
     */
    private fun connectInternal(url: String) {
        try {
            val request = Request.Builder()
                .url(url)
                .build()
            
            webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    Log.d(TAG, "WebSocket connected")
                    reconnectAttempts = 0
                    coroutineScope.launch {
                        _events.emit(WebSocketEvent.Connected)
                    }
                }
                
                override fun onMessage(webSocket: WebSocket, text: String) {
                    Log.d(TAG, "WebSocket message received: $text")
                    coroutineScope.launch {
                        _events.emit(WebSocketEvent.Message(text))
                    }
                }
                
                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    Log.d(TAG, "WebSocket closing: $code - $reason")
                    webSocket.close(1000, null)
                }
                
                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    Log.d(TAG, "WebSocket closed: $code - $reason")
                    coroutineScope.launch {
                        _events.emit(WebSocketEvent.Disconnected)
                    }
                    attemptReconnect()
                }
                
                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    Log.e(TAG, "WebSocket failure", t)
                    coroutineScope.launch {
                        _events.emit(WebSocketEvent.Error(t))
                    }
                    attemptReconnect()
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect WebSocket", e)
            coroutineScope.launch {
                _events.emit(WebSocketEvent.Error(e))
            }
            attemptReconnect()
        }
    }
    
    /**
     * Attempt to reconnect with exponential backoff
     */
    private fun attemptReconnect() {
        if (!shouldReconnect || currentUrl == null) {
            return
        }
        
        if (reconnectAttempts >= Constants.WEBSOCKET_MAX_RECONNECT_ATTEMPTS) {
            Log.w(TAG, "Max reconnection attempts reached")
            shouldReconnect = false
            return
        }
        
        reconnectAttempts++
        val delay = Constants.WEBSOCKET_RECONNECT_DELAY_MS * reconnectAttempts
        
        Log.d(TAG, "Attempting reconnect #$reconnectAttempts in ${delay}ms")
        coroutineScope.launch {
            delay(delay)
            currentUrl?.let { connectInternal(it) }
        }
    }
    
    /**
     * Send message through WebSocket
     */
    fun sendMessage(text: String): Boolean {
        return webSocket?.send(text) ?: false
    }
    
    /**
     * Disconnect WebSocket
     */
    fun disconnect() {
        shouldReconnect = false
        webSocket?.close(1000, "User disconnected")
        webSocket = null
        currentUrl = null
        reconnectAttempts = 0
    }
    
    /**
     * Check if WebSocket is connected
     */
    fun isConnected(): Boolean {
        return webSocket != null
    }
    
    companion object {
        private const val TAG = "WebSocketManager"
    }
}
