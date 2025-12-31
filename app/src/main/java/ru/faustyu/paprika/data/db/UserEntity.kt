package ru.faustyu.paprika.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: Long,
    val username: String,
    val firstName: String?,
    val lastName: String?,
    val bio: String?,
    val avatar: String?,
    val lastUpdated: Long = System.currentTimeMillis()
)
