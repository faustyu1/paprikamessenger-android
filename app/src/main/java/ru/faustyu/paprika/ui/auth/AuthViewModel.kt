package ru.faustyu.paprika.ui.auth

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import ru.faustyu.paprika.analytics.AnalyticsHelper
import ru.faustyu.paprika.data.repository.AuthRepository
import ru.faustyu.paprika.ui.base.BaseViewModel
import ru.faustyu.paprika.util.Constants
import ru.faustyu.paprika.util.CryptoManager
import ru.faustyu.paprika.util.Result
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val analyticsHelper: AnalyticsHelper
) : BaseViewModel() {

    fun authenticate(
        isLogin: Boolean, 
        username: String, 
        password: String,
        firstName: String = "",
        lastName: String = "",
        onSuccess: (String) -> Unit
    ) {
        if (username.isBlank() || password.isBlank()) {
            showError("Username and password cannot be empty")
            return
        }

        if (!isLogin) {
            if (firstName.isBlank()) {
                showError("First name is required")
                return
            }
            if (password.length < Constants.MIN_PASSWORD_LENGTH) {
                showError("Password must be at least ${Constants.MIN_PASSWORD_LENGTH} characters")
                return
            }
            if (username.length < Constants.MIN_USERNAME_LENGTH) {
                showError("Username must be at least ${Constants.MIN_USERNAME_LENGTH} characters")
                return
            }
            if (username.first().isDigit()) {
                showError("Username cannot start with a number")
                return
            }
        }
        
        viewModelScope.launch {
            showLoading()
            
            val result = if (isLogin) {
                authRepository.login(username, password)
            } else {
                // Generate real crypto keys for registration
                CryptoManager.generateKeys()
                val publicKey = CryptoManager.publicKey?.toString(16) ?: ""
                
                authRepository.register(
                    username = username,
                    password = password,
                    firstName = firstName,
                    lastName = lastName,
                    publicKey = publicKey
                )
            }
            
            when (result) {
                is Result.Success -> {
                    hideLoading()
                    
                    // Track analytics event
                    if (isLogin) {
                        analyticsHelper.logLogin("email")
                    } else {
                        analyticsHelper.logSignUp("email")
                    }
                    
                    onSuccess(result.data)
                }
                is Result.Error -> {
                    showError(result.message ?: "Authentication failed")
                }
                is Result.Loading -> {
                    // Should not happen
                }
            }
        }
    }
}
