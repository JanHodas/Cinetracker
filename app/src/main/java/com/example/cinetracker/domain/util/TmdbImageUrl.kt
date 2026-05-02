package com.example.cinetracker.domain.util

/**
 * Resolves TMDB image paths (relative paths like "/abc.jpg") into full HTTPS URLs.
 *
 * TMDB hosts images under https://image.tmdb.org/t/p/{size}{path}; the `{size}` segment
 * controls width. See https://developer.themoviedb.org/docs/image-basics for the size catalog.
 */
object TmdbImageUrl {
    private const val BASE = "https://image.tmdb.org/t/p"

    /** Standard poster width used in list items. */
    const val POSTER_W342 = "w342"

    /** Larger poster used in the detail header. */
    const val POSTER_W500 = "w500"

    /** Wide backdrop used as the detail screen header background. */
    const val BACKDROP_W780 = "w780"

    /** Profile photo used for cast members. */
    const val PROFILE_W185 = "w185"

    /**
     * Builds the full image URL for the given TMDB relative path, or null if [relativePath] is null/blank.
     */
    fun build(relativePath: String?, size: String = POSTER_W342): String? {
        if (relativePath.isNullOrBlank()) return null
        return "$BASE/$size$relativePath"
    }
}
