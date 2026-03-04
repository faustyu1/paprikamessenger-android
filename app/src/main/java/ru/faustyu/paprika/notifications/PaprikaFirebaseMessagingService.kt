package ru.faustyu.paprika.notifications

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import ru.faustyu.paprika.data.PrefsManager
import ru.faustyu.paprika.data.repository.MessageRepository
import javax.inject.Inject

/**
 * Firebase Cloud Messaging service for handling push notifications
 * Receives messages even when app is in background
 */
@AndroidEntryPoint
class PaprikaFirebaseMessagingService : FirebaseMessagingService() {
    
    @Inject
    lateinit var notificationHelper: NotificationHelper
    
    @Inject
    lateinit var messageRepository: MessageRepository
    
    @Inject
    lateinit var prefsManager: PrefsManager
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    /**
     * Called when a new FCM message is received
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        
        Log.d(TAG, "Message received from: ${remoteMessage.from}")
        
        // Check for data payload
        remoteMessage.data.isNotEmpty().let {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")
            
            handleDataMessage(remoteMessage.data)
        }
        
        // Check for notification payload (fallback)
        remoteMessage.notification?.let { notification ->
            Log.d(TAG, "Message notification body: ${notification.body}")
            
            showNotification(
                title = notification.title ?: "Новое сообщение",
                body = notification.body ?: "",
                data = remoteMessage.data
            )
        }
    }
    
    /**
     * Handle FCM data message
     */
    private fun handleDataMessage(data: Map<String, String>) {
        val type = data["type"] ?: return
        
        when (type) {
            "new_message" -> handleNewMessage(data)
            "message_read" -> handleMessageRead(data)
            "typing" -> handleTypingIndicator(data)
            else -> {
                Log.w(TAG, "Unknown message type: $type")
            }
        }
    }
    
    /**
     * Handle new message notification
     */
    private fun handleNewMessage(data: Map<String, String>) {
        val chatId = data["chat_id"]?.toLongOrNull() ?: return
        val chatTitle = data["chat_title"] ?: "Чат"
        val senderName = data["sender_name"] ?: "Пользователь"
        val messageText = data["message_text"] ?: ""
        val senderAvatar = data["sender_avatar"]
        
        // Show notification
        notificationHelper.showMessageNotification(
            chatId = chatId,
            chatTitle = chatTitle,
            senderName = senderName,
            messageText = messageText,
            senderAvatar = senderAvatar
        )
        
        // Sync messages in background
        serviceScope.launch {
            val myUserId = prefsManager.userId
            messageRepository.fetchChatMessages(chatId.toString(), myUserId)
        }
    }
    
    /**
     * Handle message read notification
     */
    private fun handleMessageRead(data: Map<String, String>) {
        val chatId = data["chat_id"]?.toLongOrNull() ?: return
        
        // Cancel notification for this chat
        notificationHelper.cancelChatNotification(chatId)
    }
    
    /**
     * Handle typing indicator (optional - can be used for live updates)
     */
    private fun handleTypingIndicator(data: Map<String, String>) {
        val chatId = data["chat_id"]?.toLongOrNull() ?: return
        val isTyping = data["is_typing"]?.toBoolean() ?: false
        
        Log.d(TAG, "User typing in chat $chatId: $isTyping")
        // TODO: Update UI if chat is open
    }
    
    /**
     * Show notification (fallback for notification payload)
     */
    private fun showNotification(
        title: String,
        body: String,
        data: Map<String, String>
    ) {
        val chatId = data["chat_id"]?.toLongOrNull() ?: 0L
        
        notificationHelper.showMessageNotification(
            chatId = chatId,
            chatTitle = title,
            senderName = "Отправитель",
            messageText = body
        )
    }
    
    /**
     * Called when a new FCM token is generated
     * Send this token to your backend to enable push notifications
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        
        Log.d(TAG, "New FCM token: $token")
        
        // Save token locally
        prefsManager.apply {
            // TODO: Add fcmToken to PrefsManager
        }
        
        // Send token to backend
        serviceScope.launch {
            sendTokenToBackend(token)
        }
    }
    
    /**
     * Send FCM token to backend for registration
     */
    private suspend fun sendTokenToBackend(token: String) {
        try {
            // TODO: Create API endpoint to register FCM token
            // apiService.registerFcmToken(token)
            Log.d(TAG, "FCM token sent to backend: $token")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send FCM token to backend", e)
        }
    }
    
    companion object {
        private const val TAG = "PaprikaFCM"
    }
}
