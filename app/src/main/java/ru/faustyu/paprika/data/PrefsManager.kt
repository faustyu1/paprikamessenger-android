package ru.faustyu.paprika.data

import android.content.SharedPreferences
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager for encrypted shared preferences.
 * Token and sensitive data are now encrypted at rest.
 */
@Singleton
class PrefsManager @Inject constructor(
    private val prefs: SharedPreferences
) {
    companion object {
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_BACKEND_URL = "backend_url"
        private const val KEY_USER_ID = "user_id"
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
    
    var userId: Long
        get() = prefs.getLong(KEY_USER_ID, 0L)
        set(value) {
            prefs.edit().putLong(KEY_USER_ID, value).apply()
        }
    
    fun clear() {
        prefs.edit().clear().apply()
    }
}
