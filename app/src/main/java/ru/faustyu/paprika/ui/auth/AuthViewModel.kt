package ru.faustyu.paprika.ui.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.faustyu.paprika.data.network.ChallengeRequest
import ru.faustyu.paprika.data.network.NetworkModule
import ru.faustyu.paprika.data.network.QRConfirmRequest
import ru.faustyu.paprika.data.network.RegisterRequest
import ru.faustyu.paprika.data.network.VerifyRequest
import ru.faustyu.paprika.util.CryptoManager

private data class ErrorBody(val error: String? = null)

class AuthViewModel : ViewModel() {
    var isLoading by mutableStateOf(false)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    // ── Registration ──────────────────────────────────────────────────────

    fun register(
        username: String,
        firstName: String,
        lastName: String,
        onSuccess: (String) -> Unit
    ) {
        if (username.isBlank()) { error = "Введите логин"; return }
        if (firstName.isBlank()) { error = "Введите имя"; return }
        if (username.length < 3) { error = "Логин минимум 3 символа"; return }
        if (username.first().isDigit()) { error = "Логин не может начинаться с цифры"; return }
        if (!username.matches(Regex("[a-zA-Z0-9_]+"))) {
            error = "Логин может содержать только латинские буквы, цифры и _"; return
        }

        val publicKey = CryptoManager.getPublicKeyBase64()
        if (publicKey == null) {
            error = "Ошибка генерации ключа — попробуйте ещё раз"
            return
        }

        viewModelScope.launch {
            isLoading = true
            error = null
            try {
                val resp = NetworkModule.api.register(
                    RegisterRequest(
                        username = username,
                        public_key = publicKey,
                        first_name = firstName,
                        last_name = lastName
                    )
                )
                if (resp.isSuccessful && resp.body()?.token != null) {
                    onSuccess(resp.body()!!.token)
                } else {
                    error = parseError(resp.errorBody()?.string(), resp.code())
                }
            } catch (e: Exception) {
                error = e.message ?: "Ошибка сети"
            } finally {
                isLoading = false
            }
        }
    }

    // ── Login via challenge-response ──────────────────────────────────────

    fun login(username: String, onSuccess: (String) -> Unit) {
        if (username.isBlank()) { error = "Введите логин"; return }

        viewModelScope.launch {
            isLoading = true
            error = null
            try {
                val api = NetworkModule.api

                // Step 1: get challenge
                val challengeResp = api.createChallenge(ChallengeRequest(username))
                if (!challengeResp.isSuccessful) {
                    error = parseError(challengeResp.errorBody()?.string(), challengeResp.code())
                    return@launch
                }
                val challengeData = challengeResp.body()!!

                // Step 2: sign challenge with Android Keystore key
                val signature = CryptoManager.signChallenge(challengeData.challenge)

                // Step 3: verify
                val verifyResp = api.verifyChallenge(
                    VerifyRequest(
                        challenge_id = challengeData.challenge_id,
                        signature = signature
                    )
                )
                if (verifyResp.isSuccessful && verifyResp.body()?.token != null) {
                    onSuccess(verifyResp.body()!!.token)
                } else {
                    error = parseError(verifyResp.errorBody()?.string(), verifyResp.code())
                }
            } catch (e: Exception) {
                error = e.message ?: "Ошибка сети"
            } finally {
                isLoading = false
            }
        }
    }

    // ── QR login (confirm from this device — Device A) ────────────────────

    fun confirmQRLogin(qrId: String, challengeHex: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            isLoading = true
            error = null
            try {
                val signature = CryptoManager.signChallenge(challengeHex)
                val resp = NetworkModule.api.confirmQRSession(
                    QRConfirmRequest(qr_id = qrId, signature = signature)
                )
                if (resp.isSuccessful) {
                    onSuccess()
                } else {
                    error = parseError(resp.errorBody()?.string(), resp.code())
                }
            } catch (e: Exception) {
                error = e.message ?: "Ошибка"
            } finally {
                isLoading = false
            }
        }
    }

    // ── QR login (wait for confirmation — Device B) ───────────────────────

    fun startQRSession(onToken: (String) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val createResp = NetworkModule.api.createQRSession()
                if (!createResp.isSuccessful) { onError("Не удалось создать QR-сессию"); return@launch }
                val qrData = createResp.body()!!

                // Poll until confirmed or expired (30s max)
                repeat(60) {
                    delay(500)
                    val statusResp = NetworkModule.api.pollQRSession(qrData.qr_id)
                    val status = statusResp.body()
                    when (status?.status) {
                        "confirmed" -> {
                            val token = status.token
                            if (token != null) { onToken(token); return@launch }
                        }
                        "expired" -> { onError("QR-код истёк"); return@launch }
                    }
                }
                onError("Время ожидания истекло")
            } catch (e: Exception) {
                onError(e.message ?: "Ошибка")
            }
        }
    }

    fun clearError() { error = null }

    private fun parseError(body: String?, code: Int): String {
        val serverMsg = body?.let {
            runCatching { Gson().fromJson(it, ErrorBody::class.java).error }.getOrNull()
        }
        if (serverMsg != null) return serverMsg
        return when (code) {
            409 -> "Этот логин уже занят — выберите другой"
            429 -> "Слишком много попыток — подождите немного"
            401, 403 -> "Неверный логин или ключ устройства"
            404 -> "Пользователь не найден"
            500 -> "Ошибка сервера — попробуйте позже"
            else -> "Ошибка $code"
        }
    }
}
