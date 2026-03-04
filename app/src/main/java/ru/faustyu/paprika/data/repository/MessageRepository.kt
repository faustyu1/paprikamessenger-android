package ru.faustyu.paprika.data.repository

import android.content.Context
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.work.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import ru.faustyu.paprika.data.db.MessageDao
import ru.faustyu.paprika.data.db.MessageEntity
import ru.faustyu.paprika.data.network.ApiService
import ru.faustyu.paprika.data.network.MessageDto
import ru.faustyu.paprika.data.workers.SendMessageWorker
import ru.faustyu.paprika.data.workers.SyncMessagesWorker
import ru.faustyu.paprika.data.workers.UploadMediaWorker
import ru.faustyu.paprika.util.Constants
import ru.faustyu.paprika.util.Result
import ru.faustyu.paprika.util.parseToUnixSeconds
import ru.faustyu.paprika.util.safeApiCall
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for message operations with Paging 3 and WorkManager support
 * Implements offline-first architecture
 */
@Singleton
class MessageRepository @Inject constructor(
    private val apiService: ApiService,
    private val messageDao: MessageDao,
    private val workManager: WorkManager,
    @ApplicationContext private val context: Context
) {
    
    /**
     * Get messages for chat with Paging 3
     * Loads messages in pages for better performance
     */
    fun getMessagesPaged(chatId: Long): Flow<PagingData<MessageEntity>> {
        return Pager(
            config = PagingConfig(
                pageSize = Constants.PAGE_SIZE,
                initialLoadSize = Constants.INITIAL_LOAD_SIZE,
                enablePlaceholders = false,
                prefetchDistance = 10
            ),
            pagingSourceFactory = { messageDao.getMessagesPaged(chatId) }
        ).flow
    }
    
    /**
     * Get messages for a chat (from database - single source of truth)
     * Use this for simple non-paged lists
     */
    fun getMessagesForChat(chatId: Long): Flow<List<MessageEntity>> {
        return messageDao.getMessagesForChat(chatId)
    }
    
    /**
     * Fetch messages from server and sync with database
     * Call this when opening chat or pulling to refresh
     */
    suspend fun fetchChatMessages(chatId: String, myUserId: Long): Result<Unit> {
        return safeApiCall {
            val cid = if (chatId == Constants.SYSTEM_CHAT_STRING_ID) {
                Constants.SYSTEM_CHAT_ID
            } else {
                chatId.toLongOrNull() ?: 0L
            }
            
            val response = apiService.getChatMessages(chatId)
            if (response.isSuccessful && response.body() != null) {
                val messages = response.body()!!
                val existingIds = messageDao.getServerIdsForChat(cid).toSet()
                
                val newMessages = messages
                    .filter { !existingIds.contains(it.id) }
                    .map { it.toEntity(cid, myUserId) }
                
                if (newMessages.isNotEmpty()) {
                    messageDao.insertMessages(newMessages)
                }
            } else {
                throw Exception("Failed to fetch messages")
            }
        }
    }
    
    /**
     * Send text message with offline support via WorkManager
     * Message will be sent even if app is closed or no internet
     */
    suspend fun sendMessage(
        chatId: String,
        content: String,
        myUserId: Long
    ): Result<MessageEntity> {
        return try {
            val cid = if (chatId == Constants.SYSTEM_CHAT_STRING_ID) {
                Constants.SYSTEM_CHAT_ID
            } else {
                chatId.toLongOrNull() ?: 0L
            }
            
            // 1. Optimistic insert to database
            val tempId = System.currentTimeMillis()
            val tempMsg = MessageEntity(
                localId = 0,
                id = tempId,
                chatId = cid,
                senderId = myUserId,
                content = content,
                type = Constants.MESSAGE_TYPE_TEXT,
                status = Constants.MESSAGE_STATUS_SENT,
                createdAt = System.currentTimeMillis() / 1000,
                isMe = true
            )
            val localId = messageDao.insertMessage(tempMsg)
            
            // 2. Schedule background work with WorkManager
            val workData = workDataOf(
                SendMessageWorker.KEY_CHAT_ID to chatId,
                SendMessageWorker.KEY_CONTENT to content,
                SendMessageWorker.KEY_LOCAL_ID to localId,
                SendMessageWorker.KEY_MESSAGE_TYPE to Constants.MESSAGE_TYPE_TEXT
            )
            
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            
            val sendWork = OneTimeWorkRequestBuilder<SendMessageWorker>()
                .setInputData(workData)
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    10,
                    TimeUnit.SECONDS
                )
                .build()
            
            workManager.enqueueUniqueWork(
                "${SendMessageWorker.WORK_NAME_PREFIX}$localId",
                ExistingWorkPolicy.REPLACE,
                sendWork
            )
            
            // Return optimistically inserted message
            Result.Success(tempMsg.copy(localId = localId))
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    /**
     * Upload media and send as message with offline support
     */
    suspend fun sendImageMessage(
        chatId: String,
        imageUri: android.net.Uri,
        myUserId: Long
    ): Result<MessageEntity> {
        return try {
            val cid = if (chatId == Constants.SYSTEM_CHAT_STRING_ID) {
                Constants.SYSTEM_CHAT_ID
            } else {
                chatId.toLongOrNull() ?: 0L
            }
            
            // 1. Copy file to temp location
            val inputStream = context.contentResolver.openInputStream(imageUri)
            val tempFile = java.io.File(context.cacheDir, "upload_${System.currentTimeMillis()}.jpg")
            tempFile.outputStream().use { output ->
                inputStream?.copyTo(output)
            }
            inputStream?.close()
            
            // 2. Create optimistic message
            val tempMsg = MessageEntity(
                localId = 0,
                id = System.currentTimeMillis(),
                chatId = cid,
                senderId = myUserId,
                content = tempFile.absolutePath, // Temporary local path
                type = Constants.MESSAGE_TYPE_IMAGE,
                status = Constants.MESSAGE_STATUS_UPLOADING,
                createdAt = System.currentTimeMillis() / 1000,
                isMe = true
            )
            val localId = messageDao.insertMessage(tempMsg)
            
            // 3. Schedule upload work
            val uploadWorkData = workDataOf(
                UploadMediaWorker.KEY_FILE_PATH to tempFile.absolutePath
            )
            
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            
            val uploadWork = OneTimeWorkRequestBuilder<UploadMediaWorker>()
                .setInputData(uploadWorkData)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
                .build()
            
            // 4. Chain with send message work
            workManager.beginUniqueWork(
                "${UploadMediaWorker.WORK_NAME_PREFIX}$localId",
                ExistingWorkPolicy.REPLACE,
                uploadWork
            ).enqueue()
            
            Result.Success(tempMsg.copy(localId = localId))
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    /**
     * Mark chat as read
     */
    suspend fun markChatAsRead(chatId: String): Result<Unit> {
        return safeApiCall {
            apiService.markChatRead(chatId)
        }
    }
    
    /**
     * Setup periodic sync for pending messages
     * Call this once during app initialization
     */
    fun setupPeriodicSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        val syncWork = PeriodicWorkRequestBuilder<SyncMessagesWorker>(
            15, // Repeat every 15 minutes
            TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()
        
        workManager.enqueueUniquePeriodicWork(
            SyncMessagesWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            syncWork
        )
    }
    
    /**
     * Get count of pending messages
     */
    suspend fun getPendingMessagesCount(): Int {
        return messageDao.getPendingMessages().size
    }
}

/**
 * Extension to convert MessageDto to MessageEntity
 */
private fun MessageDto.toEntity(chatId: Long, myUserId: Long): MessageEntity {
    return MessageEntity(
        localId = 0,
        id = id,
        chatId = chatId,
        senderId = sender_id,
        content = content,
        type = type,
        status = if (status == Constants.MESSAGE_STATUS_SENT) {
            Constants.MESSAGE_STATUS_READ  // History messages are considered read
        } else {
            status
        },
        createdAt = created_at.parseToUnixSeconds(System.currentTimeMillis() / 1000),
        isMe = sender_id == myUserId
    )
}
