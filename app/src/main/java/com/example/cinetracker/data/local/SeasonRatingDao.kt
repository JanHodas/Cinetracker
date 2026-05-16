package com.example.cinetracker.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** Data-access object for per-season user ratings. */
@Dao
interface SeasonRatingDao {

    @Upsert
    suspend fun upsert(entity: SeasonRatingEntity)

    @Query("SELECT * FROM season_ratings WHERE tmdbId = :tmdbId")
    fun observeByTmdbId(tmdbId: Int): Flow<List<SeasonRatingEntity>>

    @Query("SELECT * FROM season_ratings WHERE tmdbId = :tmdbId")
    suspend fun getByTmdbId(tmdbId: Int): List<SeasonRatingEntity>

    @Query("DELETE FROM season_ratings WHERE tmdbId = :tmdbId AND seasonNumber = :seasonNumber")
    suspend fun delete(tmdbId: Int, seasonNumber: Int)

    @Query("DELETE FROM season_ratings WHERE tmdbId = :tmdbId")
    suspend fun deleteByTmdbId(tmdbId: Int)

    @Query("DELETE FROM season_ratings")
    suspend fun deleteAll()
}
