package ru.faustyu.paprika.analytics

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Analytics helper for tracking user events
 * Uses Firebase Analytics
 */
@Singleton
class AnalyticsHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val analytics: FirebaseAnalytics by lazy {
        Firebase.analytics
    }
    
    /**
     * Log user login event
     */
    fun logLogin(method: String = "email") {
        analytics.logEvent(FirebaseAnalytics.Event.LOGIN) {
            param(FirebaseAnalytics.Param.METHOD, method)
        }
    }
    
    /**
     * Log user signup event
     */
    fun logSignUp(method: String = "email") {
        analytics.logEvent(FirebaseAnalytics.Event.SIGN_UP) {
            param(FirebaseAnalytics.Param.METHOD, method)
        }
    }
    
    /**
     * Log message sent event
     */
    fun logMessageSent(chatType: String, messageType: String) {
        analytics.logEvent("message_sent") {
            param("chat_type", chatType)
            param("message_type", messageType)
        }
    }
    
    /**
     * Log chat created event
     */
    fun logChatCreated(chatType: String, memberCount: Int) {
        analytics.logEvent("chat_created") {
            param("chat_type", chatType)
            param("member_count", memberCount.toLong())
        }
    }
    
    /**
     * Log media upload event
     */
    fun logMediaUpload(mediaType: String, sizeBytes: Long) {
        analytics.logEvent("media_upload") {
            param("media_type", mediaType)
            param("size_bytes", sizeBytes)
        }
    }
    
    /**
     * Log screen view event
     */
    fun logScreenView(screenName: String, screenClass: String) {
        analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
            param(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            param(FirebaseAnalytics.Param.SCREEN_CLASS, screenClass)
        }
    }
    
    /**
     * Log custom event
     */
    fun logEvent(eventName: String, params: Map<String, Any>? = null) {
        analytics.logEvent(eventName) {
            params?.forEach { (key, value) ->
                when (value) {
                    is String -> param(key, value)
                    is Long -> param(key, value)
                    is Int -> param(key, value.toLong())
                    is Double -> param(key, value)
                    is Boolean -> param(key, if (value) 1L else 0L)
                }
            }
        }
    }
    
    /**
     * Set user property
     */
    fun setUserProperty(name: String, value: String) {
        analytics.setUserProperty(name, value)
    }
    
    /**
     * Set user ID
     */
    fun setUserId(userId: String) {
        analytics.setUserId(userId)
    }
}

/**
 * Extension function for building analytics events
 */
private inline fun FirebaseAnalytics.logEvent(
    event: String,
    block: Bundle.() -> Unit
) {
    val bundle = Bundle()
    bundle.block()
    logEvent(event, bundle)
}

/**
 * Extension functions for Bundle to add params easily
 */
private fun Bundle.param(key: String, value: String) = putString(key, value)
private fun Bundle.param(key: String, value: Long) = putLong(key, value)
private fun Bundle.param(key: String, value: Double) = putDouble(key, value)
