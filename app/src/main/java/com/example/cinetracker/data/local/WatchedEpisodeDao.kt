package com.example.cinetracker.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * Data-access object for episode watch tracking.
 *
 * Each row represents one watched episode of a TV show. The table is
 * independent of the `movies` table — episodes can be tracked regardless
 * of whether the show is saved to the personal list.
 */
@Dao
interface WatchedEpisodeDao {

    /** Mark an episode as watched (or update its timestamp if already marked). */
    @Upsert
    suspend fun upsert(entity: WatchedEpisodeEntity)

    /** Remove a single watched-episode record. */
    @Query(
        "DELETE FROM watched_episodes " +
            "WHERE tmdbId = :tmdbId AND seasonNumber = :seasonNumber AND episodeNumber = :episodeNumber",
    )
    suspend fun delete(tmdbId: Int, seasonNumber: Int, episodeNumber: Int)

    /** Observe all watched episodes for a specific TV show (for DetailScreen). */
    @Query("SELECT * FROM watched_episodes WHERE tmdbId = :tmdbId")
    fun observeByTmdbId(tmdbId: Int): Flow<List<WatchedEpisodeEntity>>

    /** One-shot read of all watched episodes for a specific TV show. */
    @Query("SELECT * FROM watched_episodes WHERE tmdbId = :tmdbId")
    suspend fun getByTmdbId(tmdbId: Int): List<WatchedEpisodeEntity>

    /** Observe per-show watched counts for all shows (for MyList badges). */
    @Query("SELECT tmdbId, COUNT(*) AS count FROM watched_episodes GROUP BY tmdbId")
    fun observeAllCounts(): Flow<List<EpisodeWatchCount>>

    /** Remove all watched episodes for a TV show (cleanup on list removal). */
    @Query("DELETE FROM watched_episodes WHERE tmdbId = :tmdbId")
    suspend fun deleteByTmdbId(tmdbId: Int)

    /** Remove all watched episodes. */
    @Query("DELETE FROM watched_episodes")
    suspend fun deleteAll()

    /** Check if any watched episodes are missing runtime data. */
    @Query("SELECT EXISTS(SELECT 1 FROM watched_episodes WHERE runtime IS NULL LIMIT 1)")
    suspend fun hasEpisodesWithoutRuntime(): Boolean

    // ── Runtime stats ─────────────────────────────────────────────────

    /** Total watched TV runtime across all shows. */
    @Query("SELECT COALESCE(SUM(runtime), 0) FROM watched_episodes")
    fun observeTotalWatchedRuntime(): Flow<Int>

    /** Total watched TV runtime for shows matching [watchStatus]. */
    @Query("""
        SELECT COALESCE(SUM(w.runtime), 0) FROM watched_episodes w
        INNER JOIN movies m ON w.tmdbId = m.tmdbId
        WHERE m.watchStatus = :watchStatus
    """)
    fun observeWatchedRuntimeByStatus(watchStatus: String): Flow<Int>
}

/** Projection for per-show episode watch count. */
data class EpisodeWatchCount(
    val tmdbId: Int,
    val count: Int,
)
