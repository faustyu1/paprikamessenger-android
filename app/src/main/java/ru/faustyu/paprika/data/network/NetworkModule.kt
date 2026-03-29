package ru.faustyu.paprika.data.network

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import ru.faustyu.paprika.util.CryptoManager

object NetworkModule {
    var baseUrl = "https://paprikaapi.faustyu.xyz/"

    private var _api: ApiService? = null

    var authToken: String? = null

    /** Username saved on login/register — used for silent token refresh */
    var savedUsername: String? = null

    /** Called after a silent token refresh so MainActivity can persist the new token */
    var onTokenRefreshed: ((String) -> Unit)? = null

    // ── Bare client for auth calls (no auth interceptor, no authenticator — avoids loops) ──
    private val bareClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .build()
    }

    private fun createBareApi(): ApiService =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(bareClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)

    // ── Token Authenticator: silent re-auth on 401 ─────────────────────────
    private val tokenAuthenticator = okhttp3.Authenticator { _, response ->
        // Already retried once → give up to avoid infinite loop
        if (response.priorResponse?.code == 401) return@Authenticator null

        val username = savedUsername ?: return@Authenticator null

        val newToken = runBlocking {
            try {
                val api = createBareApi()
                val challengeResp = api.createChallenge(ChallengeRequest(username))
                if (!challengeResp.isSuccessful) return@runBlocking null
                val data = challengeResp.body() ?: return@runBlocking null
                val signature = CryptoManager.signChallenge(data.challenge)
                val verifyResp = api.verifyChallenge(VerifyRequest(data.challenge_id, signature))
                verifyResp.body()?.token
            } catch (e: Exception) {
                null
            }
        } ?: return@Authenticator null

        authToken = newToken
        onTokenRefreshed?.invoke(newToken)

        response.request.newBuilder()
            .header("Authorization", "Bearer $newToken")
            .build()
    }

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
            .authenticator(tokenAuthenticator)
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
        _api = null
    }

    fun getCurrentUrl(): String = baseUrl

    private fun createRetrofit(): ApiService =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
}
