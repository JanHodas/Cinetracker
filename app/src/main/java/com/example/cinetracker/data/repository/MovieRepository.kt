package com.example.cinetracker.data.repository

import com.example.cinetracker.data.local.MovieDao
import com.example.cinetracker.data.remote.TmdbApi
import com.example.cinetracker.domain.mapper.toDomain
import com.example.cinetracker.domain.mapper.toEntity
import com.example.cinetracker.domain.model.Movie
import com.example.cinetracker.domain.model.SavedMovie
import com.example.cinetracker.domain.model.WatchStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Single source of truth for movie data. Orchestrates TMDB (remote) for search
 * and detail lookups, and Room (local) for the user's personal saved list.
 *
 * All one-shot APIs return [Result] so ViewModels can map success/failure to
 * UI state without wrapping calls in try/catch. List queries return [Flow]s
 * that Room keeps up-to-date automatically.
 */
class MovieRepository(
    private val tmdbApi: TmdbApi,
    private val movieDao: MovieDao,
) {
    private val genreCacheMutex = Mutex()
    @Volatile private var cachedGenres: Map<Int, String>? = null

    // ── Remote (TMDB) ───────────────────────────────────────────────

    /**
     * Searches TMDB by free-text [query]. Empty queries short-circuit to an empty list.
     */
    suspend fun searchMovies(query: String): Result<List<Movie>> = runCatching {
        if (query.isBlank()) return@runCatching emptyList()
        val genres = ensureGenres()
        tmdbApi.searchMovies(query = query)
            .results
            .map { it.toDomain(genres) }
    }

    /**
     * Loads the rich detail payload for a single movie from TMDB.
     * If the network call fails and the movie is saved locally, falls
     * back to the Room-cached copy so the detail screen works offline.
     */
    suspend fun getMovieDetail(tmdbId: Int): Result<Movie> {
        val remoteResult = runCatching { tmdbApi.getMovieDetail(tmdbId).toDomain() }
        if (remoteResult.isSuccess) return remoteResult

        // Network failed — try local fallback for saved movies.
        val local = movieDao.observeByTmdbId(tmdbId).first()
        if (local != null) return Result.success(local.toDomain().movie)

        return remoteResult
    }

    // ── Local (Room) ────────────────────────────────────────────────

    /** Observe all saved movies (newest first). */
    fun observeSavedMovies(): Flow<List<SavedMovie>> =
        movieDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    /** Observe saved movies filtered by [status]. */
    fun observeSavedMoviesByStatus(status: WatchStatus): Flow<List<SavedMovie>> =
        movieDao.observeByStatus(status.name).map { entities -> entities.map { it.toDomain() } }

    /** Observe whether a specific movie is saved and its current state. */
    fun observeSavedState(tmdbId: Int): Flow<SavedMovie?> =
        movieDao.observeByTmdbId(tmdbId).map { it?.toDomain() }

    /**
     * Save a movie to the user's list (or update it if already present).
     * TMDB metadata is snapshotted from [movie]; user fields are supplied separately.
     */
    suspend fun saveMovie(
        movie: Movie,
        watchStatus: WatchStatus,
        userRating: Float? = null,
        note: String = "",
        dateAdded: Long = System.currentTimeMillis(),
    ) {
        movieDao.upsert(movie.toEntity(watchStatus, userRating, note, dateAdded))
    }

    /** Update only the watch status of an already-saved movie. */
    suspend fun updateStatus(tmdbId: Int, movie: Movie, status: WatchStatus, currentSaved: SavedMovie) {
        movieDao.upsert(
            movie.toEntity(
                watchStatus = status,
                userRating = currentSaved.userRating,
                note = currentSaved.note,
                dateAdded = currentSaved.dateAdded,
            ),
        )
    }

    /** Update the user rating of an already-saved movie. */
    suspend fun updateRating(movie: Movie, rating: Float?, currentSaved: SavedMovie) {
        movieDao.upsert(
            movie.toEntity(
                watchStatus = currentSaved.watchStatus,
                userRating = rating,
                note = currentSaved.note,
                dateAdded = currentSaved.dateAdded,
            ),
        )
    }

    /** Update the personal note of an already-saved movie. */
    suspend fun updateNote(movie: Movie, note: String, currentSaved: SavedMovie) {
        movieDao.upsert(
            movie.toEntity(
                watchStatus = currentSaved.watchStatus,
                userRating = currentSaved.userRating,
                note = note,
                dateAdded = currentSaved.dateAdded,
            ),
        )
    }

    /** Remove a movie from the user's list by TMDB id. */
    suspend fun removeMovie(tmdbId: Int) {
        movieDao.deleteByTmdbId(tmdbId)
    }

    /** Observe status counts for the Stats screen. */
    fun observeStatusCounts(): Flow<Map<WatchStatus, Int>> =
        movieDao.observeStatusCounts().map { counts ->
            counts.associate { WatchStatus.valueOf(it.watchStatus) to it.count }
        }

    // ── Internal ────────────────────────────────────────────────────

    /**
     * Returns (and lazily fetches) the genre id → name lookup table.
     * Failures degrade gracefully to an empty map (missing genre chips, not a crash).
     */
    private suspend fun ensureGenres(): Map<Int, String> {
        cachedGenres?.let { return it }
        return genreCacheMutex.withLock {
            cachedGenres ?: runCatching {
                tmdbApi.getGenres().genres.associate { it.id to it.name }
            }.getOrDefault(emptyMap()).also { cachedGenres = it }
        }
    }
}
