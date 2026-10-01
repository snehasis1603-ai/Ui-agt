package com.example.network

import com.example.BuildConfig
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class AuthRequest(
    val email: String,
    val password: String,
    val returnSecureToken: Boolean = true
)

@JsonClass(generateAdapter = true)
data class AuthResponse(
    val localId: String?,
    val email: String?,
    val idToken: String?,
    val refreshToken: String?,
    val expiresIn: String?
)

interface FirebaseAuthService {
    @POST("v1/accounts:signInWithPassword")
    suspend fun signInWithPassword(
        @Query("key") apiKey: String = BuildConfig.GEMINI_API_KEY,
        @Body request: AuthRequest
    ): AuthResponse

    @POST("v1/accounts:signUp")
    suspend fun signUp(
        @Query("key") apiKey: String = BuildConfig.GEMINI_API_KEY,
        @Body request: AuthRequest
    ): AuthResponse
}

object FirebaseAuthClient {
    private const val BASE_URL = "https://identitytoolkit.googleapis.com/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(NetworkErrorLoggingInterceptor())
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    val service: FirebaseAuthService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(FirebaseAuthService::class.java)
    }
}
