package com.example.cinetracker.di

import android.content.Context
import com.example.cinetracker.data.local.CineTrackDatabase
import com.example.cinetracker.data.local.MovieDao
import com.example.cinetracker.data.local.SeasonRatingDao
import com.example.cinetracker.data.local.WatchedEpisodeDao
import com.example.cinetracker.data.network.NetworkConnectivityObserver
import com.example.cinetracker.data.remote.NetworkModule
import com.example.cinetracker.data.remote.TmdbApi
import com.example.cinetracker.data.export.BackupRepository
import com.example.cinetracker.data.repository.MovieRepository

/**
 * Manual dependency container.
 *
 * Held by [com.example.cinetracker.CineTrackApplication] and accessed from ViewModel factories.
 * Hilt was deliberately skipped to keep build times low and the DI surface inspectable; the
 * project is small enough that one container is enough.
 */
class ServiceLocator(
    private val applicationContext: Context,
) {
    private val database: CineTrackDatabase by lazy {
        CineTrackDatabase.getInstance(applicationContext)
    }
    val movieDao: MovieDao by lazy { database.movieDao() }
    val watchedEpisodeDao: WatchedEpisodeDao by lazy { database.watchedEpisodeDao() }
    val seasonRatingDao: SeasonRatingDao by lazy { database.seasonRatingDao() }
    val tmdbApi: TmdbApi = NetworkModule.tmdbApi
    val movieRepository: MovieRepository by lazy {
        MovieRepository(
            applicationContext = applicationContext,
            tmdbApi = tmdbApi,
            movieDao = movieDao,
            watchedEpisodeDao = watchedEpisodeDao,
            seasonRatingDao = seasonRatingDao,
        )
    }
    val networkConnectivityObserver: NetworkConnectivityObserver by lazy {
        NetworkConnectivityObserver(applicationContext)
    }
    val backupRepository: BackupRepository by lazy {
        BackupRepository(
            movieDao = movieDao,
            watchedEpisodeDao = watchedEpisodeDao,
            seasonRatingDao = seasonRatingDao,
        )
    }
}
