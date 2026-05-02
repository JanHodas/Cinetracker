package com.example.cinetracker.data.repository

import com.example.cinetracker.data.local.EpisodeWatchCount
import com.example.cinetracker.data.local.MovieDao
import com.example.cinetracker.data.local.WatchedEpisodeDao
import com.example.cinetracker.data.local.WatchedEpisodeEntity
import com.example.cinetracker.data.remote.TmdbApi
import com.example.cinetracker.data.remote.dto.TmdbEpisodeDto
import com.example.cinetracker.domain.mapper.toDomain
import com.example.cinetracker.domain.mapper.toEntity
import com.example.cinetracker.domain.model.CastMember
import com.example.cinetracker.domain.model.MediaItem
import com.example.cinetracker.domain.model.Movie
import com.example.cinetracker.domain.model.SavedMovie
import com.example.cinetracker.domain.model.Season
import com.example.cinetracker.domain.model.TvShow
import com.example.cinetracker.domain.model.WatchStatus
import com.example.cinetracker.data.remote.dto.TmdbMultiSearchResultDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Single source of truth for media data (movies and TV shows). Orchestrates
 * TMDB (remote) for search and detail lookups, and Room (local) for the
 * user's personal saved list.
 *
 * All one-shot APIs return [Result] so ViewModels can map success/failure to
 * UI state without wrapping calls in try/catch. List queries return [Flow]s
 * that Room keeps up-to-date automatically.
 */
