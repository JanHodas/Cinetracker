package com.example.cinetracker.ui.screens.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.cinetracker.CineTrackApplication
import com.example.cinetracker.data.network.NetworkConnectivityObserver
import com.example.cinetracker.data.repository.MovieRepository
import com.example.cinetracker.ui.navigation.Route
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * ViewModel for the movie detail screen. Reads the TMDB id from the navigation
 * arguments via [SavedStateHandle], fetches the rich detail payload once and
 * exposes it as [DetailUiState].
 *
 * The detail payload is cached in the ViewModel for the duration of the back-stack
 * entry, so rotating the screen or returning to it from the system navigator does
 * not trigger another network call.
 *
 * If the initial load fails because of a connectivity issue, the screen also
 * auto-recovers when the network returns: an internal observer watches
 * [NetworkConnectivityObserver] and re-fires the load on the offline → online
 * transition while the UI is in [DetailUiState.Error].
 */
class DetailViewModel(
    private val movieRepository: MovieRepository,
    networkConnectivityObserver: NetworkConnectivityObserver,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val tmdbId: Int = checkNotNull(savedStateHandle[Route.Detail.ARG_TMDB_ID]) {
        "Missing ${Route.Detail.ARG_TMDB_ID} argument for DetailViewModel"
    }

    private val _uiState = MutableStateFlow<DetailUiState>(DetailUiState.Loading)
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    init {
        loadDetail()

        networkConnectivityObserver.isOnline
            .distinctUntilChanged()
            .onEach { online ->
                if (online && _uiState.value is DetailUiState.Error) {
                    loadDetail()
                }
            }
            .launchIn(viewModelScope)
    }

    fun retry() {
        loadDetail()
    }

    private fun loadDetail() {
        _uiState.value = DetailUiState.Loading
        viewModelScope.launch {
            movieRepository.getMovieDetail(tmdbId).fold(
                onSuccess = { movie -> _uiState.value = DetailUiState.Success(movie) },
                onFailure = { throwable ->
                    _uiState.value = DetailUiState.Error(throwable.message ?: UNKNOWN_ERROR)
                },
            )
        }
    }

    companion object {
        private const val UNKNOWN_ERROR = "Unknown error"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as CineTrackApplication
                DetailViewModel(
                    movieRepository = app.serviceLocator.movieRepository,
                    networkConnectivityObserver = app.serviceLocator.networkConnectivityObserver,
                    savedStateHandle = createSavedStateHandle(),
                )
            }
        }
    }
}
