package ru.faustyu.paprika.ui.stories

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import ru.faustyu.paprika.data.network.CreateStoryRequest
import ru.faustyu.paprika.data.network.NetworkModule
import ru.faustyu.paprika.data.network.Story
import okhttp3.MediaType.Companion.toMediaTypeOrNull

class StoriesViewModel(application: android.app.Application) : androidx.lifecycle.AndroidViewModel(application) {
    private val db = ru.faustyu.paprika.data.db.DatabaseModule.provideDatabase(application)
    private val storyDao = db.storyDao()

    var stories by mutableStateOf<List<Story>>(emptyList())
        private set

    var isLoading by mutableStateOf(false)
        private set

    init {
        viewModelScope.launch {
            storyDao.getAllStories().collect { entities ->
                stories = entities.map { entity ->
                    Story(
                        id = entity.id,
                        user_id = entity.userId,
                        media_url = entity.mediaUrl,
                        media_type = entity.mediaType,
                        caption = entity.caption,
                        expires_at = entity.expiresAt
                    )
                }
            }
        }
        loadStories()
    }

    fun loadStories() {
        viewModelScope.launch {
            isLoading = true
            try {
                val response = NetworkModule.api.getStories()
                if (response.isSuccessful) {
                    val networkStories = response.body() ?: emptyList()
                    val entities = networkStories.map { 
                        ru.faustyu.paprika.data.db.StoryEntity(
                            id = it.id,
                            userId = it.user_id,
                            mediaUrl = it.media_url,
                            mediaType = it.media_type,
                            caption = it.caption,
                            expiresAt = it.expires_at
                        )
                    }
                    storyDao.clear()
                    storyDao.insertStories(entities)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    fun createStory(uriString: String, mediaType: String, caption: String) {
        viewModelScope.launch {
            try {
                var finalMediaUrl = uriString
                
                // If it's a local URI, we need to upload it first
                if (uriString.startsWith("content://") || uriString.startsWith("file://")) {
                    val uri = android.net.Uri.parse(uriString)
                    val inputStream = getApplication<android.app.Application>().contentResolver.openInputStream(uri)
                    val bytes = inputStream?.readBytes()
                    inputStream?.close()

                    if (bytes != null) {
                        val mimeType = if (mediaType == "video") "video/*" else "image/*"
                        val requestFile = okhttp3.RequestBody.create(mimeType.toMediaTypeOrNull(), bytes)
                        val body = okhttp3.MultipartBody.Part.createFormData("file", "story_${System.currentTimeMillis()}", requestFile)
                        
                        val uploadResponse = NetworkModule.api.uploadMedia(body)
                        if (uploadResponse.isSuccessful) {
                            finalMediaUrl = uploadResponse.body()?.get("url") ?: uriString
                        }
                    }
                }

                val request = CreateStoryRequest(finalMediaUrl, mediaType, caption)
                val response = NetworkModule.api.createStory(request)
                if (response.isSuccessful) {
                    loadStories() // Refresh
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
