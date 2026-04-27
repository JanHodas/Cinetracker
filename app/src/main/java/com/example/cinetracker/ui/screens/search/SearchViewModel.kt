package com.example.cinetracker.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.cinetracker.CineTrackApplication
import com.example.cinetracker.data.repository.MovieRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

/**
 * ViewModel backing the Search screen. Exposes the current text in [query] and a derived
 * [uiState] that debounces user input, calls the repository and maps results to a [SearchUiState].
 *
 * The ViewModel survives configuration changes, so the rotation test in the spec passes
 * automatically: query text and last results are preserved without any extra plumbing.
 *
 * @see DEBOUNCE_MILLIS for the typing settle delay
 * @see MIN_QUERY_LENGTH minimum number of characters before a TMDB call is fired
 */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class SearchViewModel(
    private val movieRepository: MovieRepository,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    /** Bumped by [retry] to re-fire the last query after a transient failure. */
    private val retryTick = MutableStateFlow(0)

    val uiState: StateFlow<SearchUiState> = combine(
        _query.debounce(DEBOUNCE_MILLIS).distinctUntilChanged(),
        retryTick,
    ) { q, _ -> q }
        .flatMapLatest { rawQuery ->
            flow {
                val trimmed = rawQuery.trim()
                if (trimmed.length < MIN_QUERY_LENGTH) {
                    emit(SearchUiState.Idle)
                    return@flow
                }
                emit(SearchUiState.Loading)
                movieRepository.searchMovies(trimmed).fold(
                    onSuccess = { movies ->
                        emit(
                            if (movies.isEmpty()) SearchUiState.Empty
                            else SearchUiState.Success(movies)
                        )
                    },
                    onFailure = { throwable ->
                        emit(SearchUiState.Error(throwable.message ?: UNKNOWN_ERROR))
                    },
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STATE_FLOW_TIMEOUT_MILLIS),
            initialValue = SearchUiState.Idle,
        )

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
    }

    fun clearQuery() {
        _query.value = ""
    }

    fun retry() {
        retryTick.value++
    }

    companion object {
        private const val DEBOUNCE_MILLIS = 350L
        private const val MIN_QUERY_LENGTH = 2
        private const val STATE_FLOW_TIMEOUT_MILLIS = 5_000L
        private const val UNKNOWN_ERROR = "Unknown error"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as CineTrackApplication
                SearchViewModel(app.serviceLocator.movieRepository)
            }
        }
    }
}
