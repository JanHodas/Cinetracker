package com.example.cinetracker.domain.model

/**
 * A single season of a TV show.
 *
 * [episodes] is empty when only the summary from the TV detail response is
 * available; call the season-detail endpoint to populate the full list.
 */
data class Season(
    val seasonNumber: Int,
    val name: String,
    val overview: String,
    val posterPath: String?,
    val tmdbRating: Float? = null,
    val episodeCount: Int,
    val episodes: List<Episode> = emptyList(),
    val userRating: Float? = null,
)
