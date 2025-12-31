package ru.faustyu.paprika.ui.chat

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import ru.faustyu.paprika.data.network.ChatDto
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

class ChatListViewModel : ViewModel() {
    var chats by mutableStateOf<List<ChatDto>>(emptyList())
        private set
    
    var currentUser by mutableStateOf<ru.faustyu.paprika.data.network.UserPublic?>(null)
        private set

    var isLoading by mutableStateOf(false)
        private set

    init {
        loadChats()
    }

    fun loadChats() {
        viewModelScope.launch {
            isLoading = true
            try {
                val response = ru.faustyu.paprika.data.network.NetworkModule.api.getChats()
                if (response.isSuccessful) {
                    chats = response.body() ?: emptyList()
                }
                
                // Fetch current user for header
                val userResponse = ru.faustyu.paprika.data.network.NetworkModule.api.getMyProfile()
                if (userResponse.isSuccessful) {
                    currentUser = userResponse.body()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }
}
