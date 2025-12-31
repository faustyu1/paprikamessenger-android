package ru.faustyu.paprika.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stories")
data class StoryEntity(
    @PrimaryKey val id: Long,
    val userId: Long,
    val mediaUrl: String,
    val mediaType: String,
    val caption: String,
    val expiresAt: String
)
