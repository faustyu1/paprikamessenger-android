package ru.faustyu.paprika.ui.chat

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ru.faustyu.paprika.data.db.ChatEntity
import ru.faustyu.paprika.data.db.DatabaseModule
import ru.faustyu.paprika.data.network.AppWebSocketManager
import ru.faustyu.paprika.data.network.ChatDto
import ru.faustyu.paprika.data.network.NetworkModule
import ru.faustyu.paprika.data.network.MuteChatRequest
import ru.faustyu.paprika.data.network.UserPublic
import ru.faustyu.paprika.util.AppNotificationHelper

class ChatListViewModel(application: android.app.Application) : androidx.lifecycle.AndroidViewModel(application) {

    private val chatDao = DatabaseModule.provideDatabase(application).chatDao()

    var chats by mutableStateOf<List<ChatDto>>(emptyList())
        private set

    var currentUser by mutableStateOf<UserPublic?>(null)
        private set

    var isLoading by mutableStateOf(false)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    val typingInChat = mutableStateMapOf<Long, String>()

    init {
        // Load cached chats from Room immediately (instant display)
        viewModelScope.launch {
            val cached = chatDao.getAllChats().first()
            if (cached.isNotEmpty()) {
                chats = cached.map { it.toDto() }
            }
        }

        loadChats()

        AppWebSocketManager.addListener("chatlist_vm") { event ->
            val type = event["event"] as? String ?: return@addListener
            if (type == "user:typing") {
                val chatId = (event["chat_id"] as? Double)?.toLong() ?: return@addListener
                val username = event["username"] as? String ?: return@addListener
                typingInChat[chatId] = username
                viewModelScope.launch {
                    kotlinx.coroutines.delay(3000)
                    typingInChat.remove(chatId)
                }
            }
        }
    }

    fun loadChats() {
        viewModelScope.launch {
            isLoading = true
            error = null
            try {
                val response = NetworkModule.api.getChats()
                if (response.isSuccessful) {
                    val fresh = response.body() ?: emptyList()
                    chats = fresh
                    // Persist to Room cache
                    chatDao.clearAll()
                    chatDao.insertChats(fresh.map { it.toEntity() })
                    AppNotificationHelper.updateBadge(getApplication(), fresh.sumOf { it.unread_count }.toInt())
                }

                val userResp = NetworkModule.api.getMyProfile()
                if (userResp.isSuccessful) currentUser = userResp.body()

            } catch (e: Exception) {
                // Show error only if we have no cached data
                if (chats.isEmpty()) error = "Нет соединения с сервером"
            } finally {
                isLoading = false
            }
        }
    }

    fun muteChat(chatId: String, muteUntil: Long) {
        viewModelScope.launch {
            try { NetworkModule.api.muteChat(chatId, MuteChatRequest(muteUntil)) } catch (_: Exception) {}
        }
    }

    fun archiveChat(chatId: String) {
        viewModelScope.launch {
            try { NetworkModule.api.archiveChat(chatId); loadChats() } catch (_: Exception) {}
        }
    }

    fun pinChat(chatId: String) {
        viewModelScope.launch {
            try { NetworkModule.api.pinChat(chatId); loadChats() } catch (_: Exception) {}
        }
    }

    override fun onCleared() {
        super.onCleared()
        AppWebSocketManager.removeListener("chatlist_vm")
    }
}

private fun ChatEntity.toDto() = ChatDto(
    id = id,
    type = type,
    title = title,
    description = "",
    avatar = avatar,
    owner_id = 0,
    last_message_preview = lastMessagePreview,
    last_message_at = lastMessageAt,
    unread_count = unreadCount,
    mute_until = muteUntil,
    is_archived = isArchived,
    is_pinned = isPinned
)

private fun ChatDto.toEntity() = ChatEntity(
    id = id,
    type = type,
    title = title,
    avatar = avatar,
    lastMessagePreview = last_message_preview,
    lastMessageAt = last_message_at,
    unreadCount = unread_count,
    muteUntil = mute_until,
    isArchived = is_archived,
    isPinned = is_pinned
)
