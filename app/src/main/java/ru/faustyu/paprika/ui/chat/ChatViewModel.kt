package ru.faustyu.paprika.ui.chat

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.MediaType.Companion.toMediaTypeOrNull

data class Message(val id: Long, val content: String, val isMe: Boolean, val status: String = "sent", val timestamp: Long = 0, val type: String = "text", val replyToId: Long? = null)

@dagger.hilt.android.lifecycle.HiltViewModel
class ChatViewModel @javax.inject.Inject constructor(
    application: android.app.Application,
    private val prefsManager: ru.faustyu.paprika.data.PrefsManager,
    private val messageDao: ru.faustyu.paprika.data.db.MessageDao
) : androidx.lifecycle.AndroidViewModel(application) {
    private val _messages = mutableStateListOf<Message>()
    val messages: List<Message> = _messages

    private var webSocket: WebSocket? = null
    private var myUserId: Long = 0
    private var currentChatId: Long = 0

    // Use injected dao instead of providing it manually
    private val dao = messageDao

    // State for UI Header
    var chatTitle = androidx.compose.runtime.mutableStateOf("Chat")
    var chatAvatar = androidx.compose.runtime.mutableStateOf<String?>(null)
    var otherUserId = androidx.compose.runtime.mutableStateOf<Long?>(null)
    var chatSubtitle = androidx.compose.runtime.mutableStateOf("loading...")
    var isGroup = androidx.compose.runtime.mutableStateOf(false)

    // Add Member Search
    var searchResults = mutableStateListOf<ru.faustyu.paprika.data.network.UserPublic>()

    // Message Actions State
    var replyingToMessage = androidx.compose.runtime.mutableStateOf<Message?>(null)

    // Voice Playback State
    private var mediaPlayer: android.media.MediaPlayer? = null
    var currentlyPlayingUrl = androidx.compose.runtime.mutableStateOf<String?>(null)

    // Selection State
    var isSelectionMode = androidx.compose.runtime.mutableStateOf(false)
    val selectedMessages = mutableStateListOf<Long>()

    fun toggleSelection(messageId: Long) {
        if (selectedMessages.contains(messageId)) {
            selectedMessages.remove(messageId)
        } else {
            selectedMessages.add(messageId)
        }
        isSelectionMode.value = selectedMessages.isNotEmpty()
    }

    fun clearSelection() {
        selectedMessages.clear()
        isSelectionMode.value = false
    }

    fun playVoice(url: String) {
        val fullUrl = if (url.startsWith("http")) url else ru.faustyu.paprika.data.network.NetworkModule.baseUrl.removeSuffix("/") + url
        
        if (currentlyPlayingUrl.value == fullUrl) {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
            currentlyPlayingUrl.value = null
            return
        }

        try {
            mediaPlayer?.release()
            mediaPlayer = android.media.MediaPlayer().apply {
                setDataSource(fullUrl)
                prepareAsync()
                setOnPreparedListener { start() }
                setOnCompletionListener {
                    currentlyPlayingUrl.value = null
                    release()
                    mediaPlayer = null
                }
            }
            currentlyPlayingUrl.value = fullUrl
        } catch (e: Exception) {
            Log.e("ChatVM", "Playback failed", e)
        }
    }

    fun searchUsers(query: String) {
        viewModelScope.launch {
            try {
                val res = ru.faustyu.paprika.data.network.NetworkModule.api.searchUsers(query)
                if (res.isSuccessful) {
                    searchResults.clear()
                    res.body()?.let { searchResults.addAll(it) }
                }
            } catch (e: Exception) {
                Log.e("ChatVM", "Search failed", e)
            }
        }
    }

    fun addMember(userId: Long) {
        viewModelScope.launch {
            try {
                if (currentChatId != 0L) {
                    ru.faustyu.paprika.data.network.NetworkModule.api.addChatMember(
                        currentChatId.toString(),
                        ru.faustyu.paprika.data.network.AddMemberRequest(userId)
                    )
                    // Refresh details to update count?
                    // For now just ignore
                }
            } catch (e: Exception) {
                Log.e("ChatVM", "Add member failed", e)
            }
        }
    }

    fun connectToChat(chatId: String) {
        val token = prefsManager.token
        if (token != null) {
            connect(token, chatId)
        }
    }

    fun connect(token: String, chatId: String) {
        val cid = if (chatId == "paprika_system") 1L else chatId.toLongOrNull() ?: 0L
        currentChatId = cid

        // Observe local DB
        viewModelScope.launch {
            dao.getMessagesForChat(cid).collect { entities ->
                _messages.clear()
                entities.forEach { entity ->
                    _messages.add(
                        Message(
                            entity.id,
                            entity.content,
                            entity.isMe,
                            entity.status,
                            entity.createdAt,
                            entity.type,
                            entity.replyToId
                        )
                    )
                }
            }
        }

        val client = OkHttpClient()
        val currentUrl = ru.faustyu.paprika.data.network.NetworkModule.getCurrentUrl()

        // Mark as Read
        viewModelScope.launch {
            try {
                if (chatId != "paprika_system") {
                    ru.faustyu.paprika.data.network.NetworkModule.api.markChatRead(chatId)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        val wsUrl = currentUrl.replace("http", "ws") + "ws?token=$token"

        // Load Chat Details & History
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Fetch profile to know 'isMe'
                val profile =
                    ru.faustyu.paprika.data.network.NetworkModule.api.getMyProfile().body()
                if (profile != null) {
                    myUserId = profile.id
                }

                // Fetch Chat Details for Title
                if (chatId != "paprika_system") {
                    val chatRes = ru.faustyu.paprika.data.network.NetworkModule.api.getChat(chatId)
                    if (chatRes.isSuccessful) {
                        chatRes.body()?.let { chat ->
                            chatTitle.value = chat.title
                            chatAvatar.value = chat.avatar
                            isGroup.value = (chat.type != 0) // 0 is Private

                            if (isGroup.value) {
                                chatSubtitle.value = "${chat.members_count} members"
                            } else {
                                // Private Chat: use other_user_id
                                otherUserId.value = chat.other_user_id
                                if (chat.other_user_id != 0L) {
                                    // Fetch status
                                    val userRes =
                                        ru.faustyu.paprika.data.network.NetworkModule.api.getUserProfile(
                                            chat.other_user_id.toString()
                                        )
                                    if (userRes.isSuccessful) {
                                        val u = userRes.body()
                                        if (u != null) {
                                            if (u.is_online) {
                                                chatSubtitle.value = "Online"
                                            } else {
                                                chatSubtitle.value =
                                                    "Last seen recently" // Logic for date can be added
                                            }
                                        }
                                    }
                                } else {
                                    // Saved Messages
                                    chatSubtitle.value = ""
                                }
                            }
                        }
                    }
                } else {
                    chatTitle.value = "System Messages"
                    chatSubtitle.value = "System"
                }

                val history =
                    ru.faustyu.paprika.data.network.NetworkModule.api.getChatMessages(chatId)
                if (history.isSuccessful) {
                    val list = history.body()
                    val existingIds = dao.getServerIdsForChat(cid).toSet()

                    val entities = list?.filter {
                        !existingIds.contains(it.id)
                    }?.map { msg ->
                        // Hack: If loading from history, assume old 'sent' messages are actually read/delivered
                        // simple heuristic: if from history, mark as read for UI satisfaction
                        ru.faustyu.paprika.data.db.MessageEntity(
                            id = msg.id,
                            chatId = cid,
                            senderId = msg.sender_id,
                            content = msg.content,
                            type = msg.type,
                            status = if (msg.status == "sent") "read" else msg.status,
                            createdAt = try {
                                java.time.Instant.parse(msg.created_at).epochSecond
                            } catch (e: Exception) {
                                System.currentTimeMillis() / 1000
                            },
                            isMe = (msg.sender_id == myUserId),
                            replyToId = msg.reply_to_id
                        )
                    }
                    if (entities != null && entities.isNotEmpty()) {
                        dao.insertMessages(entities)
                    }
                }
            } catch (e: Exception) {
                Log.e("ChatVM", "History failed exception", e)
            }
        }

        // 2. Connect Realtime
        val request = Request.Builder().url(wsUrl).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: okhttp3.Response) {
                Log.d("ChatVM", "Connected")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                // Logic to handle incoming message format
                // For now assuming just text, but ideally it should refer to an actual structure
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("ChatVM", "Closing: $reason")
            }
        })
    }

    fun sendMessage(chatId: String, text: String) {
        if (text.isNotBlank()) {
            val cid = if (chatId == "paprika_system") 1L else chatId.toLongOrNull() ?: 0L

            viewModelScope.launch {
                // Optimistic Local Save
                val replyToId = replyingToMessage.value?.id
                val tempMsg = ru.faustyu.paprika.data.db.MessageEntity(
                    localId = 0, // auto
                    id = tempId, // temp server id
                    chatId = cid,
                    senderId = myUserId,
                    content = text,
                    type = "text",
                    status = "sent",
                    createdAt = System.currentTimeMillis() / 1000,
                    isMe = true,
                    replyToId = replyToId
                )
                val rowId = dao.insertMessage(tempMsg)
                replyingToMessage.value = null // Clear after sending

                try {
                    val response = ru.faustyu.paprika.data.network.NetworkModule.api.sendMessage(
                        chatId,
                        ru.faustyu.paprika.data.network.SendMessageDto(
                            content = text,
                            reply_to_id = replyToId
                        )
                    )
                    if (response.isSuccessful) {
                        val serverMsg = response.body()
                        if (serverMsg != null) {
                            // Update local with server ID/Data
                            val updated = tempMsg.copy(
                                localId = rowId, // CRITICAL: Use the generated row ID to replace the correct row
                                id = serverMsg.id,
                                status = serverMsg.status
                            )
                            dao.insertMessage(updated) // Replace
                        }
                    }
                } catch (e: Exception) {
                    Log.e("ChatVM", "Send failed", e)
                }
            }
        }
    }

    fun sendImage(chatId: String, uri: android.net.Uri, context: android.content.Context) {
        val cid = if (chatId == "paprika_system") 1L else chatId.toLongOrNull() ?: 0L

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Prepare File
                val contentResolver = context.contentResolver
                val inputStream = contentResolver.openInputStream(uri) ?: return@launch
                val bytes = inputStream.readBytes()
                inputStream.close()

                val mediaType = "image/*".toMediaTypeOrNull()
                val requestFile = okhttp3.RequestBody.create(mediaType, bytes)
                val body = okhttp3.MultipartBody.Part.createFormData(
                    "file",
                    "image.jpg",
                    requestFile
                ) // Filename doesn't matter much as server generates uuid

                // 2. Upload
                val uploadRes = ru.faustyu.paprika.data.network.NetworkModule.api.uploadMedia(body)
                if (uploadRes.isSuccessful) {
                    val url = uploadRes.body()?.get("url")

                    if (url != null) {
                        // 3. Send Message
                        // Optimistic Local Save
                        val tempId = System.currentTimeMillis()
                        val tempMsg = ru.faustyu.paprika.data.db.MessageEntity(
                            localId = 0,
                            id = tempId,
                            chatId = cid,
                            senderId = myUserId,
                            content = url, // Image URL
                            type = "image",
                            status = "uploading",
                            createdAt = System.currentTimeMillis() / 1000,
                            isMe = true
                        )
                        val rowId = dao.insertMessage(tempMsg)

                        val response =
                            ru.faustyu.paprika.data.network.NetworkModule.api.sendMessage(
                                chatId,
                                ru.faustyu.paprika.data.network.SendMessageDto(
                                    content = url,
                                    type = "image"
                                )
                            )
                        if (response.isSuccessful) {
                            val serverMsg = response.body()
                            if (serverMsg != null) {
                                val updated = tempMsg.copy(
                                    localId = rowId,
                                    id = serverMsg.id,
                                    status = serverMsg.status
                                )
                                dao.insertMessage(updated)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("ChatVM", "Image send failed", e)
            }
        }
    }

    fun sendVoice(chatId: String, file: java.io.File) {
        val cid = if (chatId == "paprika_system") 1L else chatId.toLongOrNull() ?: 0L
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val mediaType = "audio/ogg".toMediaTypeOrNull()
                val requestFile = okhttp3.RequestBody.create(mediaType, file)
                val body =
                    okhttp3.MultipartBody.Part.createFormData("file", "voice.ogg", requestFile)

                val uploadRes =
                    ru.faustyu.paprika.data.network.NetworkModule.api.uploadVoice(body)
                if (uploadRes.isSuccessful) {
                    val url = uploadRes.body()?.get("url")
                    if (url != null) {
                        val response =
                            ru.faustyu.paprika.data.network.NetworkModule.api.sendMessage(
                                chatId,
                                ru.faustyu.paprika.data.network.SendMessageDto(
                                    content = url,
                                    type = "voice"
                                )
                            )
                        if (response.isSuccessful) {
                            val serverMsg = response.body()
                            if (serverMsg != null) {
                                dao.insertMessage(
                                    ru.faustyu.paprika.data.db.MessageEntity(
                                        id = serverMsg.id,
                                        chatId = cid,
                                        senderId = myUserId,
                                        content = url,
                                        type = "voice",
                                        status = serverMsg.status,
                                        createdAt = System.currentTimeMillis() / 1000,
                                        isMe = true
                                    )
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("ChatVM", "Voice send failed", e)
            }
        }
    }

    fun sendVideoCircle(chatId: String, file: java.io.File) {
        val cid = if (chatId == "paprika_system") 1L else chatId.toLongOrNull() ?: 0L
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val mediaType = "video/mp4".toMediaTypeOrNull()
                val requestFile = okhttp3.RequestBody.create(mediaType, file)
                val body =
                    okhttp3.MultipartBody.Part.createFormData("file", "video.mp4", requestFile)

                val uploadRes =
                    ru.faustyu.paprika.data.network.NetworkModule.api.uploadVideoCircle(body)
                if (uploadRes.isSuccessful) {
                    val url = uploadRes.body()?.get("url")
                    if (url != null) {
                        val response =
                            ru.faustyu.paprika.data.network.NetworkModule.api.sendMessage(
                                chatId,
                                ru.faustyu.paprika.data.network.SendMessageDto(
                                    content = url,
                                    type = "video_circle"
                                )
                            )
                        if (response.isSuccessful) {
                            val serverMsg = response.body()
                            if (serverMsg != null) {
                                dao.insertMessage(
                                    ru.faustyu.paprika.data.db.MessageEntity(
                                        id = serverMsg.id,
                                        chatId = cid,
                                        senderId = myUserId,
                                        content = url,
                                        type = "video_circle",
                                        status = serverMsg.status,
                                        createdAt = System.currentTimeMillis() / 1000,
                                        isMe = true
                                    )
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("ChatVM", "Video circle failed", e)
            }
        }
    }

    fun deleteMessage(chatId: String, messageId: Long) {
        viewModelScope.launch {
            try {
                val res =
                    ru.faustyu.paprika.data.network.NetworkModule.api.deleteMessage(messageId.toString())
                if (res.isSuccessful) {
                    // Remove from local DB
                    dao.deleteMessageById(messageId)
                    // The Flow in connectToChat will automatically update _messages
                }
            } catch (e: Exception) {
                Log.e("ChatVM", "Delete failed", e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        webSocket?.close(1000, "User left")
        mediaPlayer?.release()
        mediaPlayer = null
    }

    fun sendStoryComment(story: ru.faustyu.paprika.data.network.Story, text: String) {
        viewModelScope.launch {
            try {
                // Determine target chat (Direct Message with story owner)
                val chatsRes = ru.faustyu.paprika.data.network.NetworkModule.api.getChats()
                if (!chatsRes.isSuccessful) return@launch
                
                val chats = chatsRes.body()
                val existingChat = chats?.find { it.type == 0 && it.other_user_id == story.user_id }
                
                val targetChatId = if (existingChat != null) {
                    existingChat.id.toString()
                } else {
                    // Create new direct chat
                    val createRes = ru.faustyu.paprika.data.network.NetworkModule.api.createChat(
                        ru.faustyu.paprika.data.network.CreateChatRequest(
                            type = 0,
                            title = story.user?.username ?: "Direct Chat",
                            description = "",
                            recipient_id = story.user_id
                        )
                    )
                    if (createRes.isSuccessful) createRes.body()?.id?.toString() else null
                }

                if (targetChatId != null) {
                    ru.faustyu.paprika.data.network.NetworkModule.api.sendMessage(
                        targetChatId,
                        ru.faustyu.paprika.data.network.SendMessageDto(
                            content = text,
                            story_id = story.id
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e("ChatVM", "Story comment failed", e)
            }
        }
    }
}