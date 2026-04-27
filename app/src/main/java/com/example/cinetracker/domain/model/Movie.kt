package com.example.cinetracker.domain.model

/**
 * Domain representation of a movie. Independent of TMDB DTO shape and Room entity shape.
 *
 * @property tmdbId TMDB-assigned identifier; serves as the natural primary key everywhere.
 * @property posterPath relative TMDB image path (e.g. "/abc.jpg"); use [com.example.cinetracker.domain.util.TmdbImageUrl]
 *           to resolve the full URL.
 * @property releaseDate ISO date string in YYYY-MM-DD as returned by TMDB; null when unknown.
 * @property genres genre names already resolved (TMDB returns ids in search results, names in detail).
 * @property tmdbRating average user rating from TMDB on a 0-10 scale, or null when not yet rated.
 */
data class Movie(
    val tmdbId: Int,
    val title: String,
    val overview: String,
    val posterPath: String?,
    val backdropPath: String?,
    val releaseDate: String?,
    val genres: List<String>,
    val tmdbRating: Float?,
)
