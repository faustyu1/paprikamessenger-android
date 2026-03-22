package ru.faustyu.paprika.ui.chat

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import ru.faustyu.paprika.data.network.AppWebSocketManager
import ru.faustyu.paprika.data.network.NetworkModule

data class Message(
    val content: String,
    val isMe: Boolean,
    val status: String = "sent",
    val timestamp: Long = 0,
    val type: String = "text"
)

class ChatViewModel(application: android.app.Application) : androidx.lifecycle.AndroidViewModel(application) {
    private val _messages = mutableStateListOf<Message>()
    val messages: List<Message> = _messages

    private var myUserId: Long = 0
    private var currentChatId: Long = 0

    private val db = ru.faustyu.paprika.data.db.DatabaseModule.provideDatabase(application)
    private val dao = db.messageDao()

    var chatTitle = androidx.compose.runtime.mutableStateOf("Chat")
    var chatAvatar = androidx.compose.runtime.mutableStateOf<String?>(null)
    var otherUserId = androidx.compose.runtime.mutableStateOf<Long?>(null)
    var chatSubtitle = androidx.compose.runtime.mutableStateOf("loading...")
    var isGroup = androidx.compose.runtime.mutableStateOf(false)
    var snackbarMessage = androidx.compose.runtime.mutableStateOf<String?>(null)

    var searchResults = mutableStateListOf<ru.faustyu.paprika.data.network.UserPublic>()

    fun searchUsers(query: String) {
        viewModelScope.launch {
            try {
                val res = NetworkModule.api.searchUsers(query)
                if (res.isSuccessful) {
                    searchResults.clear()
                    res.body()?.let { searchResults.addAll(it) }
                }
            } catch (e: Exception) {
                snackbarMessage.value = "Ошибка поиска"
            }
        }
    }

