package com.example.cinetracker.data.remote

import com.example.cinetracker.data.remote.dto.TmdbGenreListDto
import com.example.cinetracker.data.remote.dto.TmdbMovieDetailDto
import com.example.cinetracker.data.remote.dto.TmdbMultiSearchResponseDto
import com.example.cinetracker.data.remote.dto.TmdbSearchResponseDto
import com.example.cinetracker.data.remote.dto.TmdbSeasonDetailDto
import com.example.cinetracker.data.remote.dto.TmdbTvDetailDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit-backed TMDB v3 client. Authentication is supplied transparently via
 * [TmdbAuthInterceptor]; callers must not pass tokens here.
 */
interface TmdbApi {

    @GET("search/movie")
    suspend fun searchMovies(
        @Query("query") query: String,
        @Query("language") language: String = DEFAULT_LANGUAGE,
        @Query("page") page: Int = 1,
        @Query("include_adult") includeAdult: Boolean = false,
    ): TmdbSearchResponseDto

    @GET("movie/{id}")
    suspend fun getMovieDetail(
        @Path("id") tmdbId: Int,
        @Query("language") language: String = DEFAULT_LANGUAGE,
    ): TmdbMovieDetailDto

    @GET("genre/movie/list")
    suspend fun getGenres(
        @Query("language") language: String = DEFAULT_LANGUAGE,
    ): TmdbGenreListDto

    // --- TV series endpoints ---

    /** Combined movie + TV search. Results are polymorphic — see [TmdbMultiSearchResponseDto]. */
    @GET("search/multi")
    suspend fun searchMulti(
        @Query("query") query: String,
        @Query("language") language: String = DEFAULT_LANGUAGE,
        @Query("page") page: Int = 1,
        @Query("include_adult") includeAdult: Boolean = false,
    ): TmdbMultiSearchResponseDto

    @GET("tv/{id}")
    suspend fun getTvDetail(
        @Path("id") tmdbId: Int,
        @Query("language") language: String = DEFAULT_LANGUAGE,
    ): TmdbTvDetailDto

    @GET("tv/{id}/season/{seasonNumber}")
    suspend fun getTvSeason(
        @Path("id") tvId: Int,
        @Path("seasonNumber") seasonNumber: Int,
        @Query("language") language: String = DEFAULT_LANGUAGE,
    ): TmdbSeasonDetailDto

    @GET("genre/tv/list")
    suspend fun getTvGenres(
        @Query("language") language: String = DEFAULT_LANGUAGE,
    ): TmdbGenreListDto

    companion object {
        const val BASE_URL = "https://api.themoviedb.org/3/"
        const val DEFAULT_LANGUAGE = "sk-SK"
    }
}
