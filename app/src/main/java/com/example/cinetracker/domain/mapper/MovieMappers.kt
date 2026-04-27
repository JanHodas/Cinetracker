package com.example.cinetracker.domain.mapper

import com.example.cinetracker.data.remote.dto.TmdbMovieDetailDto
import com.example.cinetracker.data.remote.dto.TmdbMovieDto
import com.example.cinetracker.domain.model.Movie

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
