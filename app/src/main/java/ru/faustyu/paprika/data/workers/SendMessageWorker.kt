package ru.faustyu.paprika.data.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import ru.faustyu.paprika.data.db.MessageDao
import ru.faustyu.paprika.data.network.ApiService
import ru.faustyu.paprika.data.network.SendMessageDto
import ru.faustyu.paprika.util.Constants

/**
 * Worker for sending messages in background with retry logic
 * Works even when app is closed or no internet
 */
@HiltWorker
class SendMessageWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val apiService: ApiService,
    private val messageDao: MessageDao
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        val chatId = inputData.getString(KEY_CHAT_ID) ?: return Result.failure()
        val content = inputData.getString(KEY_CONTENT) ?: return Result.failure()
        val localId = inputData.getLong(KEY_LOCAL_ID, -1L)
        val messageType = inputData.getString(KEY_MESSAGE_TYPE) ?: Constants.MESSAGE_TYPE_TEXT
        
        if (localId == -1L) return Result.failure()
        
        return try {
            // Send to server
            val response = apiService.sendMessage(
                chatId,
                SendMessageDto(content = content, type = messageType)
            )
            
            if (response.isSuccessful && response.body() != null) {
                val serverMsg = response.body()!!
                
                // Update local message with server ID
                messageDao.updateMessageStatus(localId, serverMsg.status)
                
                Result.success()
            } else {
                // Retry on failure
                if (runAttemptCount < MAX_RETRIES) {
                    Result.retry()
                } else {
                    // Mark as failed after max retries
                    messageDao.updateMessageStatus(localId, Constants.MESSAGE_STATUS_FAILED)
                    Result.failure()
                }
            }
        } catch (e: Exception) {
            // Network error - retry
            if (runAttemptCount < MAX_RETRIES) {
                Result.retry()
            } else {
                messageDao.updateMessageStatus(localId, Constants.MESSAGE_STATUS_FAILED)
                Result.failure()
            }
        }
    }
    
    companion object {
        const val KEY_CHAT_ID = "chat_id"
        const val KEY_CONTENT = "content"
        const val KEY_LOCAL_ID = "local_id"
        const val KEY_MESSAGE_TYPE = "message_type"
        const val MAX_RETRIES = 3
        
        const val WORK_NAME_PREFIX = "send_message_"
    }
}
