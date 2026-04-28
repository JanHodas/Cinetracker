package com.example.cinetracker.domain.model

/**
 * A movie from the user's personal list — combines the TMDB metadata ([movie])
 * with user-specific fields that live only in the local Room database.
 *
 * This separation keeps [Movie] a pure "network/search" model while
 * [SavedMovie] represents "something the user explicitly saved".
 *
 * @property userRating The user's own 1–10 rating, or `null` if not yet rated.
 *           Only meaningful when [watchStatus] is [WatchStatus.WATCHED].
 * @property note Free-form personal note (e.g. "Watch with friends").
 * @property dateAdded Epoch millis when the movie was first added to the list.
 */
data class SavedMovie(
    val movie: Movie,
    val watchStatus: WatchStatus,
    val userRating: Float?,
    val note: String,
    val dateAdded: Long,
)
