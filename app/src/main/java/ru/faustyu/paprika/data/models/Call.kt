package ru.faustyu.paprika.data.models

data class Call(
    val id: Long,
    val createdAt: String,
    val endedAt: String? = null,
    val callerId: Long,
    val calleeId: Long,
    val caller: UserPublic? = null,
    val callee: UserPublic? = null,
    val status: String, // "initiated", "ringing", "active", "ended", "missed", "rejected"
    val callType: String, // "audio", "video"
    val duration: Int = 0
)

data class UserPublic(
    val id: Long,
    val username: String,
    val avatar: String? = null,
    val firstName: String? = null,
    val lastName: String? = null
)
