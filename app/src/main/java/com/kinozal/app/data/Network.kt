package com.kinozal.app.data

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

object Network {
    val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
    }

    private val baseClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * TMDB принимает либо ключ API v3 (параметр api_key), либо токен чтения v4 (заголовок Bearer).
     * Токен v4 — это JWT, он начинается с "eyJ".
     */
    fun tmdbApi(settings: SettingsRepository): TmdbApi {
        val auth = Interceptor { chain ->
            val key = runBlocking { settings.currentTmdbKey() }
            val original = chain.request()
            val request = when {
                key.isEmpty() -> original
                key.startsWith("eyJ") -> original.newBuilder()
                    .header("Authorization", "Bearer $key")
                    .build()
                else -> original.newBuilder()
                    .url(original.url.newBuilder().addQueryParameter("api_key", key).build())
                    .build()
            }
            chain.proceed(request)
        }
        return Retrofit.Builder()
            .baseUrl("https://api.themoviedb.org/3/")
            .client(baseClient.newBuilder().addInterceptor(auth).build())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(TmdbApi::class.java)
    }

    fun archiveApi(): ArchiveApi = Retrofit.Builder()
        .baseUrl("https://archive.org/")
        .client(baseClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(ArchiveApi::class.java)
}
