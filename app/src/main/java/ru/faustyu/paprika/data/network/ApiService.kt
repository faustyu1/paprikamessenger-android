package ru.faustyu.paprika.data.network

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

// ── Passwordless auth models ──────────────────────────────────────────────

data class RegisterRequest(
    @SerializedName("username") val username: String,
    @SerializedName("public_key") val public_key: String,
    @SerializedName("first_name") val first_name: String,
    @SerializedName("last_name") val last_name: String = ""
)

data class AuthResponse(
    val token: String,
    val error: String?
)

data class ChallengeRequest(@SerializedName("username") val username: String)
data class ChallengeResponse(val challenge_id: String, val challenge: String)

data class VerifyRequest(
    @SerializedName("challenge_id") val challenge_id: String,
    @SerializedName("signature") val signature: String
)

data class QRCreateResponse(val qr_id: String, val challenge: String)
data class QRConfirmRequest(
    @SerializedName("qr_id") val qr_id: String,
    @SerializedName("signature") val signature: String
)
data class QRStatusResponse(val status: String, val token: String?)

data class KeyBackupRequest(
    @SerializedName("encrypted_backup") val encrypted_backup: String,
    @SerializedName("salt") val salt: String
)
data class KeyBackupResponse(val encrypted_backup: String, val salt: String)

// ── API interface ─────────────────────────────────────────────────────────

