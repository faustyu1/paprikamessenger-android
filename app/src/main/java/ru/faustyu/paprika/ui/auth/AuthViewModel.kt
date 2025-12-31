package ru.faustyu.paprika.ui.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import ru.faustyu.paprika.data.network.AuthRequest
import ru.faustyu.paprika.data.network.NetworkModule

class AuthViewModel : ViewModel() {
    var isLoading by mutableStateOf(false)
        private set
    
    var error by mutableStateOf<String?>(null)
        private set

    fun authenticate(
        isLogin: Boolean, 
        username: String, 
        password: String,
        firstName: String = "",
        lastName: String = "",
        onSuccess: (String) -> Unit
    ) {
        if (username.isBlank() || password.isBlank()) {
            error = "Username and password cannot be empty"
            return
        }

        if (!isLogin) {
            if (firstName.isBlank()) {
                error = "First name is required"
                return
            }
            if (password.length < 6) {
                error = "Password must be at least 6 characters"
                return
            }
            if (username.length < 3) {
                error = "Username must be at least 3 characters"
                return
            }
            if (username.first().isDigit()) {
                error = "Username cannot start with a number"
                return
            }
        }
        
        viewModelScope.launch {
            isLoading = true
            error = null
            try {
                val api = NetworkModule.api
                val request = AuthRequest(
                    username = username, 
                    password = password, 
                    public_key = "dummy_pk_for_now",
                    first_name = firstName,
                    last_name = lastName
                )
                
                val response = if (isLogin) {
                    api.login(request)
                } else {
                    api.register(request)
                }

                if (response.isSuccessful && response.body()?.token != null) {
                    onSuccess(response.body()!!.token)
                } else {
                    error = response.body()?.error ?: "Authentication failed: ${response.code()}"
                }
            } catch (e: Exception) {
                error = "Network error: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }
}
