package ru.faustyu.paprika.data.db

import androidx.room.*

@Entity(tableName = "chats")
data class ChatEntity(
    @PrimaryKey val id: Long,
    val type: Int,
    val title: String,
    val avatar: String,
    val lastMessagePreview: String?,
    val lastMessageAt: Long,
    val unreadCount: Long,
    val muteUntil: Long,
    val isArchived: Boolean,
    val isPinned: Boolean,
    val cachedAt: Long = System.currentTimeMillis()
)

@Dao
interface ChatDao {
    @Query("SELECT * FROM chats")
    fun getAllChats(): kotlinx.coroutines.flow.Flow<List<ChatEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChats(chats: List<ChatEntity>)

    @Query("DELETE FROM chats")
    suspend fun clearAll()
}