interface ApiService {
    // Registration (passwordless — public key only)
    @POST("/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    // Challenge-response login
    @POST("/auth/challenge")
    suspend fun createChallenge(@Body request: ChallengeRequest): Response<ChallengeResponse>

    @POST("/auth/verify")
    suspend fun verifyChallenge(@Body request: VerifyRequest): Response<AuthResponse>

    // QR login (from another device)
    @POST("/auth/qr/create")
    suspend fun createQRSession(): Response<QRCreateResponse>

    @POST("/auth/qr/confirm")
    suspend fun confirmQRSession(@Body request: QRConfirmRequest): Response<Map<String, String>>

    @GET("/auth/qr/status/{id}")
    suspend fun pollQRSession(@retrofit2.http.Path("id") id: String): Response<QRStatusResponse>

    // Key backup (encrypted on client, decrypted with PIN)
    @POST("/auth/backup")
    suspend fun saveKeyBackup(@Body request: KeyBackupRequest): Response<Map<String, String>>

    @GET("/auth/backup/{username}")
    suspend fun getKeyBackup(@retrofit2.http.Path("username") username: String): Response<KeyBackupResponse>

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
    suspend fun getChatMessages(
        @retrofit2.http.Path("chatId") chatId: String,
        @retrofit2.http.Query("q") q: String? = null
    ): Response<List<MessageDto>>

    @POST("/chats/{chatId}/messages")
    suspend fun sendMessage(@retrofit2.http.Path("chatId") chatId: String, @Body request: SendMessageDto): Response<MessageDto>

    @GET("/chats")
    suspend fun getChats(): Response<List<ChatDto>>

    @GET("/chats/{id}")
    suspend fun getChat(@retrofit2.http.Path("id") id: String): Response<ChatDto>

    @POST("/chats/{id}/members")
    suspend fun addChatMember(@retrofit2.http.Path("id") id: String, @Body request: AddMemberRequest): Response<Unit>

    @GET("/chats/{id}/members")
    suspend fun getChatMembers(@retrofit2.http.Path("id") id: String): Response<List<GroupMemberDto>>

    @DELETE("/chats/{id}/members/{userId}")
    suspend fun removeChatMember(@retrofit2.http.Path("id") id: String, @retrofit2.http.Path("userId") userId: Long): Response<Unit>

    @POST("/chats/{id}/members/{userId}/promote")
    suspend fun promoteChatMember(@retrofit2.http.Path("id") id: String, @retrofit2.http.Path("userId") userId: Long): Response<Unit>

    @POST("/chats/{id}/members/{userId}/demote")
    suspend fun demoteChatMember(@retrofit2.http.Path("id") id: String, @retrofit2.http.Path("userId") userId: Long): Response<Unit>

    @POST("/chats/{id}/leave")
    suspend fun leaveChat(@retrofit2.http.Path("id") id: String): Response<Unit>

    @retrofit2.http.PUT("/chats/{id}")
    suspend fun updateChat(@retrofit2.http.Path("id") id: String, @Body request: UpdateChatRequest): Response<ChatDto>

    @DELETE("/chats/{id}")
    suspend fun deleteChat(@retrofit2.http.Path("id") id: String): Response<Unit>

    @POST("/chats/{id}/read")
    suspend fun markChatRead(@retrofit2.http.Path("id") id: String): Response<Unit>

    // Message actions
    @retrofit2.http.PUT("/messages/{id}")
    suspend fun editMessage(@retrofit2.http.Path("id") id: String, @Body request: EditMessageRequest): Response<MessageDto>

    @DELETE("/messages/{id}")
    suspend fun deleteMessage(@retrofit2.http.Path("id") id: String): Response<Unit>

    @POST("/messages/{id}/forward")
    suspend fun forwardMessage(@retrofit2.http.Path("id") id: String, @Body request: ForwardMessageRequest): Response<MessageDto>

    // TURN credentials
    @GET("/turn-credentials")
    suspend fun getTurnCredentials(): Response<TurnCredentials>

    // Calls
    @POST("/calls")
    suspend fun initiateCall(@Body request: ru.faustyu.paprika.ui.call.InitiateCallRequest): Response<ru.faustyu.paprika.ui.call.CallResponse>

    @POST("/calls/{id}/accept")
    suspend fun acceptCall(@retrofit2.http.Path("id") id: Long): Response<ru.faustyu.paprika.ui.call.CallResponse>

    @POST("/calls/{id}/reject")
    suspend fun rejectCall(@retrofit2.http.Path("id") id: Long): Response<ru.faustyu.paprika.ui.call.CallResponse>

    @POST("/calls/{id}/end")
    suspend fun endCall(@retrofit2.http.Path("id") id: Long): Response<ru.faustyu.paprika.ui.call.CallResponse>

    // Reactions
    @POST("/messages/{id}/reactions")
    suspend fun addReaction(@retrofit2.http.Path("id") id: Long, @Body request: AddReactionRequest): Response<Unit>

    @DELETE("/messages/{id}/reactions/{emoji}")
    suspend fun removeReaction(@retrofit2.http.Path("id") id: Long, @retrofit2.http.Path("emoji") emoji: String): Response<Unit>

    // Pin message
    @POST("/chats/{id}/pin")
    suspend fun pinMessage(@retrofit2.http.Path("id") chatId: String, @Body request: PinMessageRequest): Response<Unit>

    @DELETE("/chats/{id}/pin")
    suspend fun unpinMessage(@retrofit2.http.Path("id") chatId: String): Response<Unit>

    @GET("/chats/{id}/pinned")
    suspend fun getPinnedMessage(@retrofit2.http.Path("id") chatId: String): Response<MessageDto>

    // Invite
    @POST("/chats/{id}/invite")
    suspend fun generateInvite(@retrofit2.http.Path("id") chatId: String): Response<Map<String, String>>

    @DELETE("/chats/{id}/invite")
    suspend fun revokeInvite(@retrofit2.http.Path("id") chatId: String): Response<Unit>

    @GET("/invite/{token}")
    suspend fun getChatByInvite(@retrofit2.http.Path("token") token: String): Response<InviteChatInfo>

    @POST("/invite/{token}/join")
    suspend fun joinByInvite(@retrofit2.http.Path("token") token: String): Response<Map<String, Long>>

    // Sessions
    @GET("/sessions")
    suspend fun getSessions(): Response<List<ru.faustyu.paprika.ui.settings.SessionDto>>

    @DELETE("/sessions/{id}")
    suspend fun deleteSession(@retrofit2.http.Path("id") id: String): Response<Unit>

    // Polls
    @GET("/polls/{id}")
    suspend fun getPoll(@retrofit2.http.Path("id") id: Long): Response<PollDto>

    @POST("/polls/{id}/vote")
    suspend fun votePoll(@retrofit2.http.Path("id") id: Long, @Body request: PollVoteRequest): Response<PollDto>

    // Chat preferences
    @POST("/chats/{id}/mute")
    suspend fun muteChat(@retrofit2.http.Path("id") id: String, @Body request: MuteChatRequest): Response<Unit>

    @POST("/chats/{id}/archive")
    suspend fun archiveChat(@retrofit2.http.Path("id") id: String): Response<Unit>

    @POST("/chats/{id}/pin_chat")
    suspend fun pinChat(@retrofit2.http.Path("id") id: String): Response<Unit>

    // Stickers
    @GET("/stickers/packs")
    suspend fun getStickerPacks(): Response<List<StickerPackDto>>
}

data class ReplyToDto(
    val id: Long,
    val content: String,
    val type: String,
    val sender_id: Long
)

data class MessageDto(
    val id: Long,
    val chat_id: Long,
    val sender_id: Long,
    val content: String,
    val type: String,
    val status: String,
    val created_at: Long,
    val edited_at: Long? = null,
    val reply_to_message_id: Long? = null,
    val reply_to_message: ReplyToDto? = null,
    val forwarded_from_user: ForwardedUserDto? = null,
    val reactions: List<ReactionCount> = emptyList(),
    val poll: PollDto? = null
)

data class ForwardedUserDto(
    val id: Long,
    val username: String,
    val first_name: String? = null,
    val last_name: String? = null
)

data class SendMessageDto(
    val content: String,
    val type: String = "text",
    val reply_to_message_id: Long? = null,
    val expires_after_sec: Long? = null
)

data class UpdateProfileRequest(
    val username: String,
    val bio: String? = null,
    val first_name: String? = null,
    val last_name: String? = null,
    val birthday: String? = null // format: "YYYY-MM-DD" or null/empty to clear
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
    val last_seen: Long = 0,
    val birthday: String? = null // format: "YYYY-MM-DD" or null
)

data class Chat(
    val id: Long,
    val type: Int,
    val title: String,
    val description: String?
)

data class TurnCredentials(
    val urls: List<String>,
    val username: String? = null,
    val credential: String? = null
)

data class CreateChatRequest(
    val type: Int,
    val title: String,
    val description: String,
    val recipient_id: Long? = null
)

data class GroupMemberDto(
    val user_id: Long,
    val username: String,
    val first_name: String = "",
    val last_name: String = "",
    val avatar: String = "",
    val role: String,
    val is_online: Boolean = false
)

data class UpdateChatRequest(
    val title: String = "",
    val description: String = ""
)

data class ReactionCount(
    val emoji: String,
    val count: Long,
    val is_mine: Boolean = false
)

data class AddReactionRequest(val emoji: String)

data class PinMessageRequest(val message_id: Long)

data class EditMessageRequest(val content: String)
data class ForwardMessageRequest(val to_chat_id: Long)

data class InviteChatInfo(
    val id: Long,
    val title: String,
    val description: String,
    val avatar: String,
    val members_count: Long
)

data class PollOptionDto(
    val id: Long,
    val text: String,
    val votes_count: Long = 0,
    val is_voted_by_me: Boolean = false
)

data class PollDto(
    val id: Long,
    val question: String,
    val is_multiple: Boolean = false,
    val is_closed: Boolean = false,
    val options: List<PollOptionDto> = emptyList()
)

data class PollVoteRequest(val option_id: Long)

data class MuteChatRequest(val mute_until: Long)

data class StickerDto(
    val id: Long,
    val emoji: String = "",
    val file_url: String
)

data class StickerPackDto(
    val id: Long,
    val name: String,
    val stickers: List<StickerDto> = emptyList()
)

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
    val unread_count: Long = 0,
    val mute_until: Long = 0,
    val is_archived: Boolean = false,
    val is_pinned: Boolean = false,
    val invite_link: String? = null
)
