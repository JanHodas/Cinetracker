package com.example.cinetracker.ui.screens.mylist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.cinetracker.CineTrackApplication
import com.example.cinetracker.data.repository.DeletedSavedItem
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
 * Observes the Room-backed saved list reactively. The active watch-status
 * filter and media-type filter are combined so Room only emits rows matching
 * the currently selected filter pair.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MyListViewModel(
    private val movieRepository: MovieRepository,
) : ViewModel() {

    private val activeStatusFilter = MutableStateFlow<WatchStatus?>(null)
    private val activeMediaTypeFilter = MutableStateFlow<String?>(null)
    private val itemRenderVersions = MutableStateFlow<Map<Int, Int>>(emptyMap())

    private var lastDeletedItem: DeletedSavedItem? = null

    private val filteredItems = combine(
        activeStatusFilter,
        activeMediaTypeFilter,
    ) { status, mediaType ->
        status to mediaType
    }.flatMapLatest { (status, mediaType) ->
        when {
            status == null && mediaType == null -> movieRepository.observeSavedMovies()
            status != null && mediaType == null -> movieRepository.observeSavedMoviesByStatus(status)
            status == null && mediaType != null -> movieRepository.observeSavedByMediaType(mediaType)
            else -> movieRepository.observeSavedByStatusAndMediaType(
                status = checkNotNull(status),
                mediaType = checkNotNull(mediaType),
            )
        }
    }

    private val watchedCounts = movieRepository.observeAllWatchedCounts()

    val uiState: StateFlow<MyListUiState> = combine(
        activeStatusFilter,
        activeMediaTypeFilter,
        filteredItems,
        watchedCounts,
        itemRenderVersions,
    ) { statusFilter, mediaTypeFilter, items, counts, renderVersions ->
        MyListUiState(
            activeStatusFilter = statusFilter,
            activeMediaTypeFilter = mediaTypeFilter,
            items = items,
            watchedEpisodeCounts = counts,
            itemRenderVersions = renderVersions,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MyListUiState(),
    )

    fun setStatusFilter(status: WatchStatus?) {
        activeStatusFilter.value = status
    }

    fun setMediaTypeFilter(mediaType: String?) {
        activeMediaTypeFilter.value = mediaType
    }

    suspend fun deleteMovie(savedMovie: SavedMovie) {
        lastDeletedItem = movieRepository.deleteSavedItem(savedMovie)
    }

    suspend fun restoreLastDeleted(): Boolean {
        val deletedItem = lastDeletedItem ?: return false
        movieRepository.restoreDeletedItem(deletedItem)
        val tmdbId = deletedItem.savedMovie.movie.tmdbId
        itemRenderVersions.value = itemRenderVersions.value.toMutableMap().apply {
            this[tmdbId] = (this[tmdbId] ?: 0) + 1
        }
        lastDeletedItem = null
        return true
    }

    /** Mark the next unwatched episode for a TV show (MAL-style "+" button). */
    fun incrementEpisode(tmdbId: Int) {
        viewModelScope.launch {
            movieRepository.markNextEpisodeWatched(tmdbId)
        }
    }

    companion object {
        const val MEDIA_TYPE_MOVIE = "movie"
        const val MEDIA_TYPE_TV = "tv"

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
