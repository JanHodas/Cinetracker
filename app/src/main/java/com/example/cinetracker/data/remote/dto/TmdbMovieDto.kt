package com.example.cinetracker.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Lightweight movie shape returned by `/search/movie` and embedded inside paginated lists.
 *
 * Note that genres come as a list of integer IDs here; the human-readable names must be looked
 * up via [TmdbGenreListDto] (cached by the repository).
 */
@Serializable
data class TmdbMovieDto(
    val id: Int,
    val title: String,
    val overview: String = "",
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("genre_ids") val genreIds: List<Int> = emptyList(),
    @SerialName("vote_average") val voteAverage: Float = 0f,
)
