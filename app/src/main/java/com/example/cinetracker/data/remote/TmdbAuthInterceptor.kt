package com.example.cinetracker.data.remote

import okhttp3.Interceptor
import okhttp3.Response

/**
 * OkHttp interceptor that attaches the TMDB v4 bearer token and a JSON Accept header to
 * every request. The token never appears in source code; it is read at build time from
 * `local.properties` and surfaced via `BuildConfig.TMDB_BEARER_TOKEN`.
 */
class TmdbAuthInterceptor(
    private val bearerToken: String,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val authorized = chain.request().newBuilder()
            .addHeader("Authorization", "Bearer $bearerToken")
            .addHeader("Accept", "application/json")
            .build()
        return chain.proceed(authorized)
    }
}
