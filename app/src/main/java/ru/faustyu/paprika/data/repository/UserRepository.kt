package ru.faustyu.paprika.data.repository

import kotlinx.coroutines.flow.Flow
import ru.faustyu.paprika.data.db.UserDao
import ru.faustyu.paprika.data.db.UserEntity
import ru.faustyu.paprika.data.network.ApiService
import ru.faustyu.paprika.data.network.UpdateProfileRequest
import ru.faustyu.paprika.data.network.UserPublic
import ru.faustyu.paprika.util.Result
import ru.faustyu.paprika.util.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for user operations
 */
@Singleton
class UserRepository @Inject constructor(
    private val apiService: ApiService,
    private val userDao: UserDao
) {
    
    /**
     * Get current user profile
     */
    suspend fun getMyProfile(): Result<UserPublic> {
        return safeApiCall {
            val response = apiService.getMyProfile()
            if (response.isSuccessful && response.body() != null) {
                val user = response.body()!!
                // Cache in database
                userDao.insertUser(user.toEntity())
                user
            } else {
                throw Exception("Failed to fetch profile")
            }
        }
    }
    
    /**
     * Get user profile by ID
     */
    suspend fun getUserProfile(userId: String): Result<UserPublic> {
        return safeApiCall {
            val response = apiService.getUserProfile(userId)
            if (response.isSuccessful && response.body() != null) {
                val user = response.body()!!
                userDao.insertUser(user.toEntity())
                user
            } else {
                throw Exception("Failed to fetch user profile")
            }
        }
    }
    
    /**
     * Search users
     */
    suspend fun searchUsers(query: String): Result<List<UserPublic>> {
        return safeApiCall {
            val response = apiService.searchUsers(query)
            if (response.isSuccessful && response.body() != null) {
                response.body()!!
            } else {
                throw Exception("Search failed")
            }
        }
    }
    
    /**
     * Update profile
     */
    suspend fun updateProfile(
        username: String,
        firstName: String?,
        lastName: String?,
        bio: String?
    ): Result<UserPublic> {
        return safeApiCall {
            val request = UpdateProfileRequest(
                username = username,
                first_name = firstName,
                last_name = lastName,
                bio = bio
            )
            val response = apiService.updateProfile(request)
            if (response.isSuccessful && response.body() != null) {
                val user = response.body()!!
                userDao.insertUser(user.toEntity())
                user
            } else {
                throw Exception("Failed to update profile")
            }
        }
    }
    
    /**
     * Upload avatar
     */
    suspend fun uploadAvatar(avatarPart: okhttp3.MultipartBody.Part): Result<UserPublic> {
        return safeApiCall {
            val response = apiService.uploadAvatar(avatarPart)
            if (response.isSuccessful && response.body() != null) {
                val user = response.body()!!
                userDao.insertUser(user.toEntity())
                user
            } else {
                throw Exception("Failed to upload avatar")
            }
        }
    }
    
    /**
     * Get cached users from database
     */
    fun getCachedUsers(): Flow<List<UserEntity>> {
        return userDao.getAllUsers()
    }
}

/**
 * Extension to convert UserPublic to UserEntity
 */
private fun UserPublic.toEntity(): UserEntity {
    return UserEntity(
        id = id,
        username = username,
        firstName = first_name,
        lastName = last_name,
        bio = bio,
        avatar = avatar,
        publicKey = public_key,
        isOnline = is_online,
        lastSeen = last_seen
    )
}
