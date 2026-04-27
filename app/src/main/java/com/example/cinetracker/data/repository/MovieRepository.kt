package com.example.cinetracker.data.repository

import com.example.cinetracker.data.remote.TmdbApi
import com.example.cinetracker.domain.mapper.toDomain
import com.example.cinetracker.domain.model.Movie
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Single source of truth for movie data. In Phase 1 it only orchestrates the remote TMDB API;
 * Phase 2 will add a Room-backed local cache for the user's saved list.
 *
 * All public APIs return [Result] so the ViewModel can map success/failure to UI state without
 * having to wrap calls in try/catch.
 */
class MovieRepository(
    private val tmdbApi: TmdbApi,
) {
    private val genreCacheMutex = Mutex()
    @Volatile private var cachedGenres: Map<Int, String>? = null

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
     * Loads the rich detail payload for a single movie.
     */
    suspend fun getMovieDetail(tmdbId: Int): Result<Movie> = runCatching {
        tmdbApi.getMovieDetail(tmdbId).toDomain()
    }

    /**
     * Returns (and lazily fetches) the genre id → name lookup table.
     * Wrapped in a mutex so concurrent callers during cold start hit TMDB only once.
     * Failures are swallowed into an empty map: missing genres degrade gracefully to an
     * empty chip row, not to a blocked search flow.
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
