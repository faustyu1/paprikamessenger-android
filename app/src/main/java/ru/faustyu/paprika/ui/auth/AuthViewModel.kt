package ru.faustyu.paprika.ui.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import kotlinx.coroutines.launch
import ru.faustyu.paprika.data.network.AuthRequest
import ru.faustyu.paprika.data.network.NetworkModule

private data class ErrorBody(val error: String? = null)

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
            error = "Введите логин и пароль"
            return
        }

        if (!isLogin) {
            if (firstName.isBlank()) {
                error = "Введите имя"
                return
            }
            if (username.length < 3) {
                error = "Логин минимум 3 символа"
                return
            }
            if (username.first().isDigit()) {
                error = "Логин не может начинаться с цифры"
                return
            }
            if (password.length < 8) {
                error = "Пароль минимум 8 символов"
                return
            }
            if (!password.any { it.isUpperCase() } || !password.any { it.isLowerCase() } || !password.any { it.isDigit() }) {
                error = "Пароль должен содержать заглавную букву, строчную и цифру"
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
                    val serverError = response.errorBody()?.string()
                        ?.let { runCatching { Gson().fromJson(it, ErrorBody::class.java).error }.getOrNull() }
                    error = serverError ?: "Ошибка ${response.code()}"
                }
            } catch (e: Exception) {
                error = "Нет соединения с сервером"
            } finally {
                isLoading = false
            }
        }
    }
}
