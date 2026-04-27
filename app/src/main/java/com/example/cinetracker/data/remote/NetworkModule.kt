package com.example.cinetracker.data.remote

import com.example.cinetracker.BuildConfig
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Singleton-style factory that wires the OkHttp + Retrofit stack used to call TMDB.
 *
 * Properties are `lazy` so the client is built on first access and shared from then on. The
 * underlying [OkHttpClient] is exposed separately so other libraries (e.g. Coil) can re-use
 * the same connection pool and dispatcher.
 */
object NetworkModule {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
    }

    private val loggingInterceptor: HttpLoggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }

    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(TmdbAuthInterceptor(BuildConfig.TMDB_BEARER_TOKEN))
            .addInterceptor(loggingInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(TmdbApi.BASE_URL)
            // Pass an explicit Call.Factory so Retrofit doesn't rebuild the client.
            .callFactory(okHttpClient as Call.Factory)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    val tmdbApi: TmdbApi by lazy { retrofit.create(TmdbApi::class.java) }
}