class MovieRepository(
    private val tmdbApi: TmdbApi,
    private val movieDao: MovieDao,
    private val watchedEpisodeDao: WatchedEpisodeDao,
) {
    private val genreCacheMutex = Mutex()
    @Volatile private var cachedMovieGenres: Map<Int, String>? = null
    @Volatile private var cachedTvGenres: Map<Int, String>? = null

    // ── Remote — Search ────────────────────────────────────────────

    /**
     * Searches TMDB for movies only. Kept for backward compatibility.
     */
    suspend fun searchMovies(query: String): Result<List<Movie>> = runCatching {
        if (query.isBlank()) return@runCatching emptyList()
        val genres = ensureMovieGenres()
        tmdbApi.searchMovies(query = query)
            .results
            .map { it.toDomain(genres) }
    }

    /**
     * Combined movie + TV search via `/search/multi`. Person results are
     * filtered out; only movies and TV shows are returned as [MediaItem].
     *
     * If any result has an empty overview in the default language (sk-SK),
     * a second call with `en-US` is made and English overviews are used
     * as a fallback.
     */
    suspend fun searchMulti(query: String): Result<List<MediaItem>> = runCatching {
        if (query.isBlank()) return@runCatching emptyList()
        val allGenres = ensureAllGenres()
        val skResults = tmdbApi.searchMulti(query = query).results

        // Check if any result is missing an overview in Slovak.
        val needsFallback = skResults.any { result ->
            when (result) {
                is TmdbMultiSearchResultDto.MovieResult -> result.overview.isBlank()
                is TmdbMultiSearchResultDto.TvResult -> result.overview.isBlank()
                is TmdbMultiSearchResultDto.Unknown -> false
            }
        }

        val enOverviews: Map<Int, String> = if (needsFallback) {
            runCatching {
                tmdbApi.searchMulti(query = query, language = FALLBACK_LANGUAGE)
                    .results
                    .mapNotNull { r ->
                        when (r) {
                            is TmdbMultiSearchResultDto.MovieResult -> r.id to r.overview
                            is TmdbMultiSearchResultDto.TvResult -> r.id to r.overview
                            is TmdbMultiSearchResultDto.Unknown -> null
                        }
                    }
                    .toMap()
            }.getOrDefault(emptyMap())
        } else {
            emptyMap()
        }

        skResults.mapNotNull { result ->
            when (result) {
                is TmdbMultiSearchResultDto.MovieResult -> {
                    val overview = result.overview.ifBlank { enOverviews[result.id] ?: "" }
                    result.copy(overview = overview).toDomain(allGenres)
                }
                is TmdbMultiSearchResultDto.TvResult -> {
                    val overview = result.overview.ifBlank { enOverviews[result.id] ?: "" }
                    result.copy(overview = overview).toDomain(allGenres)
                }
                is TmdbMultiSearchResultDto.Unknown -> null
            }
        }
    }

    // ── Remote — Detail ────────────────────────────────────────────

    /**
     * Loads the rich detail payload for a single movie from TMDB.
     * If the overview is empty in sk-SK, falls back to en-US.
     * If the network call fails entirely, falls back to the Room-cached copy.
     */
    suspend fun getMovieDetail(tmdbId: Int): Result<Movie> {
        val remoteResult = runCatching {
            val detail = tmdbApi.getMovieDetail(tmdbId)
            if (detail.overview.isBlank()) {
                val enDetail = runCatching { tmdbApi.getMovieDetail(tmdbId, language = FALLBACK_LANGUAGE) }
                detail.copy(overview = enDetail.getOrNull()?.overview ?: "").toDomain()
            } else {
                detail.toDomain()
            }
        }
        if (remoteResult.isSuccess) return remoteResult

        // Network failed — try local fallback for saved movies.
        val local = movieDao.observeByTmdbId(tmdbId).first()
        if (local != null) return Result.success(local.toDomain().movie as Movie)

        return remoteResult
    }

    /**
     * Loads the rich detail payload for a single TV show from TMDB.
     * If the overview is empty in sk-SK, falls back to en-US.
     * If the network call fails entirely, falls back to the Room-cached copy.
     */
    suspend fun getTvDetail(tmdbId: Int): Result<TvShow> {
        val remoteResult = runCatching {
            val detail = tmdbApi.getTvDetail(tmdbId)
            if (detail.overview.isBlank()) {
                val enDetail = runCatching { tmdbApi.getTvDetail(tmdbId, language = FALLBACK_LANGUAGE) }
                detail.copy(overview = enDetail.getOrNull()?.overview ?: "").toDomain()
            } else {
                detail.toDomain()
            }
        }
        if (remoteResult.isSuccess) return remoteResult

        val local = movieDao.observeByTmdbId(tmdbId).first()
        if (local != null) return Result.success(local.toDomain().movie as TvShow)

        return remoteResult
    }

    /**
     * Loads a full season detail (with episodes) from TMDB.
     * No offline fallback — episode data is not stored locally.
     */
    suspend fun getTvSeason(tvId: Int, seasonNumber: Int): Result<Season> = runCatching {
        val detail = tmdbApi.getTvSeason(tvId, seasonNumber)
        if (detail.episodes.any { it.overview.isBlank() }) {
            val enDetail = runCatching {
                tmdbApi.getTvSeason(tvId, seasonNumber, language = FALLBACK_LANGUAGE)
            }.getOrNull()

            detail.copy(
                episodes = detail.episodes.withEpisodeOverviewFallback(
                    fallbackEpisodes = enDetail?.episodes.orEmpty(),
                ),
            ).toDomain()
        } else {
            detail.toDomain()
        }
    }

    /** Loads the cast list for a movie from TMDB. */
    suspend fun getMovieCast(tmdbId: Int): Result<List<CastMember>> = runCatching {
        tmdbApi.getMovieCredits(tmdbId)
            .cast
            .sortedBy { it.order }
            .map { it.toDomain() }
    }

    /** Loads the cast list for a TV show from TMDB. */
    suspend fun getTvCast(tmdbId: Int): Result<List<CastMember>> = runCatching {
        tmdbApi.getTvCredits(tmdbId)
            .cast
            .sortedBy { it.order }
            .map { it.toDomain() }
    }

    // ── Local (Room) ────────────────────────────────────────────────

    /** Observe all saved items (newest first). */
    fun observeSavedMovies(): Flow<List<SavedMovie>> =
        movieDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    /** Observe saved items filtered by [status]. */
    fun observeSavedMoviesByStatus(status: WatchStatus): Flow<List<SavedMovie>> =
        movieDao.observeByStatus(status.name).map { entities -> entities.map { it.toDomain() } }

    /** Observe saved items filtered by [mediaType] (`"movie"` or `"tv"`). */
    fun observeSavedByMediaType(mediaType: String): Flow<List<SavedMovie>> =
        movieDao.observeByMediaType(mediaType).map { entities -> entities.map { it.toDomain() } }

    /** Observe saved items filtered by both [status] and [mediaType]. */
    fun observeSavedByStatusAndMediaType(
        status: WatchStatus,
        mediaType: String,
    ): Flow<List<SavedMovie>> =
        movieDao.observeByStatusAndMediaType(status.name, mediaType)
            .map { entities -> entities.map { it.toDomain() } }

    /** Observe whether a specific item is saved and its current state. */
    fun observeSavedState(tmdbId: Int): Flow<SavedMovie?> =
        movieDao.observeByTmdbId(tmdbId).map { it?.toDomain() }

    /**
     * Save a media item to the user's list (or update it if already present).
     * Works for both [Movie] and [TvShow] — the entity's `mediaType` column
     * is set automatically by [MediaItem.toEntity].
     */
    suspend fun saveMedia(
        mediaItem: MediaItem,
        watchStatus: WatchStatus,
        userRating: Float? = null,
        note: String = "",
        dateAdded: Long = System.currentTimeMillis(),
    ) {
        movieDao.upsert(mediaItem.toEntity(watchStatus, userRating, note, dateAdded))
    }

    /**
     * Save a movie to the user's list. Delegates to [saveMedia].
     * Kept for backward compatibility with existing callers.
     */
    suspend fun saveMovie(
        movie: Movie,
        watchStatus: WatchStatus,
        userRating: Float? = null,
        note: String = "",
        dateAdded: Long = System.currentTimeMillis(),
    ) {
        saveMedia(movie, watchStatus, userRating, note, dateAdded)
    }

    /** Update only the watch status of an already-saved item. */
    suspend fun updateStatus(
        tmdbId: Int,
        mediaItem: MediaItem,
        status: WatchStatus,
        currentSaved: SavedMovie,
    ) {
        movieDao.upsert(
            mediaItem.toEntity(
                watchStatus = status,
                userRating = currentSaved.userRating,
                note = currentSaved.note,
                dateAdded = currentSaved.dateAdded,
            ),
        )
    }

    /** Update the user rating of an already-saved item. */
    suspend fun updateRating(mediaItem: MediaItem, rating: Float?, currentSaved: SavedMovie) {
        movieDao.upsert(
            mediaItem.toEntity(
                watchStatus = currentSaved.watchStatus,
                userRating = rating,
                note = currentSaved.note,
                dateAdded = currentSaved.dateAdded,
            ),
        )
    }

    /** Update the personal note of an already-saved item. */
    suspend fun updateNote(mediaItem: MediaItem, note: String, currentSaved: SavedMovie) {
        movieDao.upsert(
            mediaItem.toEntity(
                watchStatus = currentSaved.watchStatus,
                userRating = currentSaved.userRating,
                note = note,
                dateAdded = currentSaved.dateAdded,
            ),
        )
    }

    /** Remove an item from the user's list by TMDB id. Also cleans up watched episodes. */
    suspend fun removeMovie(tmdbId: Int) {
        movieDao.deleteByTmdbId(tmdbId)
        watchedEpisodeDao.deleteByTmdbId(tmdbId)
    }

    /**
     * Deletes a saved item and returns a snapshot that can be used for undo.
     * For TV shows this also preserves watched-episode progress.
     */
    suspend fun deleteSavedItem(savedMovie: SavedMovie): DeletedSavedItem {
        val watchedEpisodes = watchedEpisodeDao.getByTmdbId(savedMovie.movie.tmdbId)
        removeMovie(savedMovie.movie.tmdbId)
        return DeletedSavedItem(
            savedMovie = savedMovie,
            watchedEpisodes = watchedEpisodes,
        )
    }

    /**
     * Restores a previously deleted item, including watched-episode progress.
     */
    suspend fun restoreDeletedItem(deletedItem: DeletedSavedItem) {
        val savedMovie = deletedItem.savedMovie
        saveMedia(
            mediaItem = savedMovie.movie,
            watchStatus = savedMovie.watchStatus,
            userRating = savedMovie.userRating,
            note = savedMovie.note,
            dateAdded = savedMovie.dateAdded,
        )
        deletedItem.watchedEpisodes.forEach { watchedEpisodeDao.upsert(it) }
    }

    // ── Episode tracking ───────────────────────────────────────────────

    /** Observe all watched episodes for a TV show (for detail screen checkboxes). */
    fun observeWatchedEpisodes(tmdbId: Int): Flow<Set<Pair<Int, Int>>> =
        watchedEpisodeDao.observeByTmdbId(tmdbId).map { entities ->
            entities.map { it.seasonNumber to it.episodeNumber }.toSet()
        }

    /** Observe per-show watched counts for all TV shows (for MyList badges). */
    fun observeAllWatchedCounts(): Flow<Map<Int, Int>> =
        watchedEpisodeDao.observeAllCounts().map { counts ->
            counts.associate { it.tmdbId to it.count }
        }

    /** Toggle an episode's watched status: mark if unwatched, unmark if watched. */
    suspend fun toggleEpisodeWatched(tmdbId: Int, seasonNumber: Int, episodeNumber: Int) {
        val existing = watchedEpisodeDao.getByTmdbId(tmdbId)
            .any { it.seasonNumber == seasonNumber && it.episodeNumber == episodeNumber }
        if (existing) {
            watchedEpisodeDao.delete(tmdbId, seasonNumber, episodeNumber)
        } else {
            watchedEpisodeDao.upsert(
                WatchedEpisodeEntity(tmdbId = tmdbId, seasonNumber = seasonNumber, episodeNumber = episodeNumber),
            )
        }
    }

    /**
     * Marks the next unwatched episode for a TV show (MAL-style "+" button).
     *
     * Iterates seasons in order and finds the first episode that has not been
     * marked as watched. Uses a single TMDB API call to retrieve the season
     * structure (episode counts per season); results are cached in memory.
     *
     * @return `true` if an episode was marked, `false` if all episodes are watched.
     */
    suspend fun markNextEpisodeWatched(tmdbId: Int): Boolean {
        val watched = watchedEpisodeDao.getByTmdbId(tmdbId)
            .map { it.seasonNumber to it.episodeNumber }
            .toSet()

        val structure = getTvSeasonStructure(tmdbId) ?: return false

        for ((seasonNumber, episodeCount) in structure) {
            for (epNum in 1..episodeCount) {
                if ((seasonNumber to epNum) !in watched) {
                    watchedEpisodeDao.upsert(
                        WatchedEpisodeEntity(tmdbId = tmdbId, seasonNumber = seasonNumber, episodeNumber = epNum),
                    )
                    return true
                }
            }
        }
        return false // all watched
    }

    /**
     * In-memory cache of season structures (seasonNumber to episodeCount pairs)
     * to avoid repeated TMDB calls when the "+" button is tapped rapidly.
     */
    private val tvStructureCache = mutableMapOf<Int, List<Pair<Int, Int>>>()

    private suspend fun getTvSeasonStructure(tmdbId: Int): List<Pair<Int, Int>>? {
        tvStructureCache[tmdbId]?.let { return it }
        val detail = runCatching { tmdbApi.getTvDetail(tmdbId) }.getOrNull() ?: return null
        val structure = detail.seasons
            .filter { it.seasonNumber > 0 }
            .sortedBy { it.seasonNumber }
            .map { it.seasonNumber to it.episodeCount }
        tvStructureCache[tmdbId] = structure
        return structure
    }

    // ── Stats (unfiltered — all media types) ────────────────────────

    /** Observe status counts across all saved items. */
    fun observeStatusCounts(): Flow<Map<WatchStatus, Int>> =
        movieDao.observeStatusCounts().map { counts ->
            counts.associate { WatchStatus.valueOf(it.watchStatus) to it.count }
        }

    /** Observe average user rating across all rated items. */
    fun observeAverageRating(): Flow<Float?> = movieDao.observeAverageRating()

    /** Observe total number of saved items. */
    fun observeTotalCount(): Flow<Int> = movieDao.observeCount()

    /** Observe genre frequency across all saved items, sorted descending. */
    fun observeTopGenres(): Flow<List<Pair<String, Int>>> =
        movieDao.observeAllGenres().map { it.toGenreRanking() }

    // ── Stats (per media type) ──────────────────────────────────────

    /** Observe status counts for a specific [mediaType]. */
    fun observeStatusCountsByMediaType(mediaType: String): Flow<Map<WatchStatus, Int>> =
        movieDao.observeStatusCountsByMediaType(mediaType).map { counts ->
            counts.associate { WatchStatus.valueOf(it.watchStatus) to it.count }
        }

    /** Observe average user rating for a specific [mediaType]. */
    fun observeAverageRatingByMediaType(mediaType: String): Flow<Float?> =
        movieDao.observeAverageRatingByMediaType(mediaType)

    /** Observe total count for a specific [mediaType]. */
    fun observeTotalCountByMediaType(mediaType: String): Flow<Int> =
        movieDao.observeCountByMediaType(mediaType)

    /** Observe genre frequency for a specific [mediaType], sorted descending. */
    fun observeTopGenresByMediaType(mediaType: String): Flow<List<Pair<String, Int>>> =
        movieDao.observeAllGenresByMediaType(mediaType).map { it.toGenreRanking() }

    // ── Internal ────────────────────────────────────────────────────

    /**
     * Flattens a list of JSON-encoded genre arrays into a ranked frequency list.
     */
    private fun List<String>.toGenreRanking(): List<Pair<String, Int>> =
        flatMap { json ->
            runCatching {
                kotlinx.serialization.json.Json.decodeFromString<List<String>>(json)
            }.getOrDefault(emptyList())
        }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .map { it.key to it.value }

    /** Lazily loads and caches movie genre lookup table. */
    private suspend fun ensureMovieGenres(): Map<Int, String> {
        cachedMovieGenres?.let { return it }
        return genreCacheMutex.withLock {
            cachedMovieGenres ?: runCatching {
                tmdbApi.getGenres().genres.associate { it.id to it.name }
            }.getOrDefault(emptyMap()).also { cachedMovieGenres = it }
        }
    }

    /** Lazily loads and caches TV genre lookup table. */
    private suspend fun ensureTvGenres(): Map<Int, String> {
        cachedTvGenres?.let { return it }
        return genreCacheMutex.withLock {
            cachedTvGenres ?: runCatching {
                tmdbApi.getTvGenres().genres.associate { it.id to it.name }
            }.getOrDefault(emptyMap()).also { cachedTvGenres = it }
        }
    }

    /**
     * Returns a merged genre map (movie + TV). Used by [searchMulti] where
     * results may contain both media types.
     */
    private suspend fun ensureAllGenres(): Map<Int, String> {
        val movie = ensureMovieGenres()
        val tv = ensureTvGenres()
        return movie + tv
    }

    /**
     * Fills blank episode overviews from the fallback-language response by
     * matching episodes on TMDB id first and episode number second.
     */
    private fun List<TmdbEpisodeDto>.withEpisodeOverviewFallback(
        fallbackEpisodes: List<TmdbEpisodeDto>,
    ): List<TmdbEpisodeDto> {
        if (fallbackEpisodes.isEmpty()) return this

        val fallbackById = fallbackEpisodes.associateBy { it.id }
        val fallbackByEpisodeNumber = fallbackEpisodes.associateBy { it.episodeNumber }

        return map { episode ->
            if (episode.overview.isNotBlank()) {
                episode
            } else {
                val fallbackOverview = fallbackById[episode.id]?.overview
                    ?: fallbackByEpisodeNumber[episode.episodeNumber]?.overview
                    ?: ""
                episode.copy(overview = fallbackOverview)
            }
        }
    }

    companion object {
        /** Language used when the primary (sk-SK) overview is empty. */
        private const val FALLBACK_LANGUAGE = "en-US"
    }
}

data class DeletedSavedItem(
    val savedMovie: SavedMovie,
    val watchedEpisodes: List<WatchedEpisodeEntity>,
)
