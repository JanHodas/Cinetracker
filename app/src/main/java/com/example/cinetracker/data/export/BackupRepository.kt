package com.example.cinetracker.data.export

import com.example.cinetracker.data.local.MovieDao
import com.example.cinetracker.data.local.MovieEntity
import com.example.cinetracker.data.local.SeasonRatingDao
import com.example.cinetracker.data.local.SeasonRatingEntity
import com.example.cinetracker.data.local.WatchedEpisodeDao
import com.example.cinetracker.data.local.WatchedEpisodeEntity
import com.example.cinetracker.domain.model.WatchStatus
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json

class BackupRepository(
    private val movieDao: MovieDao,
    private val watchedEpisodeDao: WatchedEpisodeDao,
    private val seasonRatingDao: SeasonRatingDao,
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    suspend fun exportToJson(): String {
        val movies = movieDao.observeAll().first()
        val allWatchedEpisodes = movies
            .filter { it.mediaType == "tv" }
            .flatMap { watchedEpisodeDao.getByTmdbId(it.tmdbId) }
        val allSeasonRatings = movies
            .filter { it.mediaType == "tv" }
            .flatMap { seasonRatingDao.getByTmdbId(it.tmdbId) }

        val backup = BackupData(
            movies = movies.map { it.toBackup() },
            watchedEpisodes = allWatchedEpisodes.map { it.toBackup() },
            seasonRatings = allSeasonRatings.map { it.toBackup() },
        )
        return json.encodeToString(BackupData.serializer(), backup)
    }

    suspend fun importFromJson(jsonString: String): ImportResult {
        val backup = try {
            json.decodeFromString(BackupData.serializer(), jsonString)
        } catch (e: Exception) {
            return ImportResult.Error
        }

        watchedEpisodeDao.deleteAll()
        seasonRatingDao.deleteAll()
        movieDao.deleteAll()

        backup.movies.forEach { backupMovie ->
            movieDao.upsert(backupMovie.toEntity())
        }
        backup.watchedEpisodes.forEach { backupEpisode ->
            watchedEpisodeDao.upsert(backupEpisode.toEntity())
        }
        backup.seasonRatings.forEach { backupSeasonRating ->
            seasonRatingDao.upsert(backupSeasonRating.toEntity())
        }
        return ImportResult.Success(backup.movies.size)
    }

    private fun MovieEntity.toBackup() = BackupMovie(
        tmdbId = tmdbId,
        title = title,
        overview = overview,
        posterPath = posterPath,
        backdropPath = backdropPath,
        releaseDate = releaseDate,
        genres = genres,
        tmdbRating = tmdbRating,
        watchStatus = watchStatus.name,
        userRating = userRating,
        note = note,
        dateAdded = dateAdded,
        mediaType = mediaType,
        numberOfSeasons = numberOfSeasons,
        numberOfEpisodes = numberOfEpisodes,
        runtime = runtime,
        seasonEpisodeCounts = seasonEpisodeCounts,
        sortOrder = sortOrder,
    )

    private fun WatchedEpisodeEntity.toBackup() = BackupWatchedEpisode(
        tmdbId = tmdbId,
        seasonNumber = seasonNumber,
        episodeNumber = episodeNumber,
        watchedAt = watchedAt,
        runtime = runtime,
    )

    private fun SeasonRatingEntity.toBackup() = BackupSeasonRating(
        tmdbId = tmdbId,
        seasonNumber = seasonNumber,
        userRating = userRating,
        ratedAt = ratedAt,
    )

    private fun BackupMovie.toEntity() = MovieEntity(
        tmdbId = tmdbId,
        title = title,
        overview = overview,
        posterPath = posterPath,
        backdropPath = backdropPath,
        releaseDate = releaseDate,
        genres = genres,
        tmdbRating = tmdbRating,
        watchStatus = try { WatchStatus.valueOf(watchStatus) } catch (_: Exception) { WatchStatus.WANT_TO_WATCH },
        userRating = userRating,
        note = note,
        dateAdded = dateAdded,
        mediaType = mediaType,
        numberOfSeasons = numberOfSeasons,
        numberOfEpisodes = numberOfEpisodes,
        runtime = runtime,
        seasonEpisodeCounts = seasonEpisodeCounts,
        sortOrder = sortOrder,
    )

    private fun BackupWatchedEpisode.toEntity() = WatchedEpisodeEntity(
        tmdbId = tmdbId,
        seasonNumber = seasonNumber,
        episodeNumber = episodeNumber,
        watchedAt = watchedAt,
        runtime = runtime,
    )

    private fun BackupSeasonRating.toEntity() = SeasonRatingEntity(
        tmdbId = tmdbId,
        seasonNumber = seasonNumber,
        userRating = userRating,
        ratedAt = ratedAt,
    )
}

sealed interface ImportResult {
    data class Success(val count: Int) : ImportResult
    data object Error : ImportResult
}
