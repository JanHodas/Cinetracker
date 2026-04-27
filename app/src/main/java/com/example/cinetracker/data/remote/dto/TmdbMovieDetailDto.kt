package com.example.cinetracker.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Richer movie shape returned by `/movie/{id}`. Genres come pre-resolved as objects (not just IDs)
 * which means callers of the detail endpoint don't need the genre lookup table.
 */
@Serializable
data class TmdbMovieDetailDto(
    val id: Int,
    val title: String,
    val overview: String = "",
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    val genres: List<TmdbGenreDto> = emptyList(),
    @SerialName("vote_average") val voteAverage: Float = 0f,
    val runtime: Int? = null,
    val tagline: String? = null,
)
