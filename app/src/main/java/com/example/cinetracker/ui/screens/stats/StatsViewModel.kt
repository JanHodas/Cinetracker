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
 * ViewModel for the Stats screen. Combines reactive Room queries for movies
 * and TV shows into separate sections displayed on one screen.
 */
class StatsViewModel(
    movieRepository: MovieRepository,
) : ViewModel() {

    private val movieStats = combine(
        movieRepository.observeTotalCountByMediaType(MEDIA_TYPE_MOVIE),
        movieRepository.observeStatusCountsByMediaType(MEDIA_TYPE_MOVIE),
        movieRepository.observeAverageRatingByMediaType(MEDIA_TYPE_MOVIE),
        movieRepository.observeTopGenresByMediaType(MEDIA_TYPE_MOVIE),
    ) { totalCount, statusCounts, averageRating, topGenres ->
        MediaStatsSection(
            totalCount = totalCount,
            statusCounts = statusCounts,
            averageRating = averageRating,
            topGenres = topGenres,
        )
    }

    private val tvStats = combine(
        movieRepository.observeTotalCountByMediaType(MEDIA_TYPE_TV),
        movieRepository.observeStatusCountsByMediaType(MEDIA_TYPE_TV),
        movieRepository.observeAverageRatingByMediaType(MEDIA_TYPE_TV),
        movieRepository.observeTopGenresByMediaType(MEDIA_TYPE_TV),
    ) { totalCount, statusCounts, averageRating, topGenres ->
        MediaStatsSection(
            totalCount = totalCount,
            statusCounts = statusCounts,
            averageRating = averageRating,
            topGenres = topGenres,
        )
    }

    val uiState: StateFlow<StatsUiState> = combine(
        movieStats,
        tvStats,
    ) { movieSection, tvSection ->
        StatsUiState(
            totalCount = movieSection.totalCount + tvSection.totalCount,
            movieStats = movieSection,
            tvStats = tvSection,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = StatsUiState(),
    )

    companion object {
        private const val MEDIA_TYPE_MOVIE = "movie"
        private const val MEDIA_TYPE_TV = "tv"

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
