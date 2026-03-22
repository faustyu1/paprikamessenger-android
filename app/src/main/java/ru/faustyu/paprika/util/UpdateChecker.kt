package ru.faustyu.paprika.util

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

data class GitHubRelease(
    @SerializedName("tag_name") val tagName: String,
    @SerializedName("name") val name: String,
    @SerializedName("body") val body: String?,
    @SerializedName("assets") val assets: List<GitHubAsset>
)

data class GitHubAsset(
    @SerializedName("name") val name: String,
    @SerializedName("browser_download_url") val downloadUrl: String
)

object UpdateChecker {
    private val client = OkHttpClient()
    private val gson = Gson()
    private const val RELEASES_URL =
        "https://api.github.com/repos/faustyu1/paprikamessenger-android/releases/latest"

    suspend fun checkForUpdate(currentVersion: String): GitHubRelease? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(RELEASES_URL)
                .addHeader("Accept", "application/vnd.github.v3+json")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null
                val release = gson.fromJson(body, GitHubRelease::class.java)
                val latestVersion = release.tagName.trimStart('v')
                if (isNewerVersion(latestVersion, currentVersion.trimStart('v'))) release else null
            }
        } catch (_: Exception) {
            null
        }
    }

    fun apkAsset(release: GitHubRelease): GitHubAsset? =
        release.assets.firstOrNull { it.name.endsWith(".apk") }

    private fun isNewerVersion(latest: String, current: String): Boolean {
        val l = latest.split(".").mapNotNull { it.toIntOrNull() }
        val c = current.split(".").mapNotNull { it.toIntOrNull() }
        for (i in 0 until maxOf(l.size, c.size)) {
            val lv = l.getOrElse(i) { 0 }
            val cv = c.getOrElse(i) { 0 }
            if (lv > cv) return true
            if (lv < cv) return false
        }
        return false
    }
}
