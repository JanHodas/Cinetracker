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
import com.example.cinetracker.domain.model.CastMember
import com.example.cinetracker.domain.model.MediaItem
import com.example.cinetracker.domain.model.SavedMovie
import com.example.cinetracker.domain.model.Season
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
 * ViewModel for the media detail screen.
 *
 * It loads either movie or TV detail based on the navigation argument, observes
 * the locally-saved state from Room, and exposes user actions for watch status,
 * rating, note updates and removal.
 */
class DetailViewModel(
    private val movieRepository: MovieRepository,
    networkConnectivityObserver: NetworkConnectivityObserver,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val mediaType: String = checkNotNull(savedStateHandle[Route.Detail.ARG_MEDIA_TYPE]) {
        "Missing ${Route.Detail.ARG_MEDIA_TYPE} argument for DetailViewModel"
    }

    private val tmdbId: Int = checkNotNull(savedStateHandle[Route.Detail.ARG_TMDB_ID]) {
        "Missing ${Route.Detail.ARG_TMDB_ID} argument for DetailViewModel"
    }

    private val _uiState = MutableStateFlow<DetailUiState>(DetailUiState.Loading)
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    val savedState: StateFlow<SavedMovie?> = movieRepository.observeSavedState(tmdbId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

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

    fun saveToList(status: WatchStatus) {
        val mediaItem = currentMediaItem() ?: return
        viewModelScope.launch {
            movieRepository.saveMedia(mediaItem = mediaItem, watchStatus = status)
            _events.emit(DetailEvent.MovieSaved)
        }
    }

    fun updateStatus(status: WatchStatus) {
        val mediaItem = currentMediaItem() ?: return
        val saved = savedState.value ?: return
        viewModelScope.launch {
            movieRepository.updateStatus(tmdbId, mediaItem, status, saved)
        }
    }

    fun updateRating(rating: Float?) {
        val mediaItem = currentMediaItem() ?: return
        val saved = savedState.value ?: return
        viewModelScope.launch {
            movieRepository.updateRating(mediaItem, rating, saved)
        }
    }

    fun updateNote(note: String) {
        val mediaItem = currentMediaItem() ?: return
        val saved = savedState.value ?: return
        viewModelScope.launch {
            movieRepository.updateNote(mediaItem, note, saved)
        }
    }

    fun removeFromList() {
        viewModelScope.launch {
            movieRepository.removeMovie(tmdbId)
            _events.emit(DetailEvent.MovieRemoved)
        }
    }

    private fun loadDetail() {
        _uiState.value = DetailUiState.Loading
        viewModelScope.launch {
            when (mediaType) {
                MEDIA_TYPE_TV -> loadTvDetail()
                else -> loadMovieDetail()
            }
        }
    }

    private suspend fun loadMovieDetail() {
        movieRepository.getMovieDetail(tmdbId).fold(
            onSuccess = { movie ->
                _uiState.value = DetailUiState.Success(mediaItem = movie)
                loadCastForCurrentItem()
            },
            onFailure = { throwable ->
                _uiState.value = DetailUiState.Error(throwable.message ?: UNKNOWN_ERROR)
            },
        )
    }

    private suspend fun loadTvDetail() {
        movieRepository.getTvDetail(tmdbId).fold(
            onSuccess = { tvShow ->
                _uiState.value = DetailUiState.Success(
                    mediaItem = tvShow,
                    seasons = loadTvSeasons(tvShow.numberOfSeasons),
                )
                loadCastForCurrentItem()
            },
            onFailure = { throwable ->
                _uiState.value = DetailUiState.Error(throwable.message ?: UNKNOWN_ERROR)
            },
        )
    }

    private suspend fun loadTvSeasons(numberOfSeasons: Int): List<Season> {
        if (numberOfSeasons <= 0) return emptyList()
        return (1..numberOfSeasons).mapNotNull { seasonNumber ->
            movieRepository.getTvSeason(tmdbId, seasonNumber).getOrNull()
        }
    }

    private fun currentMediaItem(): MediaItem? =
        (_uiState.value as? DetailUiState.Success)?.mediaItem

    private suspend fun loadCastForCurrentItem() {
        val successState = _uiState.value as? DetailUiState.Success ?: return
        val cast = when (mediaType) {
            MEDIA_TYPE_TV -> movieRepository.getTvCast(tmdbId).getOrDefault(emptyList())
            else -> movieRepository.getMovieCast(tmdbId).getOrDefault(emptyList())
        }
        _uiState.value = successState.copy(cast = cast.take(MAX_CAST_MEMBERS))
    }

    companion object {
        private const val MEDIA_TYPE_TV = "tv"
        private const val MAX_CAST_MEMBERS = 10
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

sealed interface DetailEvent {
    data object MovieSaved : DetailEvent
    data object MovieRemoved : DetailEvent
}
