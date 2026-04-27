package com.example.cinetracker.di

import android.content.Context
import com.example.cinetracker.data.remote.NetworkModule
import com.example.cinetracker.data.remote.TmdbApi
import com.example.cinetracker.data.repository.MovieRepository

/**
 * Manual dependency container.
 *
 * Held by [com.example.cinetracker.CineTrackApplication] and accessed from ViewModel factories.
 * Hilt was deliberately skipped to keep build times low and the DI surface inspectable; the
 * project is small enough that one container is enough.
 *
 * @param applicationContext kept around for components that will need it later (Room database,
 *  WorkManager scheduler, etc.). Currently unused but reserved.
 */
class ServiceLocator(
    @Suppress("unused") private val applicationContext: Context,
) {
    val tmdbApi: TmdbApi = NetworkModule.tmdbApi
    val movieRepository: MovieRepository by lazy { MovieRepository(tmdbApi) }
}
