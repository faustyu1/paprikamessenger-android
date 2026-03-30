package ru.faustyu.paprika.data

import android.content.Context
import android.os.BatteryManager
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
        // Core
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_BACKEND_URL = "backend_url"
        private const val KEY_DARK_THEME = "dark_theme" // deprecated
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_FONT_SIZE = "font_size"
        private const val KEY_APP_LOCK = "app_lock_enabled"
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_MY_USER_ID = "my_user_id"
        private const val KEY_USERNAME = "username"
        // Notifications
        private const val KEY_NOTIFICATION_SOUND = "notification_sound"
        private const val KEY_NOTIFY_PRIVATE = "notify_private"
        private const val KEY_NOTIFY_GROUPS = "notify_groups"
        private const val KEY_NOTIFY_VIBRATE = "notify_vibrate"
        private const val KEY_BADGE_SHOW = "badge_show"
        private const val KEY_BADGE_MUTED = "badge_muted"
        // Power Saving
        private const val KEY_POWER_SAVING_MODE = "power_saving_mode" // 0=Off 1=Auto 2=On
        private const val KEY_PS_THRESHOLD = "ps_threshold"           // battery % (default 21)
        private const val KEY_PS_ANIM_STICKERS = "ps_anim_stickers"
        private const val KEY_PS_ANIM_EMOJI = "ps_anim_emoji"
        private const val KEY_PS_EFFECTS_CHATS = "ps_effects_chats"
        private const val KEY_PS_ANIM_CALLS = "ps_anim_calls"
        private const val KEY_PS_AUTOPLAY_VIDEOS = "ps_autoplay_videos"
        private const val KEY_PS_AUTOPLAY_GIFS = "ps_autoplay_gifs"
        private const val KEY_PS_PARTICLES = "ps_particles"
        private const val KEY_SMOOTH_TRANSITIONS = "smooth_transitions"
        // Data & Storage
        private const val KEY_AUTO_DL_MOBILE = "auto_dl_mobile"
        private const val KEY_AUTO_DL_WIFI = "auto_dl_wifi"
        private const val KEY_AUTO_DL_ROAMING = "auto_dl_roaming"
        private const val KEY_SAVE_GALLERY_PRIVATE = "save_gallery_private"
        private const val KEY_SAVE_GALLERY_GROUPS = "save_gallery_groups"

        val themeModeFlow = kotlinx.coroutines.flow.MutableStateFlow(0)
        val fontSizeFlow = kotlinx.coroutines.flow.MutableStateFlow(1.0f)
        val isPowerSavingFlow = kotlinx.coroutines.flow.MutableStateFlow(false)
        val smoothTransitionsFlow = kotlinx.coroutines.flow.MutableStateFlow(true)
    }

    init {
        if (prefs.contains(KEY_DARK_THEME)) {
            val dark = prefs.getBoolean(KEY_DARK_THEME, false)
            prefs.edit().remove(KEY_DARK_THEME).putInt(KEY_THEME_MODE, if (dark) 2 else 1).apply()
        }
        themeModeFlow.value = prefs.getInt(KEY_THEME_MODE, 0)
        fontSizeFlow.value = prefs.getFloat(KEY_FONT_SIZE, 1.0f)
        smoothTransitionsFlow.value = prefs.getBoolean(KEY_SMOOTH_TRANSITIONS, true)
    }

    // ── Core ────────────────────────────────────────────────────────────────

    var token: String?
        get() = prefs.getString(KEY_TOKEN, null)
        set(value) { prefs.edit().putString(KEY_TOKEN, value).apply() }

    var backendUrl: String?
        get() = prefs.getString(KEY_BACKEND_URL, null)
        set(value) { prefs.edit().putString(KEY_BACKEND_URL, value).apply() }

    fun clear() { prefs.edit().clear().apply() }

    var themeMode: Int
        get() = prefs.getInt(KEY_THEME_MODE, 0)
        set(value) { prefs.edit().putInt(KEY_THEME_MODE, value).apply(); themeModeFlow.value = value }

    var fontSize: Float
        get() = prefs.getFloat(KEY_FONT_SIZE, 1.0f)
        set(value) { prefs.edit().putFloat(KEY_FONT_SIZE, value).apply(); fontSizeFlow.value = value }

    var appLockEnabled: Boolean
        get() = prefs.getBoolean(KEY_APP_LOCK, false)
        set(value) { prefs.edit().putBoolean(KEY_APP_LOCK, value).apply() }

    var pinHash: String?
        get() = prefs.getString(KEY_PIN_HASH, null)
        set(value) {
            if (value == null) prefs.edit().remove(KEY_PIN_HASH).apply()
            else prefs.edit().putString(KEY_PIN_HASH, value).apply()
        }

    var savedUsername: String?
        get() = prefs.getString(KEY_USERNAME, null)
        set(value) {
            if (value == null) prefs.edit().remove(KEY_USERNAME).apply()
            else prefs.edit().putString(KEY_USERNAME, value).apply()
        }

    var myUserId: Long
        get() = prefs.getLong(KEY_MY_USER_ID, 0L)
        set(value) { prefs.edit().putLong(KEY_MY_USER_ID, value).apply() }

    fun getDraft(chatId: String): String? = prefs.getString("draft_$chatId", null)

    fun setDraft(chatId: String, text: String) {
        if (text.isBlank()) prefs.edit().remove("draft_$chatId").apply()
        else prefs.edit().putString("draft_$chatId", text).apply()
    }

    // ── Notifications ────────────────────────────────────────────────────────

    var notificationSound: Boolean
        get() = prefs.getBoolean(KEY_NOTIFICATION_SOUND, true)
        set(value) { prefs.edit().putBoolean(KEY_NOTIFICATION_SOUND, value).apply() }

    var notifyPrivateChats: Boolean
        get() = prefs.getBoolean(KEY_NOTIFY_PRIVATE, true)
        set(value) { prefs.edit().putBoolean(KEY_NOTIFY_PRIVATE, value).apply() }

    var notifyGroups: Boolean
        get() = prefs.getBoolean(KEY_NOTIFY_GROUPS, true)
        set(value) { prefs.edit().putBoolean(KEY_NOTIFY_GROUPS, value).apply() }

    var notifyVibrate: Boolean
        get() = prefs.getBoolean(KEY_NOTIFY_VIBRATE, true)
        set(value) { prefs.edit().putBoolean(KEY_NOTIFY_VIBRATE, value).apply() }

    var showBadge: Boolean
        get() = prefs.getBoolean(KEY_BADGE_SHOW, true)
        set(value) { prefs.edit().putBoolean(KEY_BADGE_SHOW, value).apply() }

    var badgeIncludeMuted: Boolean
        get() = prefs.getBoolean(KEY_BADGE_MUTED, false)
        set(value) { prefs.edit().putBoolean(KEY_BADGE_MUTED, value).apply() }

    // ── Power Saving ─────────────────────────────────────────────────────────

    /** 0 = Off, 1 = Auto (when battery ≤ threshold), 2 = Always on */
    var powerSavingMode: Int
        get() = prefs.getInt(KEY_POWER_SAVING_MODE, 0)
        set(value) { prefs.edit().putInt(KEY_POWER_SAVING_MODE, value).apply() }

    /** Battery % at which auto mode activates (1–99) */
    var powerSavingThreshold: Int
        get() = prefs.getInt(KEY_PS_THRESHOLD, 21)
        set(value) { prefs.edit().putInt(KEY_PS_THRESHOLD, value).apply() }

    var psSaveAnimatedStickers: Boolean
        get() = prefs.getBoolean(KEY_PS_ANIM_STICKERS, true)
        set(value) { prefs.edit().putBoolean(KEY_PS_ANIM_STICKERS, value).apply() }

    var psSaveAnimatedEmoji: Boolean
        get() = prefs.getBoolean(KEY_PS_ANIM_EMOJI, true)
        set(value) { prefs.edit().putBoolean(KEY_PS_ANIM_EMOJI, value).apply() }

    var psSaveEffectsChats: Boolean
        get() = prefs.getBoolean(KEY_PS_EFFECTS_CHATS, true)
        set(value) { prefs.edit().putBoolean(KEY_PS_EFFECTS_CHATS, value).apply() }

    var psSaveAnimationsInCalls: Boolean
        get() = prefs.getBoolean(KEY_PS_ANIM_CALLS, true)
        set(value) { prefs.edit().putBoolean(KEY_PS_ANIM_CALLS, value).apply() }

    var psSaveAutoplayVideos: Boolean
        get() = prefs.getBoolean(KEY_PS_AUTOPLAY_VIDEOS, true)
        set(value) { prefs.edit().putBoolean(KEY_PS_AUTOPLAY_VIDEOS, value).apply() }

    var psSaveAutoplayGifs: Boolean
        get() = prefs.getBoolean(KEY_PS_AUTOPLAY_GIFS, true)
        set(value) { prefs.edit().putBoolean(KEY_PS_AUTOPLAY_GIFS, value).apply() }

    var psSaveParticles: Boolean
        get() = prefs.getBoolean(KEY_PS_PARTICLES, true)
        set(value) { prefs.edit().putBoolean(KEY_PS_PARTICLES, value).apply() }

    var smoothTransitions: Boolean
        get() = prefs.getBoolean(KEY_SMOOTH_TRANSITIONS, true)
        set(value) {
            prefs.edit().putBoolean(KEY_SMOOTH_TRANSITIONS, value).apply()
            smoothTransitionsFlow.value = value
        }

    /** Recomputes isPowerSavingFlow based on current mode and battery level. */
    fun computePowerSaving(context: Context) {
        isPowerSavingFlow.value = when (powerSavingMode) {
            0 -> false
            2 -> true
            else -> {
                val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
                val level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                level in 0..powerSavingThreshold
            }
        }
    }

    // ── Data & Storage ───────────────────────────────────────────────────────

    var autoDownloadMobile: Boolean
        get() = prefs.getBoolean(KEY_AUTO_DL_MOBILE, true)
        set(value) { prefs.edit().putBoolean(KEY_AUTO_DL_MOBILE, value).apply() }

    var autoDownloadWifi: Boolean
        get() = prefs.getBoolean(KEY_AUTO_DL_WIFI, true)
        set(value) { prefs.edit().putBoolean(KEY_AUTO_DL_WIFI, value).apply() }

    var autoDownloadRoaming: Boolean
        get() = prefs.getBoolean(KEY_AUTO_DL_ROAMING, false)
        set(value) { prefs.edit().putBoolean(KEY_AUTO_DL_ROAMING, value).apply() }

    var saveGalleryPrivate: Boolean
        get() = prefs.getBoolean(KEY_SAVE_GALLERY_PRIVATE, false)
        set(value) { prefs.edit().putBoolean(KEY_SAVE_GALLERY_PRIVATE, value).apply() }

    var saveGalleryGroups: Boolean
        get() = prefs.getBoolean(KEY_SAVE_GALLERY_GROUPS, false)
        set(value) { prefs.edit().putBoolean(KEY_SAVE_GALLERY_GROUPS, value).apply() }
}
