package ru.faustyu.paprika.util

/**
 * Application-wide constants
 */
object Constants {
    // Chat types
    const val CHAT_TYPE_PRIVATE = 0
    const val CHAT_TYPE_GROUP = 1
    const val CHAT_TYPE_CHANNEL = 2
    
    // Special chat IDs
    const val SYSTEM_CHAT_ID = 1L
    const val SYSTEM_CHAT_STRING_ID = "paprika_system"
    
    // Message types
    const val MESSAGE_TYPE_TEXT = "text"
    const val MESSAGE_TYPE_IMAGE = "image"
    const val MESSAGE_TYPE_VIDEO = "video"
    const val MESSAGE_TYPE_FILE = "file"
    
    // Message status
    const val MESSAGE_STATUS_SENT = "sent"
    const val MESSAGE_STATUS_DELIVERED = "delivered"
    const val MESSAGE_STATUS_READ = "read"
    const val MESSAGE_STATUS_FAILED = "failed"
    const val MESSAGE_STATUS_UPLOADING = "uploading"
    
    // Validation
    const val MIN_PASSWORD_LENGTH = 6
    const val MIN_USERNAME_LENGTH = 3
    
    // Pagination
    const val PAGE_SIZE = 50
    const val INITIAL_LOAD_SIZE = 100
    
    // Timeouts
    const val NETWORK_TIMEOUT_SECONDS = 30L
    const val WEBSOCKET_RECONNECT_DELAY_MS = 3000L
    const val WEBSOCKET_MAX_RECONNECT_ATTEMPTS = 5
}

/**
 * Chat type enum for type-safe usage
 */
enum class ChatType(val value: Int) {
    PRIVATE(Constants.CHAT_TYPE_PRIVATE),
    GROUP(Constants.CHAT_TYPE_GROUP),
    CHANNEL(Constants.CHAT_TYPE_CHANNEL);
    
    companion object {
        fun fromValue(value: Int): ChatType = values().firstOrNull { it.value == value } ?: PRIVATE
    }
}

/**
 * Message type enum
 */
enum class MessageType(val value: String) {
    TEXT(Constants.MESSAGE_TYPE_TEXT),
    IMAGE(Constants.MESSAGE_TYPE_IMAGE),
    VIDEO(Constants.MESSAGE_TYPE_VIDEO),
    FILE(Constants.MESSAGE_TYPE_FILE);
    
    companion object {
        fun fromValue(value: String): MessageType = values().firstOrNull { it.value == value } ?: TEXT
    }
}

/**
 * Message status enum
 */
enum class MessageStatus(val value: String) {
    SENT(Constants.MESSAGE_STATUS_SENT),
    DELIVERED(Constants.MESSAGE_STATUS_DELIVERED),
    READ(Constants.MESSAGE_STATUS_READ),
    FAILED(Constants.MESSAGE_STATUS_FAILED),
    UPLOADING(Constants.MESSAGE_STATUS_UPLOADING);
    
    companion object {
        fun fromValue(value: String): MessageStatus = values().firstOrNull { it.value == value } ?: SENT
    }
}
