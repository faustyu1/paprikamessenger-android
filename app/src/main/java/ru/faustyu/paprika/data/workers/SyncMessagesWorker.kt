package ru.faustyu.paprika.data.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import ru.faustyu.paprika.data.db.MessageDao
import ru.faustyu.paprika.data.network.ApiService
import ru.faustyu.paprika.util.Constants

/**
 * Worker for syncing pending messages when network becomes available
 * Runs periodically to ensure all messages are sent
 */
@HiltWorker
class SyncMessagesWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val apiService: ApiService,
    private val messageDao: MessageDao
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        return try {
            // Get all pending messages
            val pendingMessages = messageDao.getPendingMessages()
            
            if (pendingMessages.isEmpty()) {
                return Result.success()
            }
            
            var successCount = 0
            var failureCount = 0
            
            // Try to send each pending message
            for (message in pendingMessages) {
                try {
                    val response = apiService.sendMessage(
                        message.chatId.toString(),
                        ru.faustyu.paprika.data.network.SendMessageDto(
                            content = message.content,
                            type = message.type
                        )
                    )
                    
                    if (response.isSuccessful && response.body() != null) {
                        messageDao.updateMessageStatus(
                            message.localId,
                            Constants.MESSAGE_STATUS_DELIVERED
                        )
                        successCount++
                    } else {
                        failureCount++
                    }
                } catch (e: Exception) {
                    failureCount++
                }
            }
            
            // Success if at least some messages were sent
            if (successCount > 0) {
                Result.success()
            } else if (failureCount > 0) {
                Result.retry()
            } else {
                Result.success()
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }
    
    companion object {
        const val WORK_NAME = "sync_messages"
    }
}
