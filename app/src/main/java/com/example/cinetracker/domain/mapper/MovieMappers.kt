package com.example.cinetracker.domain.mapper

import com.example.cinetracker.data.local.MovieEntity
import com.example.cinetracker.data.remote.dto.TmdbMovieDetailDto
import com.example.cinetracker.data.remote.dto.TmdbMovieDto
import com.example.cinetracker.domain.model.Movie
import com.example.cinetracker.domain.model.SavedMovie
import com.example.cinetracker.domain.model.WatchStatus

/**
 * Converts a search-result DTO into the canonical [Movie] domain model.
 * Genre IDs are resolved to human-readable names using [genreNamesById]; unknown ids are dropped.
 */
fun TmdbMovieDto.toDomain(genreNamesById: Map<Int, String>): Movie = Movie(
    tmdbId = id,
    title = title,
    overview = overview,
    posterPath = posterPath,
    backdropPath = backdropPath,
    releaseDate = releaseDate?.takeIf { it.isNotBlank() },
    genres = genreIds.mapNotNull { genreNamesById[it] },
    tmdbRating = voteAverage.takeIf { it > 0f },
)

/**
 * Converts a movie-detail DTO into the canonical [Movie] domain model.
 * Detail responses already carry genre objects, so no lookup table is needed.
 */
fun TmdbMovieDetailDto.toDomain(): Movie = Movie(
    tmdbId = id,
    title = title,
    overview = overview,
    posterPath = posterPath,
    backdropPath = backdropPath,
    releaseDate = releaseDate?.takeIf { it.isNotBlank() },
    genres = genres.map { it.name },
    tmdbRating = voteAverage.takeIf { it > 0f },
)

// ── Entity ↔ Domain ─────────────────────────────────────────────────

/** Converts a Room [MovieEntity] to the domain [SavedMovie] wrapper. */
fun MovieEntity.toDomain(): SavedMovie = SavedMovie(
    movie = Movie(
        tmdbId = tmdbId,
        title = title,
        overview = overview,
        posterPath = posterPath,
        backdropPath = backdropPath,
        releaseDate = releaseDate,
        genres = genres,
        tmdbRating = tmdbRating,
    ),
    watchStatus = watchStatus,
    userRating = userRating,
    note = note,
    dateAdded = dateAdded,
)

/**
 * Converts a domain [Movie] into a Room [MovieEntity] ready for insertion.
 * User-specific fields are supplied as parameters because [Movie] itself
 * does not carry them.
 */
fun Movie.toEntity(
    watchStatus: WatchStatus,
    userRating: Float? = null,
    note: String = "",
    dateAdded: Long = System.currentTimeMillis(),
): MovieEntity = MovieEntity(
    tmdbId = tmdbId,
    title = title,
    overview = overview,
    posterPath = posterPath,
    backdropPath = backdropPath,
    releaseDate = releaseDate,
    genres = genres,
    tmdbRating = tmdbRating,
    watchStatus = watchStatus,
    userRating = userRating,
    note = note,
    dateAdded = dateAdded,
)
