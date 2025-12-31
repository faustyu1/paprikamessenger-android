package ru.faustyu.paprika.data.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

data class AuthRequest(
    val username: String,
    val password: String,
    // Optional public key for registration
    val public_key: String? = null,
    val first_name: String? = null,
    val last_name: String? = null
)

data class AuthResponse(
    val token: String,
    val error: String?
)

interface ApiService {
    @POST("/register")
    suspend fun register(@Body request: AuthRequest): Response<AuthResponse>

    @POST("/login")
    suspend fun login(@Body request: AuthRequest): Response<AuthResponse>

    @GET("/users/search")
    suspend fun searchUsers(@Query("q") query: String): Response<List<UserPublic>>



    @POST("/chats")
    suspend fun createChat(@Body request: CreateChatRequest): Response<Chat>

    @GET("/stories")
    suspend fun getStories(): Response<List<Story>>

    @POST("/stories")
    suspend fun createStory(@Body request: CreateStoryRequest): Response<Story>

    // Profile Management
    @GET("/me")
    suspend fun getMyProfile(): Response<UserPublic>

    @POST("/me/avatar")
    @retrofit2.http.Multipart
    suspend fun uploadAvatar(@retrofit2.http.Part avatar: okhttp3.MultipartBody.Part): Response<UserPublic>

    @POST("/media/upload")
    @retrofit2.http.Multipart
    suspend fun uploadMedia(@retrofit2.http.Part file: okhttp3.MultipartBody.Part): Response<Map<String, String>>

    @POST("/me/profile")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): Response<UserPublic>

    @GET("/users/{id}")
    suspend fun getUserProfile(@retrofit2.http.Path("id") id: String): Response<UserPublic>

    // Messages
    @GET("/chats/{chatId}/messages")
    suspend fun getChatMessages(@retrofit2.http.Path("chatId") chatId: String): Response<List<MessageDto>>

    @POST("/chats/{chatId}/messages")
    suspend fun sendMessage(@retrofit2.http.Path("chatId") chatId: String, @Body request: SendMessageDto): Response<MessageDto>

    @GET("/chats")
    suspend fun getChats(): Response<List<ChatDto>>

    @GET("/chats/{id}")
    suspend fun getChat(@retrofit2.http.Path("id") id: String): Response<ChatDto>

    @POST("/chats/{id}/members")
    suspend fun addChatMember(@retrofit2.http.Path("id") id: String, @Body request: AddMemberRequest): Response<Unit>
    @POST("/chats/{id}/read")
    suspend fun markChatRead(@retrofit2.http.Path("id") id: String): Response<Unit>
}

data class ChatDto(
    val id: Long,
    val type: Int,
    val title: String,
    val description: String,
    val avatar: String,
    val owner_id: Long,
    val last_message_preview: String?,
    val last_message_at: Long,
    val members_count: Long = 0,
    val online_count: Long = 0,
    val other_user_id: Long = 0,
    val unread_count: Long = 0
)

data class MessageDto(
    val id: Long,
    val chat_id: Long,
    val sender_id: Long,
    val content: String,
    val type: String,
    val status: String,
    val created_at: String
)

data class SendMessageDto(
    val content: String,
    val type: String = "text"
)

data class UpdateProfileRequest(
    val username: String,
    val bio: String? = null,
    val first_name: String? = null,
    val last_name: String? = null
)

data class AddMemberRequest(
    val user_id: Long
)

data class Story(
    val id: Long,
    val user_id: Long,
    val media_url: String,
    val media_type: String,
    val caption: String,
    val expires_at: String
)

data class CreateStoryRequest(
    val media_url: String,
    val media_type: String,
    val caption: String
)

data class UserPublic(
    val id: Long,
    val username: String,
    val bio: String?,
    val avatar: String?,
    val public_key: String,
    val first_name: String? = null,
    val last_name: String? = null,
    val is_online: Boolean = false,
    val last_seen: Long = 0
)

data class Chat(
    val id: Long,
    val type: Int,
    val title: String,
    val description: String?
)

data class CreateChatRequest(
    val type: Int,
    val title: String,
    val description: String,
    val recipient_id: Long? = null
)
