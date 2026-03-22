package ru.faustyu.paprika.ui.chat

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import ru.faustyu.paprika.data.network.ChatDto
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateMapOf
import ru.faustyu.paprika.data.network.AppWebSocketManager

class ChatListViewModel(application: android.app.Application) : androidx.lifecycle.AndroidViewModel(application) {
    var chats by mutableStateOf<List<ChatDto>>(emptyList())
        private set

    var currentUser by mutableStateOf<ru.faustyu.paprika.data.network.UserPublic?>(null)
        private set

    var isLoading by mutableStateOf(false)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    val typingInChat = mutableStateMapOf<Long, String>()

    init {
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
            try {
                val response = ru.faustyu.paprika.data.network.NetworkModule.api.getChats()
                if (response.isSuccessful) {
                    chats = response.body() ?: emptyList()
                    val totalUnread = chats.sumOf { it.unread_count }.toInt()
                    ru.faustyu.paprika.util.AppNotificationHelper.updateBadge(getApplication(), totalUnread)
                }

                val userResponse = ru.faustyu.paprika.data.network.NetworkModule.api.getMyProfile()
                if (userResponse.isSuccessful) {
                    currentUser = userResponse.body()
                }
            } catch (e: Exception) {
                error = "Нет соединения с сервером"
            } finally {
                isLoading = false
            }
        }
    }

    fun muteChat(chatId: String, muteUntil: Long) {
        viewModelScope.launch {
            try {
                ru.faustyu.paprika.data.network.NetworkModule.api.muteChat(chatId, ru.faustyu.paprika.data.network.MuteChatRequest(muteUntil))
            } catch (_: Exception) {}
        }
    }

    fun archiveChat(chatId: String) {
        viewModelScope.launch {
            try {
                ru.faustyu.paprika.data.network.NetworkModule.api.archiveChat(chatId)
                loadChats()
            } catch (_: Exception) {}
        }
    }

    fun pinChat(chatId: String) {
        viewModelScope.launch {
            try {
                ru.faustyu.paprika.data.network.NetworkModule.api.pinChat(chatId)
                loadChats()
            } catch (_: Exception) {}
        }
    }

    override fun onCleared() {
        super.onCleared()
        AppWebSocketManager.removeListener("chatlist_vm")
    }
}
