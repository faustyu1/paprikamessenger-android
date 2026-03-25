package ru.faustyu.paprika.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class PrefsManager(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "paprika_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_BACKEND_URL = "backend_url"
        private const val KEY_DARK_THEME = "dark_theme" // deprecated
        private const val KEY_THEME_MODE = "theme_mode" // 0: System, 1: Light, 2: Dark
        private const val KEY_FONT_SIZE = "font_size"
        private const val KEY_APP_LOCK = "app_lock_enabled"
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_NOTIFICATION_SOUND = "notification_sound"
        private const val KEY_MY_USER_ID = "my_user_id"
        private const val KEY_USERNAME = "username" // saved after register for re-login

        val themeModeFlow = kotlinx.coroutines.flow.MutableStateFlow(0)
        val fontSizeFlow = kotlinx.coroutines.flow.MutableStateFlow(1.0f)
    }

    init {
        if (prefs.contains(KEY_DARK_THEME)) {
            val dark = prefs.getBoolean(KEY_DARK_THEME, false)
            prefs.edit().remove(KEY_DARK_THEME).putInt(KEY_THEME_MODE, if (dark) 2 else 1).apply()
        }
        themeModeFlow.value = prefs.getInt(KEY_THEME_MODE, 0)
        fontSizeFlow.value = prefs.getFloat(KEY_FONT_SIZE, 1.0f)
    }

    var token: String?
        get() = prefs.getString(KEY_TOKEN, null)
        set(value) {
            prefs.edit().putString(KEY_TOKEN, value).apply()
        }

    var backendUrl: String?
        get() = prefs.getString(KEY_BACKEND_URL, null)
        set(value) {
            prefs.edit().putString(KEY_BACKEND_URL, value).apply()
        }

    fun clear() {
        prefs.edit().clear().apply()
    }

    var themeMode: Int
        get() = prefs.getInt(KEY_THEME_MODE, 0)
        set(value) { 
            prefs.edit().putInt(KEY_THEME_MODE, value).apply() 
            themeModeFlow.value = value
        }

    var fontSize: Float
        get() = prefs.getFloat(KEY_FONT_SIZE, 1.0f)
        set(value) { 
            prefs.edit().putFloat(KEY_FONT_SIZE, value).apply() 
            fontSizeFlow.value = value
        }

    var appLockEnabled: Boolean
        get() = prefs.getBoolean(KEY_APP_LOCK, false)
        set(value) { prefs.edit().putBoolean(KEY_APP_LOCK, value).apply() }

    var pinHash: String?
        get() = prefs.getString(KEY_PIN_HASH, null)
        set(value) {
            if (value == null) prefs.edit().remove(KEY_PIN_HASH).apply()
            else prefs.edit().putString(KEY_PIN_HASH, value).apply()
        }

    var notificationSound: Boolean
        get() = prefs.getBoolean(KEY_NOTIFICATION_SOUND, true)
        set(value) { prefs.edit().putBoolean(KEY_NOTIFICATION_SOUND, value).apply() }

    var savedUsername: String?
        get() = prefs.getString(KEY_USERNAME, null)
        set(value) {
            if (value == null) prefs.edit().remove(KEY_USERNAME).apply()
            else prefs.edit().putString(KEY_USERNAME, value).apply()
        }

    fun getDraft(chatId: String): String? = prefs.getString("draft_$chatId", null)

    fun setDraft(chatId: String, text: String) {
        if (text.isBlank()) prefs.edit().remove("draft_$chatId").apply()
        else prefs.edit().putString("draft_$chatId", text).apply()
    }

    var myUserId: Long
        get() = prefs.getLong(KEY_MY_USER_ID, 0L)
        set(value) {
            prefs.edit().putLong(KEY_MY_USER_ID, value).apply()
        }
}
