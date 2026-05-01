package com.example.cinetracker.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Shared credits payload returned by both movie and TV credits endpoints. */
@Serializable
data class TmdbCreditsDto(
    val cast: List<TmdbCastMemberDto> = emptyList(),
)

/** Single cast member entry from TMDB credits. */
@Serializable
data class TmdbCastMemberDto(
    val id: Int,
    val name: String,
    val character: String? = null,
    @SerialName("profile_path") val profilePath: String? = null,
    @SerialName("order") val order: Int = Int.MAX_VALUE,
)
