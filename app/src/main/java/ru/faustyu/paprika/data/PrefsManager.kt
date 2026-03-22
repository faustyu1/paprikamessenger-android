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
        private const val KEY_DARK_THEME = "dark_theme"
        private const val KEY_FONT_SIZE = "font_size"
        private const val KEY_APP_LOCK = "app_lock_enabled"
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_NOTIFICATION_SOUND = "notification_sound"
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

    var darkTheme: Boolean
        get() = prefs.getBoolean(KEY_DARK_THEME, false)
        set(value) { prefs.edit().putBoolean(KEY_DARK_THEME, value).apply() }

    var fontSize: Float
        get() = prefs.getFloat(KEY_FONT_SIZE, 1.0f)
        set(value) { prefs.edit().putFloat(KEY_FONT_SIZE, value).apply() }

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

    fun getDraft(chatId: String): String? = prefs.getString("draft_$chatId", null)

    fun setDraft(chatId: String, text: String) {
        if (text.isBlank()) prefs.edit().remove("draft_$chatId").apply()
        else prefs.edit().putString("draft_$chatId", text).apply()
    }
}
