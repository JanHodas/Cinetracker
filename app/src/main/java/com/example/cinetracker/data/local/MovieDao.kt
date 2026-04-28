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

    /** Observe all saved movies ordered by the date they were added (newest first). */
    @Query("SELECT * FROM movies ORDER BY dateAdded DESC")
    fun observeAll(): Flow<List<MovieEntity>>

    /** Observe only movies with a specific [watchStatus]. */
    @Query("SELECT * FROM movies WHERE watchStatus = :watchStatus ORDER BY dateAdded DESC")
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

    /** Total number of saved movies (for Stats screen). */
    @Query("SELECT COUNT(*) FROM movies")
    fun observeCount(): Flow<Int>

    /** Count of movies per watch status (for Stats screen). */
    @Query("SELECT watchStatus, COUNT(*) as count FROM movies GROUP BY watchStatus")
    fun observeStatusCounts(): Flow<List<StatusCount>>

    /** Average user rating across all rated movies (for Stats screen). */
    @Query("SELECT AVG(userRating) FROM movies WHERE userRating IS NOT NULL")
    fun observeAverageRating(): Flow<Float?>

    /** All genres stored across saved movies (for Stats screen — top genres). */
    @Query("SELECT genres FROM movies")
    fun observeAllGenres(): Flow<List<String>>
}

/** Projection class for the status-count aggregate query. */
data class StatusCount(
    val watchStatus: String,
    val count: Int,
)
