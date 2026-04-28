package com.example.cinetracker.ui.screens.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.cinetracker.CineTrackApplication
import com.example.cinetracker.data.repository.MovieRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * ViewModel for the Stats screen. Combines four reactive Room queries into
 * a single [StatsUiState] that updates automatically when the user's list
 * changes (e.g. adds/removes a movie on another screen).
 */
class StatsViewModel(
    movieRepository: MovieRepository,
) : ViewModel() {

    val uiState: StateFlow<StatsUiState> = combine(
        movieRepository.observeTotalCount(),
        movieRepository.observeStatusCounts(),
        movieRepository.observeAverageRating(),
        movieRepository.observeTopGenres(),
    ) { total, statusCounts, avgRating, topGenres ->
        StatsUiState(
            totalCount = total,
            statusCounts = statusCounts,
            averageRating = avgRating,
            topGenres = topGenres,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = StatsUiState(),
    )

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as CineTrackApplication
                StatsViewModel(
                    movieRepository = app.serviceLocator.movieRepository,
                )
            }
        }
    }
}
