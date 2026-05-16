package com.example.cinetracker.data.export

import com.example.cinetracker.data.local.MovieDao
import com.example.cinetracker.data.local.MovieEntity
import com.example.cinetracker.data.local.WatchedEpisodeDao
import com.example.cinetracker.data.local.WatchedEpisodeEntity
import com.example.cinetracker.domain.model.WatchStatus
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json

class BackupRepository(
    private val movieDao: MovieDao,
    private val watchedEpisodeDao: WatchedEpisodeDao,
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

        val backup = BackupData(
            movies = movies.map { it.toBackup() },
            watchedEpisodes = allWatchedEpisodes.map { it.toBackup() },
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
        movieDao.deleteAll()

        backup.movies.forEach { backupMovie ->
            movieDao.upsert(backupMovie.toEntity())
        }
        backup.watchedEpisodes.forEach { backupEpisode ->
            watchedEpisodeDao.upsert(backupEpisode.toEntity())
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
    )

    private fun WatchedEpisodeEntity.toBackup() = BackupWatchedEpisode(
        tmdbId = tmdbId,
        seasonNumber = seasonNumber,
        episodeNumber = episodeNumber,
        watchedAt = watchedAt,
        runtime = runtime,
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
    )

    private fun BackupWatchedEpisode.toEntity() = WatchedEpisodeEntity(
        tmdbId = tmdbId,
        seasonNumber = seasonNumber,
        episodeNumber = episodeNumber,
        watchedAt = watchedAt,
        runtime = runtime,
    )
}

sealed interface ImportResult {
    data class Success(val count: Int) : ImportResult
    data object Error : ImportResult
}
