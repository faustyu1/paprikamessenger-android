package ru.faustyu.paprika

import android.app.Application
import coil.Coil
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import okhttp3.Cache
import ru.faustyu.paprika.data.network.NetworkModule
import java.io.File

class PaprikaApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // ── OkHttp HTTP cache (20 MB) ─────────────────────────────────────
        val httpCacheDir = File(cacheDir, "http_cache")
        val httpCache = Cache(httpCacheDir, 20L * 1024 * 1024)
        NetworkModule.initCache(httpCache)

        // ── Coil: 100 MB disk cache + 32 MB memory cache ─────────────────
        val imageLoader = ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.15) // 15% of available RAM
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(File(cacheDir, "image_cache"))
                    .maxSizeBytes(100L * 1024 * 1024)
                    .build()
            }
            .crossfade(true)
            .build()

        Coil.setImageLoader(imageLoader)
    }
}