    fun addMember(userId: Long) {
        viewModelScope.launch {
            try {
                if (currentChatId != 0L) {
                    NetworkModule.api.addChatMember(
                        currentChatId.toString(),
                        ru.faustyu.paprika.data.network.AddMemberRequest(userId)
                    )
                }
            } catch (e: Exception) {
                Log.e("ChatVM", "Add member failed", e)
            }
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
                    _messages.add(Message(entity.content, entity.isMe, entity.status, entity.createdAt, entity.type))
                }
            }
        }

        // Mark as Read
        viewModelScope.launch {
            try {
                if (chatId != "paprika_system") {
                    NetworkModule.api.markChatRead(chatId)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Load Chat Details & History
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val profile = NetworkModule.api.getMyProfile().body()
                if (profile != null) myUserId = profile.id

                if (chatId != "paprika_system") {
                    val chatRes = NetworkModule.api.getChat(chatId)
                    if (chatRes.isSuccessful) {
                        chatRes.body()?.let { chat ->
                            chatTitle.value = chat.title
                            chatAvatar.value = chat.avatar
                            isGroup.value = (chat.type != 0)
                            if (isGroup.value) {
                                chatSubtitle.value = "${chat.members_count} members"
                            } else {
                                otherUserId.value = chat.other_user_id
                                if (chat.other_user_id != 0L) {
                                    val userRes = NetworkModule.api.getUserProfile(chat.other_user_id.toString())
                                    if (userRes.isSuccessful) {
                                        val u = userRes.body()
                                        chatSubtitle.value = if (u?.is_online == true) "Online" else "Last seen recently"
                                    }
                                } else {
                                    chatSubtitle.value = ""
                                }
                            }
                        }
                    }
                } else {
                    chatTitle.value = "System Messages"
                    chatSubtitle.value = "System"
                }

                val history = NetworkModule.api.getChatMessages(chatId)
                if (history.isSuccessful) {
                    val list = history.body()
                    val existingIds = dao.getServerIdsForChat(cid).toSet()
                    val entities = list?.filter { !existingIds.contains(it.id) }?.map { msg ->
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
                            isMe = (msg.sender_id == myUserId)
                        )
                    }
                    if (!entities.isNullOrEmpty()) dao.insertMessages(entities)
                }
            } catch (e: Exception) {
                snackbarMessage.value = "Не удалось загрузить сообщения"
            }
        }

        // Listen for new messages via AppWebSocketManager
        AppWebSocketManager.addListener("chat_vm_$cid") { event ->
            val type = event["event"] as? String ?: return@addListener
            if (type == "message:new") {
                val msgChatId = (event["chat_id"] as? Double)?.toLong() ?: return@addListener
                if (msgChatId != cid) return@addListener
                val content = event["content"] as? String ?: return@addListener
                val senderId = (event["sender_id"] as? Double)?.toLong() ?: return@addListener
                val msgId = (event["id"] as? Double)?.toLong() ?: System.currentTimeMillis()
                val createdAt = (event["created_at"] as? Double)?.toLong() ?: (System.currentTimeMillis() / 1000)
                val msgType = event["type"] as? String ?: "text"
                val entity = ru.faustyu.paprika.data.db.MessageEntity(
                    id = msgId,
                    chatId = cid,
                    senderId = senderId,
                    content = content,
                    type = msgType,
                    status = "delivered",
                    createdAt = createdAt,
                    isMe = (senderId == myUserId)
                )
                viewModelScope.launch { dao.insertMessage(entity) }
            }
        }
    }

    fun sendMessage(chatId: String, text: String) {
        if (text.isNotBlank()) {
            val cid = if (chatId == "paprika_system") 1L else chatId.toLongOrNull() ?: 0L
            viewModelScope.launch {
                val tempId = System.currentTimeMillis()
                val tempMsg = ru.faustyu.paprika.data.db.MessageEntity(
                    localId = 0,
                    id = tempId,
                    chatId = cid,
                    senderId = myUserId,
                    content = text,
                    type = "text",
                    status = "sent",
                    createdAt = System.currentTimeMillis() / 1000,
                    isMe = true
                )
                val rowId = dao.insertMessage(tempMsg)
                try {
                    val response = NetworkModule.api.sendMessage(
                        chatId,
                        ru.faustyu.paprika.data.network.SendMessageDto(content = text)
                    )
                    if (response.isSuccessful) {
                        response.body()?.let { serverMsg ->
                            dao.insertMessage(
                                tempMsg.copy(localId = rowId, id = serverMsg.id, status = serverMsg.status)
                            )
                        }
                    }
                } catch (e: Exception) {
                    snackbarMessage.value = "Не удалось отправить сообщение"
                }
            }
        }
    }

    fun sendImage(chatId: String, uri: android.net.Uri, context: android.content.Context) {
        val cid = if (chatId == "paprika_system") 1L else chatId.toLongOrNull() ?: 0L
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri) ?: return@launch
                val bytes = inputStream.readBytes()
                inputStream.close()
                val requestFile = okhttp3.RequestBody.create("image/*".toMediaTypeOrNull(), bytes)
                val body = okhttp3.MultipartBody.Part.createFormData("file", "image.jpg", requestFile)
                val uploadRes = NetworkModule.api.uploadMedia(body)
                if (uploadRes.isSuccessful) {
                    val url = uploadRes.body()?.get("url") ?: return@launch
                    val tempId = System.currentTimeMillis()
                    val tempMsg = ru.faustyu.paprika.data.db.MessageEntity(
                        localId = 0, id = tempId, chatId = cid, senderId = myUserId,
                        content = url, type = "image", status = "uploading",
                        createdAt = System.currentTimeMillis() / 1000, isMe = true
                    )
                    val rowId = dao.insertMessage(tempMsg)
                    val response = NetworkModule.api.sendMessage(
                        chatId,
                        ru.faustyu.paprika.data.network.SendMessageDto(content = url, type = "image")
                    )
                    if (response.isSuccessful) {
                        response.body()?.let { serverMsg ->
                            dao.insertMessage(
                                tempMsg.copy(localId = rowId, id = serverMsg.id, status = serverMsg.status)
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                snackbarMessage.value = "Не удалось отправить фото"
            }
        }
    }

    fun sendVoice(chatId: String, filePath: String, context: android.content.Context) {
        val cid = if (chatId == "paprika_system") 1L else chatId.toLongOrNull() ?: 0L
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = java.io.File(filePath)
                val requestFile = okhttp3.RequestBody.create("audio/m4a".toMediaTypeOrNull(), file)
                val body = okhttp3.MultipartBody.Part.createFormData("file", file.name, requestFile)
                val uploadRes = NetworkModule.api.uploadMedia(body)
                if (uploadRes.isSuccessful) {
                    val url = uploadRes.body()?.get("url") ?: return@launch
                    val tempId = System.currentTimeMillis()
                    val tempMsg = ru.faustyu.paprika.data.db.MessageEntity(
                        id = tempId, chatId = cid, senderId = myUserId,
                        content = url, type = "voice", status = "uploading",
                        createdAt = System.currentTimeMillis() / 1000, isMe = true
                    )
                    val rowId = dao.insertMessage(tempMsg)
                    val response = NetworkModule.api.sendMessage(
                        chatId, ru.faustyu.paprika.data.network.SendMessageDto(content = url, type = "voice")
                    )
                    if (response.isSuccessful) {
                        response.body()?.let {
                            dao.insertMessage(tempMsg.copy(localId = rowId, id = it.id, status = it.status))
                        }
                    }
                    file.delete()
                }
            } catch (e: Exception) {
                snackbarMessage.value = "Не удалось отправить голосовое"
            }
        }
    }

    fun sendVideoCircle(chatId: String, filePath: String) {
        val cid = if (chatId == "paprika_system") 1L else chatId.toLongOrNull() ?: 0L
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = java.io.File(filePath)
                val requestFile = okhttp3.RequestBody.create("video/mp4".toMediaTypeOrNull(), file)
                val body = okhttp3.MultipartBody.Part.createFormData("file", file.name, requestFile)
                val uploadRes = NetworkModule.api.uploadMedia(body)
                if (uploadRes.isSuccessful) {
                    val url = uploadRes.body()?.get("url") ?: return@launch
                    val tempId = System.currentTimeMillis()
                    val tempMsg = ru.faustyu.paprika.data.db.MessageEntity(
                        id = tempId, chatId = cid, senderId = myUserId,
                        content = url, type = "video_circle", status = "uploading",
                        createdAt = System.currentTimeMillis() / 1000, isMe = true
                    )
                    val rowId = dao.insertMessage(tempMsg)
                    val response = NetworkModule.api.sendMessage(
                        chatId, ru.faustyu.paprika.data.network.SendMessageDto(content = url, type = "video_circle")
                    )
                    if (response.isSuccessful) {
                        response.body()?.let {
                            dao.insertMessage(tempMsg.copy(localId = rowId, id = it.id, status = it.status))
                        }
                    }
                    file.delete()
                }
            } catch (e: Exception) {
                snackbarMessage.value = "Не удалось отправить видео"
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        AppWebSocketManager.removeListener("chat_vm_$currentChatId")
    }
}
