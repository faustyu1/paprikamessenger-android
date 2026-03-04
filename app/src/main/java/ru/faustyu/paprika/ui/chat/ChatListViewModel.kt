package ru.faustyu.paprika.ui.chat

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import ru.faustyu.paprika.data.network.ChatDto
import ru.faustyu.paprika.data.network.UserPublic
import ru.faustyu.paprika.data.repository.ChatRepository
import ru.faustyu.paprika.data.repository.UserRepository
import ru.faustyu.paprika.ui.base.BaseViewModel
import ru.faustyu.paprika.util.Result
import javax.inject.Inject

@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository
) : BaseViewModel() {
    
    var chats by mutableStateOf<List<ChatDto>>(emptyList())
        private set
    
    var currentUser by mutableStateOf<UserPublic?>(null)
        private set

    init {
        loadChats()
    }

    fun loadChats() {
        viewModelScope.launch {
            showLoading()
            
            // Fetch chats
            when (val result = chatRepository.getChats()) {
                is Result.Success -> {
                    chats = result.data
                }
                is Result.Error -> {
                    showError(result.message ?: "Failed to load chats")
                }
                is Result.Loading -> {}
            }
            
            // Fetch current user
            when (val result = userRepository.getMyProfile()) {
                is Result.Success -> {
                    currentUser = result.data
                    hideLoading()
                }
                is Result.Error -> {
                    showError(result.message ?: "Failed to load profile")
                }
                is Result.Loading -> {}
            }
        }
    }
}
