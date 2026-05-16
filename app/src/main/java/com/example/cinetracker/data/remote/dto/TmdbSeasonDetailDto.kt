package com.example.cinetracker.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Full season detail returned by `/tv/{id}/season/{season_number}`.
 *
 * Contains the full episode list with individual runtimes and overviews.
 */
@Serializable
data class TmdbSeasonDetailDto(
    val id: Int,
    @SerialName("season_number") val seasonNumber: Int,
    val name: String,
    val overview: String = "",
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("vote_average") val voteAverage: Float = 0f,
    val episodes: List<TmdbEpisodeDto> = emptyList(),
)
