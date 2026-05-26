package com.example.cinetracker.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.cinetracker.CineTrackApplication
import com.example.cinetracker.data.network.NetworkConnectivityObserver
import com.example.cinetracker.data.repository.MovieRepository
import com.example.cinetracker.domain.model.MediaItem
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class SearchViewModel(
    private val movieRepository: MovieRepository,
    networkConnectivityObserver: NetworkConnectivityObserver,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val searchState = MutableStateFlow(SearchUiState())
    private val savedTmdbIds: StateFlow<Set<Int>> = movieRepository.observeSavedMovies()
        .map { items -> items.map { it.movie.tmdbId }.toSet() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STATE_FLOW_TIMEOUT_MILLIS),
            initialValue = emptySet(),
        )

    private var allResults: List<MediaItem> = emptyList()
    private var loadedRemotePages = 0
    private var remoteTotalPages = 0
    private var remoteTotalResults = 0

    val uiState: StateFlow<SearchUiState> = combine(
        searchState,
        savedTmdbIds,
    ) { state, savedIds ->
        state.copy(savedTmdbIds = savedIds)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STATE_FLOW_TIMEOUT_MILLIS),
        initialValue = SearchUiState(),
    )

    init {
        _query
            .debounce(DEBOUNCE_MILLIS)
            .map { it.trim() }
            .distinctUntilChanged()
            .onEach { trimmed ->
                if (trimmed.length < MIN_QUERY_LENGTH) {
                    resetSearch()
                } else {
                    performSearch(trimmed)
                }
            }
            .launchIn(viewModelScope)

        networkConnectivityObserver.isOnline
            .distinctUntilChanged()
            .onEach { online ->
                if (online && query.value.trim().length >= MIN_QUERY_LENGTH && searchState.value.errorMessage != null) {
                    retry()
                }
            }
            .launchIn(viewModelScope)
    }

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
    }

    fun clearQuery() {
        _query.value = ""
    }

    fun retry() {
        val trimmed = query.value.trim()
        if (trimmed.length < MIN_QUERY_LENGTH) return
        performSearch(trimmed)
    }

    fun goToPreviousPage() {
        val current = searchState.value.currentPage
        if (current <= 1) return
        updateVisiblePage(current - 1)
    }

    fun goToNextPage() {
        val current = searchState.value.currentPage
        val totalPages = searchState.value.totalPages
        if (current >= totalPages) return

        viewModelScope.launch {
            val targetPage = current + 1
            val requiredItems = minOf(targetPage * RESULTS_PER_PAGE, remoteTotalResults)

            if (allResults.size < requiredItems && loadedRemotePages < remoteTotalPages) {
                searchState.value = searchState.value.copy(
                    isLoadingMore = true,
                    errorMessage = null,
                )
                val loaded = ensureResultsForPage(targetPage, query.value.trim())
                if (!loaded) {
                    searchState.value = searchState.value.copy(
                        isLoadingMore = false,
                        errorMessage = UNKNOWN_ERROR,
                    )
                    return@launch
                }
            }

            updateVisiblePage(targetPage)
        }
    }

    fun addToWantToWatch(mediaItem: MediaItem) {
        viewModelScope.launch {
            movieRepository.saveAsWantToWatchIfMissing(mediaItem)
        }
    }

    private fun performSearch(trimmedQuery: String) {
        viewModelScope.launch {
            resetSearch(keepIdle = false)
            searchState.value = searchState.value.copy(isLoading = true)

            movieRepository.searchMultiPage(trimmedQuery, page = 1).fold(
                onSuccess = { pageResult ->
                    allResults = pageResult.items
                    loadedRemotePages = if (pageResult.items.isEmpty()) 0 else 1
                    remoteTotalPages = pageResult.totalPages
                    remoteTotalResults = pageResult.totalResults
                    updateVisiblePage(1)
                },
                onFailure = { throwable ->
                    resetSearch(keepIdle = false)
                    searchState.value = SearchUiState(
                        isIdle = false,
                        isLoading = false,
                        errorMessage = throwable.message ?: UNKNOWN_ERROR,
                    )
                },
            )
        }
    }

    private suspend fun ensureResultsForPage(targetPage: Int, query: String): Boolean {
        if (query.length < MIN_QUERY_LENGTH) return false

        val requiredItems = minOf(targetPage * RESULTS_PER_PAGE, remoteTotalResults)
        while (allResults.size < requiredItems && loadedRemotePages < remoteTotalPages) {
            val nextRemotePage = loadedRemotePages + 1
            val pageResult = movieRepository.searchMultiPage(query, nextRemotePage).getOrElse {
                return false
            }
            allResults = allResults + pageResult.items
            loadedRemotePages = nextRemotePage
            remoteTotalPages = pageResult.totalPages
            remoteTotalResults = pageResult.totalResults
        }
        return allResults.size >= requiredItems
    }

    private fun updateVisiblePage(page: Int) {
        val totalPages = if (remoteTotalResults == 0) 0 else ((remoteTotalResults - 1) / RESULTS_PER_PAGE) + 1
        val clampedPage = page.coerceIn(1, totalPages.coerceAtLeast(1))
        val startIndex = (clampedPage - 1) * RESULTS_PER_PAGE
        val endIndex = minOf(startIndex + RESULTS_PER_PAGE, allResults.size)
        val items = if (startIndex >= endIndex) emptyList() else allResults.subList(startIndex, endIndex)

        searchState.value = SearchUiState(
            isIdle = false,
            isLoading = false,
            isLoadingMore = false,
            items = items,
            currentPage = clampedPage,
            totalPages = totalPages,
            totalResults = remoteTotalResults,
            errorMessage = null,
        )
    }

    private fun resetSearch(keepIdle: Boolean = true) {
        allResults = emptyList()
        loadedRemotePages = 0
        remoteTotalPages = 0
        remoteTotalResults = 0
        searchState.value = SearchUiState(isIdle = keepIdle)
    }

    companion object {
        private const val DEBOUNCE_MILLIS = 350L
        private const val MIN_QUERY_LENGTH = 2
        private const val RESULTS_PER_PAGE = 15
        private const val STATE_FLOW_TIMEOUT_MILLIS = 5_000L
        private const val UNKNOWN_ERROR = "Unknown error"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as CineTrackApplication
                SearchViewModel(
                    movieRepository = app.serviceLocator.movieRepository,
                    networkConnectivityObserver = app.serviceLocator.networkConnectivityObserver,
                )
            }
        }
    }
}
