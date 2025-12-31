package ru.faustyu.paprika.data

import android.content.Context
import android.content.SharedPreferences

class PrefsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("paprika_prefs", Context.MODE_PRIVATE)

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
}
