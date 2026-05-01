package com.example.cinetracker.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Single episode within a season detail response. */
@Serializable
data class TmdbEpisodeDto(
    val id: Int,
    @SerialName("episode_number") val episodeNumber: Int,
    val name: String,
    val overview: String = "",
    @SerialName("air_date") val airDate: String? = null,
    val runtime: Int? = null,
    @SerialName("still_path") val stillPath: String? = null,
)
