package ru.faustyu.paprika.data.repository

import android.util.Log
import kotlinx.coroutines.flow.Flow
import ru.faustyu.paprika.data.PrefsManager
import ru.faustyu.paprika.data.websocket.WebSocketEvent
import ru.faustyu.paprika.data.websocket.WebSocketManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for WebSocket operations
 */
@Singleton
class WebSocketRepository @Inject constructor(
    private val webSocketManager: WebSocketManager,
    private val prefsManager: PrefsManager
) {
    
    /**
     * Get WebSocket events flow
     */
    val events: Flow<WebSocketEvent> = webSocketManager.events
    
    /**
     * Connect to WebSocket
     */
    fun connect(baseUrl: String) {
        val token = prefsManager.token
        if (token.isNullOrBlank()) {
            Log.e("WebSocketRepository", "No auth token available")
            return
        }
        
        webSocketManager.connect(baseUrl, token)
    }
    
    /**
     * Send message through WebSocket
     */
    fun sendMessage(text: String): Boolean {
        return webSocketManager.sendMessage(text)
    }
    
    /**
     * Disconnect WebSocket
     */
    fun disconnect() {
        webSocketManager.disconnect()
    }
    
    /**
     * Check if connected
     */
    fun isConnected(): Boolean {
        return webSocketManager.isConnected()
    }
}
