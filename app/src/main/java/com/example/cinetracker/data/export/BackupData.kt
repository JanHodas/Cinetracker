package com.example.cinetracker.data.export

import kotlinx.serialization.Serializable

@Serializable
data class BackupData(
    val version: Int = CURRENT_VERSION,
    val movies: List<BackupMovie> = emptyList(),
    val watchedEpisodes: List<BackupWatchedEpisode> = emptyList(),
) {
    companion object {
        const val CURRENT_VERSION = 1
    }
}

@Serializable
data class BackupMovie(
    val tmdbId: Int,
    val title: String,
    val overview: String = "",
    val posterPath: String? = null,
    val backdropPath: String? = null,
    val releaseDate: String? = null,
    val genres: List<String> = emptyList(),
    val tmdbRating: Float? = null,
    val watchStatus: String,
    val userRating: Float? = null,
    val note: String = "",
    val dateAdded: Long,
    val mediaType: String = "movie",
    val numberOfSeasons: Int? = null,
    val numberOfEpisodes: Int? = null,
    val runtime: Int? = null,
)

@Serializable
data class BackupWatchedEpisode(
    val tmdbId: Int,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val watchedAt: Long,
    val runtime: Int? = null,
)
