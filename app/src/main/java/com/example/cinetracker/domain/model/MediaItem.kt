package com.example.cinetracker.domain.model

/**
 * Common contract for displayable media (movies and TV shows).
 *
 * Both [Movie] and [TvShow] implement this interface, which allows the search
 * result list, the personal watchlist, and reusable UI components to handle
 * either type without `when`-branching on every property access.
 *
 * Type-specific data (e.g. [TvShow.numberOfSeasons]) lives on the concrete
 * subtype and is accessed via smart-casts where needed.
 */
sealed interface MediaItem {
    val tmdbId: Int
    val title: String
    val overview: String
    val posterPath: String?
    val backdropPath: String?
    val releaseDate: String?
    val genres: List<String>
    val tmdbRating: Float?
}
