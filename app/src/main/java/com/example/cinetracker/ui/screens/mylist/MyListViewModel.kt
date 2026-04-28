package com.example.cinetracker.ui.screens.mylist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.cinetracker.CineTrackApplication
import com.example.cinetracker.data.repository.MovieRepository
import com.example.cinetracker.domain.model.SavedMovie
import com.example.cinetracker.domain.model.WatchStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for the "My List" screen.
 *
 * Observes the Room-backed saved-movie list reactively. When the user
 * changes the filter chip, the upstream [Flow] switches via [flatMapLatest]
 * so Room only emits rows matching the selected status.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MyListViewModel(
    private val movieRepository: MovieRepository,
) : ViewModel() {

    private val _activeFilter = MutableStateFlow<WatchStatus?>(null)

    /** The reactive movie list for the currently selected filter. */
    private val filteredMovies = _activeFilter.flatMapLatest { status ->
        if (status == null) {
            movieRepository.observeSavedMovies()
        } else {
            movieRepository.observeSavedMoviesByStatus(status)
        }
    }

    val uiState: StateFlow<MyListUiState> = combine(
        _activeFilter,
        filteredMovies,
    ) { filter, movies ->
        MyListUiState(activeFilter = filter, movies = movies)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MyListUiState(),
    )

    /** Change the filter. Pass `null` for "All". */
    fun setFilter(status: WatchStatus?) {
        _activeFilter.value = status
    }

    /** Swipe-to-delete a movie from the list. */
    fun deleteMovie(savedMovie: SavedMovie) {
        viewModelScope.launch {
            movieRepository.removeMovie(savedMovie.movie.tmdbId)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as CineTrackApplication
                MyListViewModel(
                    movieRepository = app.serviceLocator.movieRepository,
                )
            }
        }
    }
}
