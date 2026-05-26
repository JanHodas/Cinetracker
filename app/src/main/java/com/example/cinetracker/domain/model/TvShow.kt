package com.example.cinetracker.domain.model

/**
 * Domain representation of a TV show. Mirrors [Movie] structure via the shared
 * [MediaItem] interface but adds TV-specific metadata.
 *
 * @property title mapped from TMDB's `name` field.
 * @property releaseDate mapped from TMDB's `first_air_date`.
 * @property numberOfSeasons total season count as reported by TMDB.
 * @property numberOfEpisodes total episode count across all seasons.
 */
data class TvShow(
    override val tmdbId: Int,
    override val title: String,
    override val overview: String,
    override val posterPath: String?,
    override val backdropPath: String?,
    override val releaseDate: String?,
    override val genres: List<String>,
    override val tmdbRating: Float?,
    val numberOfSeasons: Int,
    val numberOfEpisodes: Int,
    val episodeRunTime: Int? = null,
    val seasonEpisodeCounts: List<SeasonEpisodeCount> = emptyList(),
) : MediaItem
