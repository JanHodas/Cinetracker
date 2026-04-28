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
import com.example.cinetracker.domain.model.Movie
import com.example.cinetracker.domain.model.SavedMovie
import com.example.cinetracker.domain.model.WatchStatus
import com.example.cinetracker.ui.navigation.Route
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for the movie detail screen. Manages two independent concerns:
 *
 * 1. **TMDB detail** — fetched once and cached for the back-stack entry lifetime.
 *    Exposed as [uiState] (Loading / Success / Error). Auto-recovers on
 *    offline → online transitions.
 *
 * 2. **Saved state** — reactive Room observation of whether this movie is in
 *    the user's personal list. Exposed as [savedState] (flow of [SavedMovie?]).
 *    `null` means "not saved"; non-null carries watch status, rating and note.
 *
 * User actions (save, update status/rating/note, remove) go through dedicated
 * public functions that delegate to [MovieRepository].
 */
class DetailViewModel(
    private val movieRepository: MovieRepository,
    networkConnectivityObserver: NetworkConnectivityObserver,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val tmdbId: Int = checkNotNull(savedStateHandle[Route.Detail.ARG_TMDB_ID]) {
        "Missing ${Route.Detail.ARG_TMDB_ID} argument for DetailViewModel"
    }

    // ── TMDB detail ─────────────────────────────────────────────────

    private val _uiState = MutableStateFlow<DetailUiState>(DetailUiState.Loading)
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    // ── Saved (local) state ─────────────────────────────────────────

    /** `null` = movie is not in the user's list. */
    val savedState: StateFlow<SavedMovie?> = movieRepository.observeSavedState(tmdbId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // ── One-shot events (snackbar messages) ─────────────────────────

    private val _events = MutableSharedFlow<DetailEvent>()
    val events: SharedFlow<DetailEvent> = _events.asSharedFlow()

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

    // ── User actions ────────────────────────────────────────────────

    /** First-time save — picks the initial status. */
    fun saveToList(status: WatchStatus) {
        val movie = currentMovie() ?: return
        viewModelScope.launch {
            movieRepository.saveMovie(movie = movie, watchStatus = status)
            _events.emit(DetailEvent.MovieSaved)
        }
    }

    /** Change the watch status of an already-saved movie. */
    fun updateStatus(status: WatchStatus) {
        val movie = currentMovie() ?: return
        val saved = savedState.value ?: return
        viewModelScope.launch {
            movieRepository.updateStatus(tmdbId, movie, status, saved)
        }
    }

    /** Update the user's own 1–10 rating. */
    fun updateRating(rating: Float?) {
        val movie = currentMovie() ?: return
        val saved = savedState.value ?: return
        viewModelScope.launch {
            movieRepository.updateRating(movie, rating, saved)
        }
    }

    /** Update the personal note. */
    fun updateNote(note: String) {
        val movie = currentMovie() ?: return
        val saved = savedState.value ?: return
        viewModelScope.launch {
            movieRepository.updateNote(movie, note, saved)
        }
    }

    /** Remove the movie from the user's list entirely. */
    fun removeFromList() {
        viewModelScope.launch {
            movieRepository.removeMovie(tmdbId)
            _events.emit(DetailEvent.MovieRemoved)
        }
    }

    // ── Internal ────────────────────────────────────────────────────

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

    private fun currentMovie(): Movie? = (_uiState.value as? DetailUiState.Success)?.movie

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

/** One-shot events emitted by [DetailViewModel] for snackbar feedback. */
sealed interface DetailEvent {
    data object MovieSaved : DetailEvent
    data object MovieRemoved : DetailEvent
}
