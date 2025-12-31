package ru.faustyu.paprika.data.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object NetworkModule {
    // Default to emulator localhost
    var baseUrl = "http://localhost:8080/"
    
    // Backing field for the api service
    private var _api: ApiService? = null

    // Token for authenticated requests
    var authToken: String? = null

    private val client by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        val authInterceptor = okhttp3.Interceptor { chain ->
            val requestBuilder = chain.request().newBuilder()
            authToken?.let { token ->
                requestBuilder.addHeader("Authorization", "Bearer $token")
            }
            chain.proceed(requestBuilder.build())
        }

        OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor(authInterceptor)
            .build()
    }

    val api: ApiService
        get() {
            if (_api == null) {
                _api = createRetrofit()
            }
            return _api!!
        }

    fun setCustomUrl(url: String) {
        var newUrl = url
        if (!newUrl.startsWith("http://") && !newUrl.startsWith("https://")) {
            newUrl = "http://$newUrl"
        }
        if (!newUrl.endsWith("/")) {
            newUrl += "/"
        }
        baseUrl = newUrl
        _api = null // Invalidate existing instance
    }
    
    fun getCurrentUrl(): String = baseUrl

    private fun createRetrofit(): ApiService {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
