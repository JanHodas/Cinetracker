package com.example.cinetracker.ui.screens.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.cinetracker.CineTrackApplication
import com.example.cinetracker.data.export.BackupRepository
import com.example.cinetracker.data.export.ImportResult
import com.example.cinetracker.data.repository.MovieRepository
import com.example.cinetracker.domain.model.WatchStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class StatsViewModel(
    private val movieRepository: MovieRepository,
    private val backupRepository: BackupRepository,
) : ViewModel() {

    private val _backupEvents = MutableSharedFlow<BackupEvent>()
    val backupEvents: SharedFlow<BackupEvent> = _backupEvents.asSharedFlow()

    private val activeStatusFilter = MutableStateFlow<WatchStatus?>(null)
    private val activeMediaTypeFilter = MutableStateFlow<String?>(null)

    private val filteredStats = combine(
        activeStatusFilter,
        activeMediaTypeFilter,
    ) { status, mediaType ->
        status to mediaType
    }.flatMapLatest { (status, mediaType) ->
        val totalCountFlow = when {
            status == null && mediaType == null -> movieRepository.observeTotalCount()
            status != null && mediaType == null -> movieRepository.observeTotalCountByStatus(status)
            status == null && mediaType != null -> movieRepository.observeTotalCountByMediaType(mediaType)
            else -> movieRepository.observeTotalCountByStatusAndMediaType(
                status = checkNotNull(status), mediaType = checkNotNull(mediaType),
            )
        }
        val averageRatingFlow = when {
            status == null && mediaType == null -> movieRepository.observeAverageRating()
            status != null && mediaType == null -> movieRepository.observeAverageRatingByStatus(status)
            status == null && mediaType != null -> movieRepository.observeAverageRatingByMediaType(mediaType)
            else -> movieRepository.observeAverageRatingByStatusAndMediaType(
                status = checkNotNull(status), mediaType = checkNotNull(mediaType),
            )
        }
        val topGenresFlow = when {
            status == null && mediaType == null -> movieRepository.observeTopGenres()
            status != null && mediaType == null -> movieRepository.observeTopGenresByStatus(status)
            status == null && mediaType != null -> movieRepository.observeTopGenresByMediaType(mediaType)
            else -> movieRepository.observeTopGenresByStatusAndMediaType(
                status = checkNotNull(status), mediaType = checkNotNull(mediaType),
            )
        }
        val statusCountsFlow = when {
            mediaType == null -> movieRepository.observeStatusCounts()
            else -> movieRepository.observeStatusCountsByMediaType(mediaType)
        }
        val totalRuntimeFlow = when {
            status == null && mediaType == null -> movieRepository.observeTotalRuntime()
            status != null && mediaType == null -> movieRepository.observeTotalRuntimeByStatus(status)
            status == null && mediaType != null -> movieRepository.observeTotalRuntimeByMediaType(mediaType)
            else -> movieRepository.observeTotalRuntimeByStatusAndMediaType(
                status = checkNotNull(status), mediaType = checkNotNull(mediaType),
            )
        }

        combine(totalCountFlow, averageRatingFlow, topGenresFlow, statusCountsFlow, totalRuntimeFlow) {
                totalCount, averageRating, topGenres, statusCounts, totalRuntime ->
            StatsData(
                totalCount = totalCount,
                averageRating = averageRating,
                topGenres = topGenres,
                statusCounts = statusCounts,
                totalRuntimeMinutes = totalRuntime,
            )
        }
    }

    val uiState: StateFlow<StatsUiState> = combine(
        activeStatusFilter,
        activeMediaTypeFilter,
        filteredStats,
    ) { statusFilter, mediaTypeFilter, data ->
        StatsUiState(
            activeStatusFilter = statusFilter,
            activeMediaTypeFilter = mediaTypeFilter,
            totalCount = data.totalCount,
            statusCounts = data.statusCounts,
            averageRating = data.averageRating,
            topGenres = data.topGenres,
            totalRuntimeMinutes = data.totalRuntimeMinutes,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = StatsUiState(),
    )

    fun setStatusFilter(status: WatchStatus?) {
        activeStatusFilter.value = status
    }

    fun setMediaTypeFilter(mediaType: String?) {
        activeMediaTypeFilter.value = mediaType
    }

    fun exportData(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val jsonData = backupRepository.exportToJson()
            onResult(jsonData)
        }
    }

    fun importData(jsonString: String) {
        viewModelScope.launch {
            val result = backupRepository.importFromJson(jsonString)
            _backupEvents.emit(
                when (result) {
                    is ImportResult.Success -> BackupEvent.ImportSuccess(result.count)
                    is ImportResult.Error -> BackupEvent.ImportError
                },
            )
        }
    }

    companion object {
        const val MEDIA_TYPE_MOVIE = "movie"
        const val MEDIA_TYPE_TV = "tv"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as CineTrackApplication
                StatsViewModel(
                    movieRepository = app.serviceLocator.movieRepository,
                    backupRepository = app.serviceLocator.backupRepository,
                )
            }
        }
    }
}

sealed interface BackupEvent {
    data class ImportSuccess(val count: Int) : BackupEvent
    data object ImportError : BackupEvent
}

private data class StatsData(
    val totalCount: Int,
    val averageRating: Float?,
    val topGenres: List<Pair<String, Int>>,
    val statusCounts: Map<WatchStatus, Int>,
    val totalRuntimeMinutes: Int,
)
