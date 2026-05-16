package com.example.cinetracker.data.repository

import android.content.Context
import com.example.cinetracker.data.local.EpisodeWatchCount
import com.example.cinetracker.data.local.MovieDao
import com.example.cinetracker.data.local.SeasonRatingDao
import com.example.cinetracker.data.local.SeasonRatingEntity
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
import com.example.cinetracker.ui.language.LanguageManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
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
    private val applicationContext: Context,
    private val tmdbApi: TmdbApi,
    private val movieDao: MovieDao,
    private val watchedEpisodeDao: WatchedEpisodeDao,
    private val seasonRatingDao: SeasonRatingDao,
) {
    private val genreCacheMutex = Mutex()
    @Volatile private var cachedMovieGenres: Map<String, Map<Int, String>> = emptyMap()
    @Volatile private var cachedTvGenres: Map<String, Map<Int, String>> = emptyMap()

    // ── Remote — Search ────────────────────────────────────────────

    /**
     * Searches TMDB for movies only. Kept for backward compatibility.
     */
    suspend fun searchMovies(query: String): Result<List<Movie>> = runCatching {
        if (query.isBlank()) return@runCatching emptyList()
        val language = currentTmdbLanguage()
        val genres = ensureMovieGenres(language)
        tmdbApi.searchMovies(query = query, language = language)
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
        val language = currentTmdbLanguage()
        val allGenres = ensureAllGenres(language)
        val localizedResults = tmdbApi.searchMulti(query = query, language = language).results

        val fallbackLanguage = fallbackLanguage(language)
        val needsFallback = fallbackLanguage != null && localizedResults.any { result ->
            when (result) {
                is TmdbMultiSearchResultDto.MovieResult -> result.overview.isBlank()
                is TmdbMultiSearchResultDto.TvResult -> result.overview.isBlank()
                is TmdbMultiSearchResultDto.Unknown -> false
            }
        }

        val fallbackOverviews: Map<Int, String> = if (needsFallback) {
            runCatching {
                tmdbApi.searchMulti(query = query, language = checkNotNull(fallbackLanguage))
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

        localizedResults.mapNotNull { result ->
            when (result) {
                is TmdbMultiSearchResultDto.MovieResult -> {
                    val overview = result.overview.ifBlank { fallbackOverviews[result.id] ?: "" }
                    result.copy(overview = overview).toDomain(allGenres)
                }
                is TmdbMultiSearchResultDto.TvResult -> {
                    val overview = result.overview.ifBlank { fallbackOverviews[result.id] ?: "" }
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
        val language = currentTmdbLanguage()
        val remoteResult = runCatching {
            val detail = tmdbApi.getMovieDetail(tmdbId, language = language)
            val fallbackLanguage = fallbackLanguage(language)
            if (detail.overview.isBlank() && fallbackLanguage != null) {
                val fallbackDetail = runCatching { tmdbApi.getMovieDetail(tmdbId, language = fallbackLanguage) }
                detail.copy(overview = fallbackDetail.getOrNull()?.overview ?: "").toDomain()
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
        val language = currentTmdbLanguage()
        val remoteResult = runCatching {
            val detail = tmdbApi.getTvDetail(tmdbId, language = language)
            val fallbackLanguage = fallbackLanguage(language)
            if (detail.overview.isBlank() && fallbackLanguage != null) {
                val fallbackDetail = runCatching { tmdbApi.getTvDetail(tmdbId, language = fallbackLanguage) }
                detail.copy(overview = fallbackDetail.getOrNull()?.overview ?: "").toDomain()
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
        val language = currentTmdbLanguage()
        val detail = tmdbApi.getTvSeason(tvId, seasonNumber, language = language)
        val fallbackLanguage = fallbackLanguage(language)
        if (detail.episodes.any { it.overview.isBlank() } && fallbackLanguage != null) {
            val enDetail = runCatching {
                tmdbApi.getTvSeason(tvId, seasonNumber, language = fallbackLanguage)
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
        tmdbApi.getMovieCredits(tmdbId, language = currentTmdbLanguage())
            .cast
            .sortedBy { it.order }
            .map { it.toDomain() }
    }

    /** Loads the cast list for a TV show from TMDB. */
    suspend fun getTvCast(tmdbId: Int): Result<List<CastMember>> = runCatching {
        tmdbApi.getTvCredits(tmdbId, language = currentTmdbLanguage())
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
        if (mediaItem is TvShow && watchStatus == WatchStatus.WATCHED) {
            markAllEpisodesWatched(mediaItem)
        }
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

    /**
     * Saves the item as "want to watch" only when it is not already in the list.
     *
     * @return `true` if a new row was created, `false` if the item already existed.
     */
    suspend fun saveAsWantToWatchIfMissing(mediaItem: MediaItem): Boolean {
        val existing = movieDao.observeByTmdbId(mediaItem.tmdbId).first()
        if (existing != null) return false
        val enrichedMediaItem = when (mediaItem) {
            is TvShow -> getTvDetail(mediaItem.tmdbId).getOrDefault(mediaItem)
            else -> mediaItem
        }
        saveMedia(mediaItem = enrichedMediaItem, watchStatus = WatchStatus.WANT_TO_WATCH)
        return true
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
        if (mediaItem is TvShow && status == WatchStatus.WATCHED) {
            markAllEpisodesWatched(mediaItem)
        }
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
        seasonRatingDao.deleteByTmdbId(tmdbId)
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

    /** Observe user ratings for individual seasons of a TV show. */
    fun observeSeasonRatings(tmdbId: Int): Flow<Map<Int, Float?>> =
        seasonRatingDao.observeByTmdbId(tmdbId).map { entities ->
            entities.associate { it.seasonNumber to it.userRating }
        }

    /** Toggle an episode's watched status: mark if unwatched, unmark if watched. */
    suspend fun toggleEpisodeWatched(tmdbId: Int, seasonNumber: Int, episodeNumber: Int, runtime: Int? = null) {
        val existing = watchedEpisodeDao.getByTmdbId(tmdbId)
            .any { it.seasonNumber == seasonNumber && it.episodeNumber == episodeNumber }
        if (existing) {
            watchedEpisodeDao.delete(tmdbId, seasonNumber, episodeNumber)
        } else {
            watchedEpisodeDao.upsert(
                WatchedEpisodeEntity(
                    tmdbId = tmdbId,
                    seasonNumber = seasonNumber,
                    episodeNumber = episodeNumber,
                    runtime = runtime,
                ),
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
        val savedEntity = movieDao.observeByTmdbId(tmdbId).first()
        val watched = watchedEpisodeDao.getByTmdbId(tmdbId)
            .map { it.seasonNumber to it.episodeNumber }
            .toSet()

        val episodes = getTvEpisodeDetails(tmdbId) ?: return false

        val nextEpisode = episodes.firstOrNull { (it.seasonNumber to it.episodeNumber) !in watched }
            ?: return false

        watchedEpisodeDao.upsert(
            WatchedEpisodeEntity(
                tmdbId = tmdbId,
                seasonNumber = nextEpisode.seasonNumber,
                episodeNumber = nextEpisode.episodeNumber,
                runtime = nextEpisode.runtime,
            ),
        )
        if (savedEntity?.mediaType == "tv" && savedEntity.watchStatus == WatchStatus.WANT_TO_WATCH) {
            val savedMovie = savedEntity.toDomain()
            updateStatus(
                tmdbId = tmdbId,
                mediaItem = savedMovie.movie,
                status = WatchStatus.WATCHING,
                currentSaved = savedMovie,
            )
        }
        return true
    }

    /** Marks every regular episode of the given TV show as watched. */
    suspend fun markAllEpisodesWatched(tvShow: TvShow) {
        val episodes = getTvEpisodeDetails(tvShow.tmdbId) ?: return
        episodes.forEach { ep ->
            watchedEpisodeDao.upsert(
                WatchedEpisodeEntity(
                    tmdbId = tvShow.tmdbId,
                    seasonNumber = ep.seasonNumber,
                    episodeNumber = ep.episodeNumber,
                    runtime = ep.runtime,
                ),
            )
        }
    }

    /** Marks all episodes in a specific season as watched. */
    suspend fun markSeasonWatched(tmdbId: Int, seasonNumber: Int, episodeRuntimes: List<Pair<Int, Int?>>) {
        episodeRuntimes.forEach { (episodeNumber, runtime) ->
            watchedEpisodeDao.upsert(
                WatchedEpisodeEntity(
                    tmdbId = tmdbId,
                    seasonNumber = seasonNumber,
                    episodeNumber = episodeNumber,
                    runtime = runtime,
                ),
            )
        }
    }

    /** Unmarks all watched episodes in a specific season. */
    suspend fun unmarkSeasonWatched(tmdbId: Int, seasonNumber: Int, episodeNumbers: List<Int>) {
        episodeNumbers.forEach { episodeNumber ->
            watchedEpisodeDao.delete(
                tmdbId = tmdbId,
                seasonNumber = seasonNumber,
                episodeNumber = episodeNumber,
            )
        }
    }

    /** Stores or clears the user's rating for a specific season. */
    suspend fun updateSeasonRating(tmdbId: Int, seasonNumber: Int, rating: Float?) {
        if (rating == null) {
            seasonRatingDao.delete(tmdbId, seasonNumber)
        } else {
            seasonRatingDao.upsert(
                SeasonRatingEntity(
                    tmdbId = tmdbId,
                    seasonNumber = seasonNumber,
                    userRating = rating,
                ),
            )
        }
    }

    /**
     * Refreshes already-saved movies and TV shows so the local list reflects
     * the currently selected app language.
     */
    suspend fun refreshSavedMediaForCurrentLanguage() {
        val needsLanguageRefresh = LanguageManager.shouldRefreshLocalizedContent(applicationContext)
        val needsMovieRuntimeRefresh = movieDao.hasItemsWithoutRuntime()
        val needsEpisodeRuntimeRefresh = watchedEpisodeDao.hasEpisodesWithoutRuntime()
        if (!needsLanguageRefresh && !needsMovieRuntimeRefresh && !needsEpisodeRuntimeRefresh) return

        val savedItems = movieDao.observeAll().first()
        savedItems.forEach { entity ->
            val localizedMedia = when (entity.mediaType) {
                "tv" -> getTvDetail(entity.tmdbId).getOrNull()
                else -> getMovieDetail(entity.tmdbId).getOrNull()
            } ?: return@forEach

            val newEntity = localizedMedia.toEntity(
                watchStatus = entity.watchStatus,
                userRating = entity.userRating,
                note = entity.note,
                dateAdded = entity.dateAdded,
            )
            movieDao.upsert(
                if (newEntity.runtime == null && entity.runtime != null) {
                    newEntity.copy(runtime = entity.runtime)
                } else {
                    newEntity
                },
            )

            if (entity.mediaType == "tv" && needsEpisodeRuntimeRefresh) {
                backfillEpisodeRuntimes(entity.tmdbId)
            }
        }

        LanguageManager.markLocalizedContentSynced(applicationContext)
    }

    private suspend fun backfillEpisodeRuntimes(tmdbId: Int) {
        val episodes = getTvEpisodeDetails(tmdbId) ?: return
        val runtimeMap = episodes.associate { (it.seasonNumber to it.episodeNumber) to it.runtime }
        val watchedEpisodes = watchedEpisodeDao.getByTmdbId(tmdbId)
        watchedEpisodes.filter { it.runtime == null }.forEach { entity ->
            val runtime = runtimeMap[entity.seasonNumber to entity.episodeNumber]
            if (runtime != null) {
                watchedEpisodeDao.upsert(entity.copy(runtime = runtime))
            }
        }
    }

    /**
     * In-memory cache of per-episode info (seasonNumber, episodeNumber, runtime)
     * to avoid repeated TMDB calls when the "+" button is tapped rapidly.
     */
    private val tvEpisodeCache = mutableMapOf<Int, List<EpisodeInfo>>()

    private data class EpisodeInfo(val seasonNumber: Int, val episodeNumber: Int, val runtime: Int?)

    private suspend fun getTvEpisodeDetails(tmdbId: Int): List<EpisodeInfo>? {
        tvEpisodeCache[tmdbId]?.let { return it }
        val detail = runCatching {
            tmdbApi.getTvDetail(tmdbId, language = currentTmdbLanguage())
        }.getOrNull() ?: return null

        val episodes = mutableListOf<EpisodeInfo>()
        detail.seasons
            .filter { it.seasonNumber > 0 }
            .sortedBy { it.seasonNumber }
            .forEach { season ->
                val seasonDetail = runCatching {
                    tmdbApi.getTvSeason(tmdbId, season.seasonNumber, language = currentTmdbLanguage())
                }.getOrNull()
                if (seasonDetail != null) {
                    seasonDetail.episodes.forEach { ep ->
                        episodes.add(EpisodeInfo(season.seasonNumber, ep.episodeNumber, ep.runtime))
                    }
                } else {
                    for (epNum in 1..season.episodeCount) {
                        episodes.add(EpisodeInfo(season.seasonNumber, epNum, null))
                    }
                }
            }
        tvEpisodeCache[tmdbId] = episodes
        return episodes
    }

    private suspend fun getTvSeasonStructure(tmdbId: Int): List<Pair<Int, Int>>? {
        val episodes = getTvEpisodeDetails(tmdbId) ?: return null
        return episodes
            .groupBy { it.seasonNumber }
            .entries
            .sortedBy { it.key }
            .map { (seasonNumber, eps) -> seasonNumber to eps.size }
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

    // ── Stats (runtime) ────────────────────────────────────────────

    /** Observe total watched runtime: movie runtime + actual TV episode runtimes. */
    fun observeTotalRuntime(): Flow<Int> =
        combine(movieDao.observeMovieRuntime(), watchedEpisodeDao.observeTotalWatchedRuntime()) { m, t -> m + t }

    /** Observe total watched runtime for a specific [mediaType]. */
    fun observeTotalRuntimeByMediaType(mediaType: String): Flow<Int> = when (mediaType) {
        "movie" -> movieDao.observeMovieRuntime()
        "tv" -> watchedEpisodeDao.observeTotalWatchedRuntime()
        else -> observeTotalRuntime()
    }

    /** Observe total watched runtime for a specific [status]. */
    fun observeTotalRuntimeByStatus(status: WatchStatus): Flow<Int> =
        combine(
            movieDao.observeMovieRuntimeByStatus(status.name),
            watchedEpisodeDao.observeWatchedRuntimeByStatus(status.name),
        ) { m, t -> m + t }

    /** Observe total watched runtime for a specific [status] and [mediaType]. */
    fun observeTotalRuntimeByStatusAndMediaType(status: WatchStatus, mediaType: String): Flow<Int> = when (mediaType) {
        "movie" -> movieDao.observeMovieRuntimeByStatus(status.name)
        "tv" -> watchedEpisodeDao.observeWatchedRuntimeByStatus(status.name)
        else -> observeTotalRuntimeByStatus(status)
    }

    // ── Stats (per status) ──────────────────────────────────────────

    /** Observe total count for a specific [status]. */
    fun observeTotalCountByStatus(status: WatchStatus): Flow<Int> =
        movieDao.observeCountByStatus(status.name)

    /** Observe average user rating for a specific [status]. */
    fun observeAverageRatingByStatus(status: WatchStatus): Flow<Float?> =
        movieDao.observeAverageRatingByStatus(status.name)

    /** Observe genre frequency for a specific [status], sorted descending. */
    fun observeTopGenresByStatus(status: WatchStatus): Flow<List<Pair<String, Int>>> =
        movieDao.observeAllGenresByStatus(status.name).map { it.toGenreRanking() }

    // ── Stats (per status + media type) ─────────────────────────────

    /** Observe total count for a specific [status] and [mediaType]. */
    fun observeTotalCountByStatusAndMediaType(status: WatchStatus, mediaType: String): Flow<Int> =
        movieDao.observeCountByStatusAndMediaType(status.name, mediaType)

    /** Observe average user rating for a specific [status] and [mediaType]. */
    fun observeAverageRatingByStatusAndMediaType(status: WatchStatus, mediaType: String): Flow<Float?> =
        movieDao.observeAverageRatingByStatusAndMediaType(status.name, mediaType)

    /** Observe genre frequency for a specific [status] and [mediaType], sorted descending. */
    fun observeTopGenresByStatusAndMediaType(status: WatchStatus, mediaType: String): Flow<List<Pair<String, Int>>> =
        movieDao.observeAllGenresByStatusAndMediaType(status.name, mediaType).map { it.toGenreRanking() }

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
    private suspend fun ensureMovieGenres(language: String): Map<Int, String> {
        cachedMovieGenres[language]?.let { return it }
        return genreCacheMutex.withLock {
            cachedMovieGenres[language] ?: runCatching {
                tmdbApi.getGenres(language = language).genres.associate { it.id to it.name }
            }.getOrDefault(emptyMap()).also { genres ->
                cachedMovieGenres = cachedMovieGenres + (language to genres)
            }
        }
    }

    /** Lazily loads and caches TV genre lookup table. */
    private suspend fun ensureTvGenres(language: String): Map<Int, String> {
        cachedTvGenres[language]?.let { return it }
        return genreCacheMutex.withLock {
            cachedTvGenres[language] ?: runCatching {
                tmdbApi.getTvGenres(language = language).genres.associate { it.id to it.name }
            }.getOrDefault(emptyMap()).also { genres ->
                cachedTvGenres = cachedTvGenres + (language to genres)
            }
        }
    }

    /**
     * Returns a merged genre map (movie + TV). Used by [searchMulti] where
     * results may contain both media types.
     */
    private suspend fun ensureAllGenres(language: String): Map<Int, String> {
        val movie = ensureMovieGenres(language)
        val tv = ensureTvGenres(language)
        return movie + tv
    }

    private fun currentTmdbLanguage(): String =
        LanguageManager.currentTmdbLanguage(applicationContext)

    private fun fallbackLanguage(primaryLanguage: String): String? =
        if (primaryLanguage == FALLBACK_LANGUAGE) null else FALLBACK_LANGUAGE

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
