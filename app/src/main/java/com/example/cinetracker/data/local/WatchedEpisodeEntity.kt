package com.example.cinetracker.data.local

import androidx.room.Entity

/**
 * Tracks a single watched episode for a TV show.
 *
 * The composite primary key (tmdbId + seasonNumber + episodeNumber) ensures
 * that each episode can only be marked once per show.
 */
@Entity(
    tableName = "watched_episodes",
    primaryKeys = ["tmdbId", "seasonNumber", "episodeNumber"],
)
data class WatchedEpisodeEntity(
    /** TMDB ID of the parent TV show. */
    val tmdbId: Int,
    val seasonNumber: Int,
    val episodeNumber: Int,
    /** Epoch millis when the episode was marked as watched. */
    val watchedAt: Long = System.currentTimeMillis(),
    /** Episode runtime in minutes (from TMDB). Null if unknown. */
    val runtime: Int? = null,
)
