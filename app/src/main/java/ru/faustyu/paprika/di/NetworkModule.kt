package ru.faustyu.paprika.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.HttpUrl.Companion.toHttpUrl
import ru.faustyu.paprika.BuildConfig
import ru.faustyu.paprika.data.network.ApiService
import ru.faustyu.paprika.util.Constants
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import javax.inject.Qualifier

/**
 * Qualifier for auth token
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AuthToken

/**
 * Qualifier for base URL
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class BaseUrl

/**
 * Hilt module providing network-related dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    
    @Provides
    @Singleton
    @BaseUrl
    fun provideBaseUrl(): String {
        return "http://localhost:8080/" // Placeholder, changed by interceptor
    }
    
    @Provides
    @Singleton
    fun provideHostSelectionInterceptor(prefsManager: ru.faustyu.paprika.data.PrefsManager): HostSelectionInterceptor {
        return HostSelectionInterceptor(prefsManager)
    }

    @Provides
    @Singleton
    fun provideAuthInterceptor(prefsManager: ru.faustyu.paprika.data.PrefsManager): AuthInterceptor {
        return AuthInterceptor(prefsManager)
    }
    
    /**
     * Provides OkHttpClient with logging and auth interceptor
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(
        hostSelectionInterceptor: HostSelectionInterceptor,
        authInterceptor: AuthInterceptor
    ): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        
        return OkHttpClient.Builder()
            .addInterceptor(hostSelectionInterceptor)
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(Constants.NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(Constants.NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(Constants.NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
    }
    
    /**
     * Provides Retrofit instance
     */
    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        @BaseUrl baseUrl: String
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    /**
     * Provides ApiService
     */
    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService {
        return retrofit.create(ApiService::class.java)
    }
}

/**
 * Interceptor that changes the base URL at runtime based on PrefsManager
 */
class HostSelectionInterceptor @javax.inject.Inject constructor(
    private val prefsManager: ru.faustyu.paprika.data.PrefsManager
) : okhttp3.Interceptor {
    override fun intercept(chain: okhttp3.Interceptor.Chain): okhttp3.Response {
        var request = chain.request()
        val customUrl = prefsManager.backendUrl
        
        if (customUrl != null) {
            val newUrl = customUrl.toHttpUrl()
            val newFullUrl = request.url.newBuilder()
                .scheme(newUrl.scheme)
                .host(newUrl.host)
                .port(newUrl.port)
                .build()
            request = request.newBuilder()
                .url(newFullUrl)
                .build()
        }
        
        return chain.proceed(request)
    }
}

/**
 * Interceptor that adds Auth token to requests if available
 */
class AuthInterceptor @javax.inject.Inject constructor(
    private val prefsManager: ru.faustyu.paprika.data.PrefsManager
) : okhttp3.Interceptor {
    override fun intercept(chain: okhttp3.Interceptor.Chain): okhttp3.Response {
        val original = chain.request()
        val token = prefsManager.token
        
        if (token.isNullOrBlank()) {
            return chain.proceed(original)
        }
        
        val request = original.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()
            
        return chain.proceed(request)
    }
}
