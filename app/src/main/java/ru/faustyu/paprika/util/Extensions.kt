package ru.faustyu.paprika.util

import ru.faustyu.paprika.data.network.NetworkModule
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Extension function to convert relative or absolute image URL to full URL
 */
fun String?.toFullImageUrl(): String? {
    if (this == null) return null
    if (startsWith("http://") || startsWith("https://")) return this
    
    val baseUrl = NetworkModule.baseUrl.removeSuffix("/")
    return "$baseUrl$this"
}

/**
 * Extension function to add cache-busting timestamp to image URL
 */
fun String.withCacheBuster(): String {
    val separator = if (contains("?")) "&" else "?"
    return "$this${separator}t=${System.currentTimeMillis()}"
}

/**
 * Format Unix timestamp to readable date/time
 */
fun Long.toFormattedDateTime(): String {
    return try {
        val dateTime = LocalDateTime.ofInstant(
            Instant.ofEpochSecond(this),
            ZoneId.systemDefault()
        )
        DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").format(dateTime)
    } catch (e: Exception) {
        ""
    }
}

/**
 * Format Unix timestamp to time only
 */
fun Long.toFormattedTime(): String {
    return try {
        val dateTime = LocalDateTime.ofInstant(
            Instant.ofEpochSecond(this),
            ZoneId.systemDefault()
        )
        DateTimeFormatter.ofPattern("HH:mm").format(dateTime)
    } catch (e: Exception) {
        ""
    }
}

/**
 * Parse ISO 8601 timestamp to Unix seconds, with fallback
 */
fun String.parseToUnixSeconds(fallback: Long = 0L): Long {
    return try {
        Instant.parse(this).epochSecond
    } catch (e: Exception) {
        fallback
    }
}
