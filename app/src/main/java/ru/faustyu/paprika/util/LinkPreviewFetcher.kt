package ru.faustyu.paprika.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

data class LinkPreviewData(
    val url: String,
    val title: String,
    val description: String? = null,
    val imageUrl: String? = null
)

object LinkPreviewFetcher {
    private val cache = ConcurrentHashMap<String, LinkPreviewData?>()

    suspend fun fetch(url: String): LinkPreviewData? {
        if (cache.containsKey(url)) return cache[url]
        return withContext(Dispatchers.IO) {
            try {
                val conn = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                conn.connectTimeout = 4000
                conn.readTimeout = 4000
                conn.setRequestProperty("User-Agent", "Mozilla/5.0")
                conn.instanceFollowRedirects = true
                val html = conn.inputStream.bufferedReader().readText().take(32768)
                conn.disconnect()
                parseOgTags(url, html).also { cache[url] = it }
            } catch (_: Exception) {
                cache[url] = null
                null
            }
        }
    }

    private fun parseOgTags(url: String, html: String): LinkPreviewData? {
        fun meta(prop: String): String? {
            val r1 = Regex("""<meta[^>]+property=["']og:$prop["'][^>]+content=["']([^"'<>]+)["']""", RegexOption.IGNORE_CASE)
            val r2 = Regex("""<meta[^>]+content=["']([^"'<>]+)["'][^>]+property=["']og:$prop["']""", RegexOption.IGNORE_CASE)
            return r1.find(html)?.groupValues?.get(1) ?: r2.find(html)?.groupValues?.get(1)
        }
        val title = meta("title") ?: Regex("<title[^>]*>([^<]+)</title>", RegexOption.IGNORE_CASE)
            .find(html)?.groupValues?.get(1) ?: return null
        return LinkPreviewData(
            url = url,
            title = title.trim().take(100),
            description = meta("description")?.trim()?.take(200),
            imageUrl = meta("image")
        )
    }
}
