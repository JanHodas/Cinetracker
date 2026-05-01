package com.example.cinetracker.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Lightweight TV show shape returned by `/search/multi`.
 *
 * Mirrors [TmdbMovieDto] but uses TV-specific field names (`name` instead of `title`,
 * `first_air_date` instead of `release_date`).
 */
@Serializable
data class TmdbTvDto(
    val id: Int,
    val name: String,
    val overview: String = "",
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("first_air_date") val firstAirDate: String? = null,
    @SerialName("genre_ids") val genreIds: List<Int> = emptyList(),
    @SerialName("vote_average") val voteAverage: Float = 0f,
)
