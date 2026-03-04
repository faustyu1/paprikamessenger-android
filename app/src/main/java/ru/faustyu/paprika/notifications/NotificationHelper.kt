package ru.faustyu.paprika.notifications

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.graphics.drawable.IconCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import ru.faustyu.paprika.MainActivity
import ru.faustyu.paprika.R
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper class for creating and managing notifications
 * Supports Android 8+ notification channels
 */
@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    
    companion object {
        const val CHANNEL_ID_MESSAGES = "messages_channel"
        const val CHANNEL_ID_SYSTEM = "system_channel"
        const val CHANNEL_NAME_MESSAGES = "Сообщения"
        const val CHANNEL_NAME_SYSTEM = "Системные уведомления"
        
        private const val NOTIFICATION_ID_BASE = 1000
    }
    
    init {
        createNotificationChannels()
    }
    
    /**
     * Create notification channels for Android 8+
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Channel for messages
            val messagesChannel = NotificationChannel(
                CHANNEL_ID_MESSAGES,
                CHANNEL_NAME_MESSAGES,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Уведомления о новых сообщениях"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }
            
            // Channel for system notifications
            val systemChannel = NotificationChannel(
                CHANNEL_ID_SYSTEM,
                CHANNEL_NAME_SYSTEM,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Системные уведомления приложения"
                setShowBadge(false)
            }
            
            notificationManager.createNotificationChannel(messagesChannel)
            notificationManager.createNotificationChannel(systemChannel)
        }
    }
    
    /**
     * Show notification for new message
     */
    @SuppressLint("MissingPermission")
    fun showMessageNotification(
        chatId: Long,
        chatTitle: String,
        senderName: String,
        messageText: String,
        senderAvatar: String? = null
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("chatId", chatId.toString())
            putExtra("openChat", true)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            chatId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Create person for sender
        val sender = Person.Builder()
            .setName(senderName)
            .apply {
                senderAvatar?.let {
                    // TODO: Load avatar as IconCompat
                }
            }
            .build()
        
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_MESSAGES)
            .setSmallIcon(R.mipmap.ic_launcher) // TODO: Create proper notification icon
            .setContentTitle(chatTitle)
            .setContentText("$senderName: $messageText")
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setStyle(
                NotificationCompat.MessagingStyle(sender)
                    .setConversationTitle(chatTitle)
                    .addMessage(messageText, System.currentTimeMillis(), sender)
            )
            .build()
        
        val notificationId = NOTIFICATION_ID_BASE + chatId.toInt()
        notificationManager.notify(notificationId, notification)
    }
    
    /**
     * Show system notification (e.g., message sent successfully)
     */
    @SuppressLint("MissingPermission")
    fun showSystemNotification(
        title: String,
        message: String,
        notificationId: Int = System.currentTimeMillis().toInt()
    ) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_SYSTEM)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        
        notificationManager.notify(notificationId, notification)
    }
    
    /**
     * Cancel notification for specific chat
     */
    fun cancelChatNotification(chatId: Long) {
        val notificationId = NOTIFICATION_ID_BASE + chatId.toInt()
        notificationManager.cancel(notificationId)
    }
    
    /**
     * Cancel all notifications
     */
    fun cancelAllNotifications() {
        notificationManager.cancelAll()
    }
}
