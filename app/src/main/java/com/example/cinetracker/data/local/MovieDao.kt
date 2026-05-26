package com.example.cinetracker.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * Data-access object for the user's saved movie list.
 *
 * All list-returning queries use [Flow] so the UI automatically recomposes
 * when the underlying data changes (e.g. after an insert or status update).
 */
@Dao
interface MovieDao {

    /** Observe all saved movies ordered by user sort order, then newest first. */
    @Query("SELECT * FROM movies ORDER BY sortOrder ASC, dateAdded DESC")
    fun observeAll(): Flow<List<MovieEntity>>

    /** Observe only movies with a specific [watchStatus]. */
    @Query("SELECT * FROM movies WHERE watchStatus = :watchStatus ORDER BY sortOrder ASC, dateAdded DESC")
    fun observeByStatus(watchStatus: String): Flow<List<MovieEntity>>

    /** One-shot lookup used by DetailViewModel to check if a movie is already saved. */
    @Query("SELECT * FROM movies WHERE tmdbId = :tmdbId LIMIT 1")
    fun observeByTmdbId(tmdbId: Int): Flow<MovieEntity?>

    /** Insert or update a movie (conflict = replace by primary key). */
    @Upsert
    suspend fun upsert(movie: MovieEntity)

    /** Remove a movie from the user's list. */
    @Delete
    suspend fun delete(movie: MovieEntity)

    /** Remove a movie by its TMDB id. */
    @Query("DELETE FROM movies WHERE tmdbId = :tmdbId")
    suspend fun deleteByTmdbId(tmdbId: Int)

    /** Remove all saved items. */
    @Query("DELETE FROM movies")
    suspend fun deleteAll()

    /** Get current sortOrder for an item, or null if not found. */
    @Query("SELECT sortOrder FROM movies WHERE tmdbId = :tmdbId")
    suspend fun getSortOrder(tmdbId: Int): Int?

    /** Get the highest sortOrder currently assigned, or null if the table is empty. */
    @Query("SELECT MAX(sortOrder) FROM movies")
    suspend fun getMaxSortOrder(): Int?

    /** Update sort order for a single item. */
    @Query("UPDATE movies SET sortOrder = :sortOrder WHERE tmdbId = :tmdbId")
    suspend fun updateSortOrder(tmdbId: Int, sortOrder: Int)

    // ── Unfiltered stats (backward compatibility) ────────────────────

    /** Total number of saved items (for Stats screen). */
    @Query("SELECT COUNT(*) FROM movies")
    fun observeCount(): Flow<Int>

    /** Count per watch status (for Stats screen). */
    @Query("SELECT watchStatus, COUNT(*) as count FROM movies GROUP BY watchStatus")
    fun observeStatusCounts(): Flow<List<StatusCount>>

    /** Average user rating across all rated items (for Stats screen). */
    @Query("SELECT AVG(userRating) FROM movies WHERE userRating IS NOT NULL")
    fun observeAverageRating(): Flow<Float?>

    /** All genres stored across saved items (for Stats screen — top genres). */
    @Query("SELECT genres FROM movies")
    fun observeAllGenres(): Flow<List<String>>

    // ── Media-type filtered queries ────────────────────────────────

    /** Observe items of a specific [mediaType] (`"movie"` or `"tv"`). */
    @Query("SELECT * FROM movies WHERE mediaType = :mediaType ORDER BY sortOrder ASC, dateAdded DESC")
    fun observeByMediaType(mediaType: String): Flow<List<MovieEntity>>

    /** Observe items filtered by both [watchStatus] and [mediaType]. */
    @Query("SELECT * FROM movies WHERE watchStatus = :watchStatus AND mediaType = :mediaType ORDER BY sortOrder ASC, dateAdded DESC")
    fun observeByStatusAndMediaType(watchStatus: String, mediaType: String): Flow<List<MovieEntity>>

    /** Total number of items with a given [mediaType]. */
    @Query("SELECT COUNT(*) FROM movies WHERE mediaType = :mediaType")
    fun observeCountByMediaType(mediaType: String): Flow<Int>

    /** Count per watch status filtered by [mediaType]. */
    @Query("SELECT watchStatus, COUNT(*) as count FROM movies WHERE mediaType = :mediaType GROUP BY watchStatus")
    fun observeStatusCountsByMediaType(mediaType: String): Flow<List<StatusCount>>

    /** Average user rating filtered by [mediaType]. */
    @Query("SELECT AVG(userRating) FROM movies WHERE userRating IS NOT NULL AND mediaType = :mediaType")
    fun observeAverageRatingByMediaType(mediaType: String): Flow<Float?>

    /** All genres filtered by [mediaType]. */
    @Query("SELECT genres FROM movies WHERE mediaType = :mediaType")
    fun observeAllGenresByMediaType(mediaType: String): Flow<List<String>>

    /** Returns true if there are saved items with null runtime (need metadata refresh). */
    @Query("SELECT EXISTS(SELECT 1 FROM movies WHERE runtime IS NULL LIMIT 1)")
    suspend fun hasItemsWithoutRuntime(): Boolean

    // ── Runtime stats ─────────────────────────────────────────────────
    // Movies: SUM(runtime) directly.
    // TV: SUM(perEpisodeRuntime × watchedEpisodeCount) via correlated subquery.

    @Query("SELECT COALESCE(SUM(runtime), 0) FROM movies WHERE mediaType = 'movie'")
    fun observeMovieRuntime(): Flow<Int>

    @Query("SELECT COALESCE(SUM(runtime), 0) FROM movies WHERE mediaType = 'movie' AND watchStatus = :watchStatus")
    fun observeMovieRuntimeByStatus(watchStatus: String): Flow<Int>

    // ── Status-filtered stats ───────────────────────────────────────

    /** Total number of items with a given [watchStatus]. */
    @Query("SELECT COUNT(*) FROM movies WHERE watchStatus = :watchStatus")
    fun observeCountByStatus(watchStatus: String): Flow<Int>

    /** Average user rating filtered by [watchStatus]. */
    @Query("SELECT AVG(userRating) FROM movies WHERE userRating IS NOT NULL AND watchStatus = :watchStatus")
    fun observeAverageRatingByStatus(watchStatus: String): Flow<Float?>

    /** All genres filtered by [watchStatus]. */
    @Query("SELECT genres FROM movies WHERE watchStatus = :watchStatus")
    fun observeAllGenresByStatus(watchStatus: String): Flow<List<String>>

    // ── Status + media-type filtered stats ──────────────────────────

    /** Total number of items with a given [watchStatus] and [mediaType]. */
    @Query("SELECT COUNT(*) FROM movies WHERE watchStatus = :watchStatus AND mediaType = :mediaType")
    fun observeCountByStatusAndMediaType(watchStatus: String, mediaType: String): Flow<Int>

    /** Average user rating filtered by [watchStatus] and [mediaType]. */
    @Query("SELECT AVG(userRating) FROM movies WHERE userRating IS NOT NULL AND watchStatus = :watchStatus AND mediaType = :mediaType")
    fun observeAverageRatingByStatusAndMediaType(watchStatus: String, mediaType: String): Flow<Float?>

    /** All genres filtered by [watchStatus] and [mediaType]. */
    @Query("SELECT genres FROM movies WHERE watchStatus = :watchStatus AND mediaType = :mediaType")
    fun observeAllGenresByStatusAndMediaType(watchStatus: String, mediaType: String): Flow<List<String>>
}

/** Projection class for the status-count aggregate query. */
data class StatusCount(
    val watchStatus: String,
    val count: Int,
)
