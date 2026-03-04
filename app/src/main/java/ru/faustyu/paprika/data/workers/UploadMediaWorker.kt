package ru.faustyu.paprika.data.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import ru.faustyu.paprika.data.network.ApiService
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

/**
 * Worker for uploading media files in background
 * Handles large files with retry logic
 */
@HiltWorker
class UploadMediaWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val apiService: ApiService
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        val filePath = inputData.getString(KEY_FILE_PATH) ?: return Result.failure()
        val file = File(filePath)
        
        if (!file.exists()) {
            return Result.failure()
        }
        
        return try {
            val mediaType = "image/*".toMediaTypeOrNull()
            val requestFile = file.asRequestBody(mediaType)
            val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
            
            val response = apiService.uploadMedia(body)
            
            if (response.isSuccessful && response.body() != null) {
                val url = response.body()!!["url"]
                
                if (url != null) {
                    // Return URL as output data
                    val outputData = androidx.work.workDataOf(
                        KEY_UPLOADED_URL to url
                    )
                    Result.success(outputData)
                } else {
                    Result.failure()
                }
            } else {
                if (runAttemptCount < MAX_RETRIES) {
                    Result.retry()
                } else {
                    Result.failure()
                }
            }
        } catch (e: Exception) {
            if (runAttemptCount < MAX_RETRIES) {
                Result.retry()
            } else {
                Result.failure()
            }
        } finally {
            // Clean up temp file
            if (file.exists()) {
                file.delete()
            }
        }
    }
    
    companion object {
        const val KEY_FILE_PATH = "file_path"
        const val KEY_UPLOADED_URL = "uploaded_url"
        const val MAX_RETRIES = 3
        
        const val WORK_NAME_PREFIX = "upload_media_"
    }
}
