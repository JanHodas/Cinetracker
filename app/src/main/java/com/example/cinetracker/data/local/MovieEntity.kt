package com.example.cinetracker.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.cinetracker.domain.model.WatchStatus

/**
 * Room entity representing a movie the user has saved to their personal list.
 *
 * [tmdbId] is used as the natural primary key — it is globally unique and
 * eliminates the need for a synthetic auto-increment id.
 *
 * [genres] is stored as a JSON array of strings via [Converters] (e.g.
 * `["Action","Drama"]`). This is simpler than a full genre-relation table
 * while still being type-safe through the TypeConverter.
 */
@Entity(tableName = "movies")
data class MovieEntity(
    @PrimaryKey val tmdbId: Int,
    val title: String,
    val overview: String,
    val posterPath: String?,
    val backdropPath: String?,
    val releaseDate: String?,
    val genres: List<String>,
    val tmdbRating: Float?,
    val watchStatus: WatchStatus,
    val userRating: Float?,
    val note: String,
    val dateAdded: Long,
)
