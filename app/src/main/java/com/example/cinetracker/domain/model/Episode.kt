package com.example.cinetracker.domain.model

/**
 * A single episode within a [Season].
 *
 * @property runtime episode length in minutes, or `null` if TMDB does not have the data.
 * @property airDate ISO date string (YYYY-MM-DD) of the original air date.
 */
data class Episode(
    val episodeNumber: Int,
    val name: String,
    val overview: String,
    val airDate: String?,
    val runtime: Int?,
)
