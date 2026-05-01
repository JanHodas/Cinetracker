package com.example.cinetracker.domain.mapper

import com.example.cinetracker.data.local.MovieEntity
import com.example.cinetracker.data.remote.dto.TmdbEpisodeDto
import com.example.cinetracker.data.remote.dto.TmdbMovieDetailDto
import com.example.cinetracker.data.remote.dto.TmdbMovieDto
import com.example.cinetracker.data.remote.dto.TmdbMultiSearchResultDto
import com.example.cinetracker.data.remote.dto.TmdbSeasonDetailDto
import com.example.cinetracker.data.remote.dto.TmdbSeasonSummaryDto
import com.example.cinetracker.data.remote.dto.TmdbTvDetailDto
import com.example.cinetracker.domain.model.Episode
import com.example.cinetracker.domain.model.MediaItem
import com.example.cinetracker.domain.model.Movie
import com.example.cinetracker.domain.model.SavedMovie
import com.example.cinetracker.domain.model.Season
import com.example.cinetracker.domain.model.TvShow
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

// ── Multi-search result → Domain ───────────────────────────────────

/** Converts a multi-search movie result into the canonical [Movie] model. */
fun TmdbMultiSearchResultDto.MovieResult.toDomain(
    genreNamesById: Map<Int, String>,
): Movie = Movie(
    tmdbId = id,
    title = title,
    overview = overview,
    posterPath = posterPath,
    backdropPath = backdropPath,
    releaseDate = releaseDate?.takeIf { it.isNotBlank() },
    genres = genreIds.mapNotNull { genreNamesById[it] },
    tmdbRating = voteAverage.takeIf { it > 0f },
)

/** Converts a multi-search TV result into the canonical [TvShow] model. */
fun TmdbMultiSearchResultDto.TvResult.toDomain(
    genreNamesById: Map<Int, String>,
): TvShow = TvShow(
    tmdbId = id,
    title = name,
    overview = overview,
    posterPath = posterPath,
    backdropPath = backdropPath,
    releaseDate = firstAirDate?.takeIf { it.isNotBlank() },
    genres = genreIds.mapNotNull { genreNamesById[it] },
    tmdbRating = voteAverage.takeIf { it > 0f },
    numberOfSeasons = 0,    // not available in search results
    numberOfEpisodes = 0,
)

// ── TV detail DTOs → Domain ────────────────────────────────────────

/** Converts a full TV detail DTO into the domain [TvShow] model. */
fun TmdbTvDetailDto.toDomain(): TvShow = TvShow(
    tmdbId = id,
    title = name,
    overview = overview,
    posterPath = posterPath,
    backdropPath = backdropPath,
    releaseDate = firstAirDate?.takeIf { it.isNotBlank() },
    genres = genres.map { it.name },
    tmdbRating = voteAverage.takeIf { it > 0f },
    numberOfSeasons = numberOfSeasons,
    numberOfEpisodes = numberOfEpisodes,
)

/** Converts a season summary (from TV detail response) into the domain [Season] model. */
fun TmdbSeasonSummaryDto.toDomain(): Season = Season(
    seasonNumber = seasonNumber,
    name = name,
    overview = overview,
    posterPath = posterPath,
    episodeCount = episodeCount,
)

/** Converts a full season detail DTO (with episode list) into the domain [Season] model. */
fun TmdbSeasonDetailDto.toDomain(): Season = Season(
    seasonNumber = seasonNumber,
    name = name,
    overview = overview,
    posterPath = posterPath,
    episodeCount = episodes.size,
    episodes = episodes.map { it.toDomain() },
)

/** Converts an episode DTO into the domain [Episode] model. */
fun TmdbEpisodeDto.toDomain(): Episode = Episode(
    episodeNumber = episodeNumber,
    name = name,
    overview = overview,
    airDate = airDate?.takeIf { it.isNotBlank() },
    runtime = runtime,
)
