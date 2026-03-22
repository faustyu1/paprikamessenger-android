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

data class ReactionGroup(val emoji: String, val count: Int, val isMine: Boolean = false)

data class PollOptionUi(val id: Long, val text: String, val votesCount: Int, val isVotedByMe: Boolean)

data class Message(
    val id: Long = 0,
    val content: String,
    val isMe: Boolean,
    val status: String = "sent",
    val timestamp: Long = 0,
    val type: String = "text",
    val edited: Boolean = false,
    val replyToContent: String? = null,
    val replyToType: String? = null,
    val replyToSenderId: Long? = null,
    val forwardedFromName: String? = null,
    val reactions: List<ReactionGroup> = emptyList(),
    val pollId: Long? = null,
    val pollQuestion: String? = null,
    val pollOptions: List<PollOptionUi> = emptyList()
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
    var replyToMessage = androidx.compose.runtime.mutableStateOf<Message?>(null)
    var typingText = androidx.compose.runtime.mutableStateOf("")
    var pinnedMessage = androidx.compose.runtime.mutableStateOf<Message?>(null)
    var isOwner = androidx.compose.runtime.mutableStateOf(false)
    var isAdmin = androidx.compose.runtime.mutableStateOf(false)
    var isChannel = androidx.compose.runtime.mutableStateOf(false)
    var disappearingTimerSec = androidx.compose.runtime.mutableStateOf(0L)
    var initialUnreadCount = androidx.compose.runtime.mutableStateOf(0L)

    var searchResults = mutableStateListOf<ru.faustyu.paprika.data.network.UserPublic>()
    val forwardChats = mutableStateListOf<ru.faustyu.paprika.data.network.ChatDto>()

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
        chatTitle.value = ""
        chatSubtitle.value = ""
        chatAvatar.value = null
        isGroup.value = false
        otherUserId.value = null

        // Observe local DB
        viewModelScope.launch {
            dao.getMessagesForChat(cid).collect { entities ->
                _messages.clear()
                entities.forEach { entity ->
                    _messages.add(Message(id = entity.id, content = entity.content, isMe = entity.isMe, status = entity.status, timestamp = entity.createdAt, type = entity.type))
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

        // Load chats for forwarding
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val chatsRes = NetworkModule.api.getChats()
                if (chatsRes.isSuccessful) {
                    chatsRes.body()?.let { list ->
                        forwardChats.clear()
                        forwardChats.addAll(list)
                    }
                }
            } catch (_: Exception) {}
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
                            isChannel.value = (chat.type == 2)
                            isOwner.value = (myUserId == chat.owner_id)
                            initialUnreadCount.value = chat.unread_count
                            if (isGroup.value) {
                                chatSubtitle.value = "${chat.members_count} участников"
                                try {
                                    val membersRes = NetworkModule.api.getChatMembers(chatId)
                                    if (membersRes.isSuccessful) {
                                        val myMember = membersRes.body()?.find { it.user_id == myUserId }
                                        isAdmin.value = myMember?.role == "admin" || isOwner.value
                                    }
                                } catch (_: Exception) {}
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

                // Load pinned message
                if (chatId != "paprika_system") {
                    try {
                        val pinRes = NetworkModule.api.getPinnedMessage(chatId)
                        if (pinRes.isSuccessful) {
                            pinRes.body()?.let { dto ->
                                pinnedMessage.value = Message(
                                    id = dto.id,
                                    content = dto.content,
                                    isMe = (dto.sender_id == myUserId),
                                    type = dto.type,
                                    timestamp = try { java.time.Instant.parse(dto.created_at).epochSecond } catch (_: Exception) { 0L }
                                )
                            }
                        }
                    } catch (_: Exception) {}
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

                    // Load rich data (reactions, replies) into in-memory messages
                    list?.forEach { dto ->
                        val idx = _messages.indexOfFirst { it.id == dto.id }
                        if (idx >= 0) {
                            _messages[idx] = _messages[idx].copy(
                                edited = dto.edited_at != null,
                                replyToContent = dto.reply_to_message?.content,
                                replyToType = dto.reply_to_message?.type,
                                replyToSenderId = dto.reply_to_message?.sender_id,
                                forwardedFromName = dto.forwarded_from_user?.let { u ->
                                    if (!u.first_name.isNullOrBlank()) "${u.first_name} ${u.last_name ?: ""}".trim()
                                    else u.username
                                },
                                reactions = dto.reactions.map { r -> ReactionGroup(r.emoji, r.count.toInt(), r.is_mine) },
                                pollId = dto.poll?.id,
                                pollQuestion = dto.poll?.question,
                                pollOptions = dto.poll?.options?.map { o ->
                                    PollOptionUi(o.id, o.text, o.votes_count.toInt(), o.is_voted_by_me)
                                } ?: emptyList()
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                snackbarMessage.value = "Не удалось загрузить сообщения"
            }
        }

        // Listen for events via AppWebSocketManager
        AppWebSocketManager.addListener("chat_vm_$cid") { event ->
            val type = event["event"] as? String ?: return@addListener

            when (type) {
                "message:new" -> {
                    val msgChatId = (event["chat_id"] as? Double)?.toLong() ?: return@addListener
                    if (msgChatId != cid) return@addListener
                    val content = event["content"] as? String ?: return@addListener
                    val senderId = (event["sender_id"] as? Double)?.toLong() ?: return@addListener
                    val msgId = (event["id"] as? Double)?.toLong() ?: System.currentTimeMillis()
                    val createdAt = (event["created_at"] as? Double)?.toLong() ?: (System.currentTimeMillis() / 1000)
                    val msgType = event["type"] as? String ?: "text"
                    val entity = ru.faustyu.paprika.data.db.MessageEntity(
                        id = msgId, chatId = cid, senderId = senderId, content = content,
                        type = msgType, status = "delivered", createdAt = createdAt,
                        isMe = (senderId == myUserId)
                    )
                    viewModelScope.launch { dao.insertMessage(entity) }
                }

                "message:deleted" -> {
                    val msgChatId = (event["chat_id"] as? Double)?.toLong() ?: return@addListener
                    if (msgChatId != cid) return@addListener
                    val msgId = (event["message_id"] as? Double)?.toLong() ?: return@addListener
                    viewModelScope.launch {
                        val idx = _messages.indexOfFirst { it.id == msgId }
                        if (idx >= 0) _messages.removeAt(idx)
                        dao.deleteMessage(msgId)
                    }
                }

                "user:typing" -> {
                    val msgChatId = (event["chat_id"] as? Double)?.toLong() ?: return@addListener
                    if (msgChatId != cid) return@addListener
                    viewModelScope.launch {
                        typingText.value = "печатает..."
                        kotlinx.coroutines.delay(3000)
                        typingText.value = ""
                    }
                }

                "message:reaction" -> {
                    val msgChatId = (event["chat_id"] as? Double)?.toLong() ?: return@addListener
                    if (msgChatId != cid) return@addListener
                    val msgId = (event["message_id"] as? Double)?.toLong() ?: return@addListener
                    @Suppress("UNCHECKED_CAST")
                    val rawReactions = event["reactions"] as? List<Map<String, Any>> ?: return@addListener
                    val reactions = rawReactions.map { r ->
                        ReactionGroup(
                            emoji = r["emoji"] as? String ?: "",
                            count = ((r["count"] as? Double)?.toInt() ?: 0)
                        )
                    }
                    viewModelScope.launch {
                        val idx = _messages.indexOfFirst { it.id == msgId }
                        if (idx >= 0) _messages[idx] = _messages[idx].copy(reactions = reactions)
                    }
                }

                "message:pinned" -> {
                    val msgChatId = (event["chat_id"] as? Double)?.toLong() ?: return@addListener
                    if (msgChatId != cid) return@addListener
                    val msgId = (event["message_id"] as? Double)?.toLong() ?: return@addListener
                    val content = event["message_content"] as? String ?: ""
                    val msgType = event["message_type"] as? String ?: "text"
                    viewModelScope.launch {
                        pinnedMessage.value = Message(id = msgId, content = content, isMe = false, type = msgType)
                    }
                }

                "message:unpinned" -> {
                    val msgChatId = (event["chat_id"] as? Double)?.toLong() ?: return@addListener
                    if (msgChatId != cid) return@addListener
                    viewModelScope.launch { pinnedMessage.value = null }
                }
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
                    val replyId = replyToMessage.value?.id
                    replyToMessage.value = null
                    val timerSec = disappearingTimerSec.value.takeIf { it > 0 }
                    val response = NetworkModule.api.sendMessage(
                        chatId,
                        ru.faustyu.paprika.data.network.SendMessageDto(
                            content = text,
                            reply_to_message_id = replyId,
                            expires_after_sec = timerSec
                        )
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

    fun sendFile(chatId: String, uri: android.net.Uri, context: android.content.Context) {
        val cid = if (chatId == "paprika_system") 1L else chatId.toLongOrNull() ?: 0L
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val cursor = context.contentResolver.query(uri, null, null, null, null)
                val nameIndex = cursor?.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                cursor?.moveToFirst()
                val fileName = cursor?.getString(nameIndex ?: 0) ?: "file"
                cursor?.close()
                val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
                val inputStream = context.contentResolver.openInputStream(uri) ?: return@launch
                val bytes = inputStream.readBytes()
                inputStream.close()
                val requestBody = okhttp3.RequestBody.create(mimeType.toMediaTypeOrNull(), bytes)
                val body = okhttp3.MultipartBody.Part.createFormData("file", fileName, requestBody)
                val uploadRes = NetworkModule.api.uploadMedia(body)
                if (uploadRes.isSuccessful) {
                    val url = uploadRes.body()?.get("url") ?: return@launch
                    val tempId = System.currentTimeMillis()
                    val tempMsg = ru.faustyu.paprika.data.db.MessageEntity(
                        localId = 0, id = tempId, chatId = cid, senderId = myUserId,
                        content = url, type = "file", status = "uploading",
                        createdAt = System.currentTimeMillis() / 1000, isMe = true
                    )
                    val rowId = dao.insertMessage(tempMsg)
                    val response = NetworkModule.api.sendMessage(
                        chatId, ru.faustyu.paprika.data.network.SendMessageDto(content = url, type = "file")
                    )
                    if (response.isSuccessful) {
                        response.body()?.let {
                            dao.insertMessage(tempMsg.copy(localId = rowId, id = it.id, status = it.status))
                        }
                    }
                }
            } catch (e: Exception) {
                snackbarMessage.value = "Не удалось отправить файл"
            }
        }
    }

    fun setReply(message: Message) { replyToMessage.value = message }
    fun clearReply() { replyToMessage.value = null }

    fun sendTyping(chatId: String) {
        val cid = if (chatId == "paprika_system") 1L else chatId.toLongOrNull() ?: 0L
        AppWebSocketManager.send(mapOf("event" to "user:typing", "chat_id" to cid))
    }

    fun deleteMessage(msgId: Long) {
        viewModelScope.launch {
            try {
                NetworkModule.api.deleteMessage(msgId.toString())
                val idx = _messages.indexOfFirst { it.id == msgId }
                if (idx >= 0) _messages.removeAt(idx)
                dao.deleteMessage(msgId)
            } catch (e: Exception) {
                snackbarMessage.value = "Не удалось удалить сообщение"
            }
        }
    }

    fun editMessage(msgId: Long, newContent: String) {
        viewModelScope.launch {
            try {
                NetworkModule.api.editMessage(msgId.toString(), ru.faustyu.paprika.data.network.EditMessageRequest(newContent))
                val idx = _messages.indexOfFirst { it.id == msgId }
                if (idx >= 0) _messages[idx] = _messages[idx].copy(content = newContent, edited = true)
            } catch (e: Exception) {
                snackbarMessage.value = "Не удалось редактировать сообщение"
            }
        }
    }

    fun reactToMessage(msgId: Long, emoji: String) {
        viewModelScope.launch {
            try {
                val idx = _messages.indexOfFirst { it.id == msgId }
                if (idx < 0) return@launch
                val existing = _messages[idx].reactions.find { it.emoji == emoji }
                if (existing?.isMine == true) {
                    NetworkModule.api.removeReaction(msgId, emoji)
                } else {
                    NetworkModule.api.addReaction(msgId, ru.faustyu.paprika.data.network.AddReactionRequest(emoji))
                }
                // Optimistic update
                val reactions = _messages[idx].reactions.toMutableList()
                val eIdx = reactions.indexOfFirst { it.emoji == emoji }
                if (eIdx >= 0) {
                    val r = reactions[eIdx]
                    if (r.isMine) reactions[eIdx] = r.copy(count = (r.count - 1).coerceAtLeast(0), isMine = false)
                    else reactions[eIdx] = r.copy(count = r.count + 1, isMine = true)
                } else {
                    reactions.add(ReactionGroup(emoji, 1, true))
                }
                _messages[idx] = _messages[idx].copy(reactions = reactions.filter { it.count > 0 })
            } catch (e: Exception) {
                snackbarMessage.value = "Ошибка реакции"
            }
        }
    }

    fun forwardMessage(msgId: Long, toChatId: Long) {
        viewModelScope.launch {
            try {
                NetworkModule.api.forwardMessage(msgId.toString(), ru.faustyu.paprika.data.network.ForwardMessageRequest(toChatId))
                snackbarMessage.value = "Сообщение переслано"
            } catch (e: Exception) {
                snackbarMessage.value = "Не удалось переслать"
            }
        }
    }

    fun pinMessage(chatId: String, msgId: Long) {
        viewModelScope.launch {
            try {
                NetworkModule.api.pinMessage(chatId, ru.faustyu.paprika.data.network.PinMessageRequest(msgId))
                val msg = _messages.find { it.id == msgId }
                pinnedMessage.value = msg
            } catch (e: Exception) {
                snackbarMessage.value = "Не удалось закрепить сообщение"
            }
        }
    }

    fun unpinMessage(chatId: String) {
        viewModelScope.launch {
            try {
                NetworkModule.api.unpinMessage(chatId)
                pinnedMessage.value = null
            } catch (e: Exception) {
                snackbarMessage.value = "Не удалось открепить сообщение"
            }
        }
    }

    fun sendPoll(chatId: String, question: String, options: List<String>, isMultiple: Boolean = false) {
        if (question.isBlank() || options.size < 2) return
        viewModelScope.launch {
            try {
                val optionsJson = options.joinToString(",") { "\"${it.replace("\"", "\\\"")}\"" }
                val content = """{"question":"${question.replace("\"","\\\"")}","options":[$optionsJson],"is_multiple":$isMultiple}"""
                NetworkModule.api.sendMessage(
                    chatId,
                    ru.faustyu.paprika.data.network.SendMessageDto(content = content, type = "poll")
                )
            } catch (e: Exception) {
                snackbarMessage.value = "Не удалось создать опрос"
            }
        }
    }

    fun votePoll(pollId: Long, optionId: Long, msgId: Long) {
        viewModelScope.launch {
            try {
                val resp = NetworkModule.api.votePoll(pollId, ru.faustyu.paprika.data.network.PollVoteRequest(optionId))
                if (resp.isSuccessful) {
                    val poll = resp.body() ?: return@launch
                    val idx = _messages.indexOfFirst { it.id == msgId }
                    if (idx >= 0) {
                        _messages[idx] = _messages[idx].copy(
                            pollOptions = poll.options.map { o ->
                                PollOptionUi(o.id, o.text, o.votes_count.toInt(), o.is_voted_by_me)
                            }
                        )
                    }
                }
            } catch (e: Exception) {
                snackbarMessage.value = "Ошибка голосования"
            }
        }
    }

    fun exportChat(chatId: String, context: android.content.Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val resp = NetworkModule.api.getChatMessages(chatId)
                if (resp.isSuccessful) {
                    val messages = resp.body() ?: return@launch
                    val sb = StringBuilder()
                    sb.appendLine("Экспорт чата")
                    sb.appendLine("=============")
                    messages.reversed().forEach { msg ->
                        val time = try {
                            val inst = java.time.Instant.parse(msg.created_at)
                            java.time.format.DateTimeFormatter
                                .ofPattern("dd.MM.yyyy HH:mm")
                                .withZone(java.time.ZoneId.systemDefault())
                                .format(inst)
                        } catch (_: Exception) { msg.created_at }
                        sb.appendLine("[$time] ${msg.sender_id}: ${msg.content}")
                    }
                    val fileName = "chat_${chatId}_export.txt"
                    val resolver = context.contentResolver
                    val values = android.content.ContentValues().apply {
                        put(android.provider.MediaStore.Downloads.DISPLAY_NAME, fileName)
                        put(android.provider.MediaStore.Downloads.MIME_TYPE, "text/plain")
                    }
                    val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    uri?.let {
                        resolver.openOutputStream(it)?.use { os -> os.write(sb.toString().toByteArray()) }
                    }
                    viewModelScope.launch { snackbarMessage.value = "История чата сохранена в загрузки" }
                }
            } catch (e: Exception) {
                viewModelScope.launch { snackbarMessage.value = "Ошибка экспорта" }
            }
        }
    }

    fun sendGif(chatId: String, gifUrl: String) {
        val cid = if (chatId == "paprika_system") 1L else chatId.toLongOrNull() ?: 0L
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val tempId = System.currentTimeMillis()
                val tempMsg = ru.faustyu.paprika.data.db.MessageEntity(
                    localId = 0, id = tempId, chatId = cid, senderId = myUserId,
                    content = gifUrl, type = "gif", status = "sending",
                    createdAt = System.currentTimeMillis() / 1000, isMe = true
                )
                val rowId = dao.insertMessage(tempMsg)
                val response = NetworkModule.api.sendMessage(
                    chatId,
                    ru.faustyu.paprika.data.network.SendMessageDto(content = gifUrl, type = "gif")
                )
                if (response.isSuccessful) {
                    response.body()?.let { serverMsg ->
                        dao.insertMessage(
                            tempMsg.copy(localId = rowId, id = serverMsg.id, status = serverMsg.status)
                        )
                    }
                }
            } catch (e: Exception) {
                snackbarMessage.value = "Не удалось отправить GIF"
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        AppWebSocketManager.removeListener("chat_vm_$currentChatId")
    }
}
