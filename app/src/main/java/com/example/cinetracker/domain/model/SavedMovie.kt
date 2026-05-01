package com.example.cinetracker.domain.model

/**
 * An item from the user's personal list — combines the TMDB metadata ([movie])
 * with user-specific fields that live only in the local Room database.
 *
 * Despite the name, [movie] can hold either a [Movie] or a [TvShow] — both
 * implement [MediaItem]. The class name is kept for backward compatibility.
 *
 * @property movie The underlying media item (movie or TV show).
 * @property userRating The user's own 1–10 rating, or `null` if not yet rated.
 *           Only meaningful when [watchStatus] is [WatchStatus.WATCHED].
 * @property note Free-form personal note (e.g. "Watch with friends").
 * @property dateAdded Epoch millis when the item was first added to the list.
 */
data class SavedMovie(
    val movie: MediaItem,
    val watchStatus: WatchStatus,
    val userRating: Float?,
    val note: String,
    val dateAdded: Long,
)
