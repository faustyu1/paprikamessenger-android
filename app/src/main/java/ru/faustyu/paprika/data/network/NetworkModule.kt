package ru.faustyu.paprika.data.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object NetworkModule {
    var baseUrl = "https://paprikaapi.faustyu.xyz/"
    
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
            .protocols(listOf(okhttp3.Protocol.HTTP_1_1))
            .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
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
            newUrl = "https://$newUrl"
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
