package com.example.cinetracker.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Full TV show detail returned by `/tv/{id}`.
 *
 * Contains pre-resolved genres (objects, not IDs) and a list of season summaries.
 */
@Serializable
data class TmdbTvDetailDto(
    val id: Int,
    val name: String,
    val overview: String = "",
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("first_air_date") val firstAirDate: String? = null,
    val genres: List<TmdbGenreDto> = emptyList(),
    @SerialName("vote_average") val voteAverage: Float = 0f,
    @SerialName("number_of_seasons") val numberOfSeasons: Int = 0,
    @SerialName("number_of_episodes") val numberOfEpisodes: Int = 0,
    @SerialName("episode_run_time") val episodeRunTime: List<Int> = emptyList(),
    val seasons: List<TmdbSeasonSummaryDto> = emptyList(),
)

/**
 * Brief season info embedded inside a TV detail response.
 *
 * For full episode listings, call `/tv/{id}/season/{season_number}` separately.
 */
@Serializable
data class TmdbSeasonSummaryDto(
    val id: Int,
    @SerialName("season_number") val seasonNumber: Int,
    val name: String,
    val overview: String = "",
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("episode_count") val episodeCount: Int = 0,
    @SerialName("air_date") val airDate: String? = null,
)
