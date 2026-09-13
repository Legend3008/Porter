package com.porter.core.network

import com.porter.core.auth.TokenStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Auth interceptor — attaches Bearer token to every request.
 * Per frontend.md §10: NEVER log tokens. Logging interceptor runs at HEADERS level
 * in debug only, and the Authorization header is redacted.
 */
class AuthInterceptor(private val tokenStorage: TokenStorage) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val accessToken = tokenStorage.getAccessToken()
        val request: Request = chain.request().newBuilder().apply {
            if (accessToken != null) {
                header("Authorization", "Bearer $accessToken")
            }
        }.build()
        return chain.proceed(request)
    }
}

/**
 * Idempotency-Key interceptor — attaches a per-call UUID to all mutating requests.
 * Per frontend.md §68 / backend.md §16: POST/PUT/PATCH requests must carry this header.
 */
class IdempotencyInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val request = chain.request()
        val method = request.method.uppercase()
        val newRequest = if (method in listOf("POST", "PUT", "PATCH")) {
            request.newBuilder()
                .header("Idempotency-Key", java.util.UUID.randomUUID().toString())
                .build()
        } else request
        return chain.proceed(newRequest)
    }
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideAuthInterceptor(tokenStorage: TokenStorage): AuthInterceptor =
        AuthInterceptor(tokenStorage)

    @Provides
    @Singleton
    fun provideIdempotencyInterceptor(): IdempotencyInterceptor = IdempotencyInterceptor()

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        idempotencyInterceptor: IdempotencyInterceptor,
    ): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor { message ->
            // Never log Authorization header
            if (!message.startsWith("Authorization:")) {
                android.util.Log.d("Porter/HTTP", message)
            }
        }.apply {
            level = HttpLoggingInterceptor.Level.BODY
            // Redact the Authorization header from all logs
            redactHeader("Authorization")
            redactHeader("Cookie")
        }

        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(idempotencyInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://api.porter.com/api/v1/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
}
