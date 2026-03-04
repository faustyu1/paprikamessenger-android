package ru.faustyu.paprika.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import ru.faustyu.paprika.data.PrefsManager
import ru.faustyu.paprika.data.network.ApiService
import ru.faustyu.paprika.data.network.AuthRequest
import ru.faustyu.paprika.util.Result
import ru.faustyu.paprika.util.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for authentication operations
 */
@Singleton
class AuthRepository @Inject constructor(
    private val apiService: ApiService,
    private val prefsManager: PrefsManager
) {
    
    /**
     * Login user
     */
    suspend fun login(username: String, password: String): Result<String> {
        return safeApiCall {
            val request = AuthRequest(
                username = username,
                password = password
            )
            val response = apiService.login(request)
            
            if (response.isSuccessful && response.body()?.token != null) {
                val token = response.body()!!.token
                prefsManager.token = token
                token
            } else {
                throw Exception(response.body()?.error ?: "Login failed")
            }
        }
    }
    
    /**
     * Register new user
     */
    suspend fun register(
        username: String,
        password: String,
        firstName: String,
        lastName: String,
        publicKey: String
    ): Result<String> {
        return safeApiCall {
            val request = AuthRequest(
                username = username,
                password = password,
                public_key = publicKey,
                first_name = firstName,
                last_name = lastName
            )
            val response = apiService.register(request)
            
            if (response.isSuccessful && response.body()?.token != null) {
                val token = response.body()!!.token
                prefsManager.token = token
                token
            } else {
                throw Exception(response.body()?.error ?: "Registration failed")
            }
        }
    }
    
    /**
     * Logout user
     */
    fun logout() {
        prefsManager.clear()
    }
    
    /**
     * Check if user is logged in
     */
    fun isLoggedIn(): Boolean {
        return prefsManager.token != null
    }
    
    /**
     * Get current auth token
     */
    fun getToken(): String? {
        return prefsManager.token
    }
    
    /**
     * Update backend URL
     */
    fun setBackendUrl(url: String) {
        prefsManager.backendUrl = url
    }
    
    /**
     * Get backend URL
     */
    fun getBackendUrl(): String? {
        return prefsManager.backendUrl
    }
}
