package ru.faustyu.paprika.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0, // Local ID for Room
    val id: Long, // Server ID
    val chatId: Long,
    val senderId: Long,
    val content: String,
    val type: String,
    val status: String,
    val createdAt: Long, // Timestamp
    val isMe: Boolean // Computed field stored for convenience
)
