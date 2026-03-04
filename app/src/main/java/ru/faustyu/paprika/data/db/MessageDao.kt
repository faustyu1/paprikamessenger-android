package ru.faustyu.paprika.data.db

import androidx.paging.PagingSource
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    
    /**
     * Get messages for chat as Flow (reactive updates)
     */
    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY createdAt DESC")
    fun getMessagesForChat(chatId: Long): Flow<List<MessageEntity>>
    
    /**
     * Get messages for chat with Paging 3 support
     * Automatically handles pagination
     */
    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY createdAt DESC")
    fun getMessagesPaged(chatId: Long): PagingSource<Int, MessageEntity>
    
    /**
     * Get server IDs for deduplication
     */
    @Query("SELECT id FROM messages WHERE chatId = :chatId")
    suspend fun getServerIdsForChat(chatId: Long): List<Long>
    
    /**
     * Insert single message (upsert on conflict)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity): Long
    
    /**
     * Insert multiple messages
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)
    
    /**
     * Update message status
     */
    @Query("UPDATE messages SET status = :status WHERE localId = :localId")
    suspend fun updateMessageStatus(localId: Long, status: String)
    
    /**
     * Get pending messages (for offline sync)
     */
    @Query("SELECT * FROM messages WHERE status IN ('sent', 'uploading', 'failed') AND isMe = 1")
    suspend fun getPendingMessages(): List<MessageEntity>
    
    /**
     * Delete all messages for chat
     */
    @Query("DELETE FROM messages WHERE chatId = :chatId")
    suspend fun deleteMessagesForChat(chatId: Long)
    
    /**
     * Get message count for chat
     */
    @Query("SELECT COUNT(*) FROM messages WHERE chatId = :chatId")
    suspend fun getMessageCount(chatId: Long): Int

    @Query("SELECT * FROM messages WHERE id = :id LIMIT 1")
    suspend fun getMessageById(id: Long): MessageEntity?

    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun deleteMessageById(id: Long)
}
