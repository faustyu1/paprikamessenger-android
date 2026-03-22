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

    fun getDraft(chatId: String): String? = prefs.getString("draft_$chatId", null)

    fun setDraft(chatId: String, text: String) {
        if (text.isBlank()) prefs.edit().remove("draft_$chatId").apply()
        else prefs.edit().putString("draft_$chatId", text).apply()
    }
}
