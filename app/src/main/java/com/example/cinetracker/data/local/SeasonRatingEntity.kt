package com.example.cinetracker.data.local

import androidx.room.Entity

/** Local user rating for a specific TV season. */
@Entity(
    tableName = "season_ratings",
    primaryKeys = ["tmdbId", "seasonNumber"],
)
data class SeasonRatingEntity(
    val tmdbId: Int,
    val seasonNumber: Int,
    val userRating: Float?,
    val ratedAt: Long = System.currentTimeMillis(),
)
