package ru.faustyu.paprika.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.faustyu.paprika.data.network.ApiService
import ru.faustyu.paprika.data.network.ChatDto
import ru.faustyu.paprika.data.network.CreateChatRequest
import ru.faustyu.paprika.data.network.AddMemberRequest
import ru.faustyu.paprika.util.Constants
import ru.faustyu.paprika.util.Result
import ru.faustyu.paprika.util.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for chat operations
 */
@Singleton
class ChatRepository @Inject constructor(
    private val apiService: ApiService
) {
    
    /**
     * Get all chats
     */
    suspend fun getChats(): Result<List<ChatDto>> {
        return safeApiCall {
            val response = apiService.getChats()
            if (response.isSuccessful && response.body() != null) {
                response.body()!!
            } else {
                throw Exception("Failed to fetch chats")
            }
        }
    }
    
    /**
     * Get chat by ID
     */
    suspend fun getChat(chatId: String): Result<ChatDto> {
        return safeApiCall {
            val response = apiService.getChat(chatId)
            if (response.isSuccessful && response.body() != null) {
                response.body()!!
            } else {
                throw Exception("Failed to fetch chat")
            }
        }
    }
    
    /**
     * Create private chat
     */
    suspend fun createPrivateChat(recipientId: Long): Result<ChatDto> {
        return safeApiCall {
            val request = CreateChatRequest(
                type = Constants.CHAT_TYPE_PRIVATE,
                title = "",
                description = "",
                recipient_id = recipientId
            )
            val response = apiService.createChat(request)
            if (response.isSuccessful && response.body() != null) {
                response.body()!!.let { chat ->
                    ChatDto(
                        id = chat.id,
                        type = chat.type,
                        title = chat.title,
                        description = chat.description ?: "",
                        avatar = "",
                        owner_id = 0,
                        last_message_preview = null,
                        last_message_at = 0,
                        members_count = 0,
                        online_count = 0,
                        other_user_id = recipientId,
                        unread_count = 0
                    )
                }
            } else {
                throw Exception("Failed to create chat")
            }
        }
    }
    
    /**
     * Create group chat
     */
    suspend fun createGroupChat(title: String, description: String): Result<ChatDto> {
        return safeApiCall {
            val request = CreateChatRequest(
                type = Constants.CHAT_TYPE_GROUP,
                title = title,
                description = description
            )
            val response = apiService.createChat(request)
            if (response.isSuccessful && response.body() != null) {
                response.body()!!.let { chat ->
                    ChatDto(
                        id = chat.id,
                        type = chat.type,
                        title = chat.title,
                        description = chat.description ?: "",
                        avatar = "",
                        owner_id = 0,
                        last_message_preview = null,
                        last_message_at = 0,
                        members_count = 0,
                        online_count = 0,
                        other_user_id = 0,
                        unread_count = 0
                    )
                }
            } else {
                throw Exception("Failed to create group")
            }
        }
    }
    
    /**
     * Add member to chat
     */
    suspend fun addMember(chatId: String, userId: Long): Result<Unit> {
        return safeApiCall {
            val request = AddMemberRequest(user_id = userId)
            apiService.addChatMember(chatId, request)
        }
    }
}
