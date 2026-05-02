package com.example.cinetracker.ui.screens.mylist

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cinetracker.R
import com.example.cinetracker.domain.model.Movie
import com.example.cinetracker.domain.model.SavedMovie
import com.example.cinetracker.domain.model.TvShow
import com.example.cinetracker.domain.model.WatchStatus
import com.example.cinetracker.ui.components.MovieListItem
import kotlinx.coroutines.launch
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyListScreen(
    onItemClick: (mediaType: String, tmdbId: Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MyListViewModel = viewModel(factory = MyListViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val deletedMessage = stringResource(R.string.mylist_deleted_snackbar)
    val undoLabel = stringResource(R.string.mylist_undo)

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.mylist_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            StatusFilterRow(
                activeFilter = uiState.activeStatusFilter,
                onFilterSelected = viewModel::setStatusFilter,
            )

            MediaTypeFilterRow(
                activeFilter = uiState.activeMediaTypeFilter,
                onFilterSelected = viewModel::setMediaTypeFilter,
            )

            if (uiState.items.isEmpty()) {
                EmptyState(
                    hasStatusFilter = uiState.activeStatusFilter != null,
                    hasMediaTypeFilter = uiState.activeMediaTypeFilter != null,
                )
            } else {
                MovieList(
                    items = uiState.items,
                    watchedEpisodeCounts = uiState.watchedEpisodeCounts,
                    itemRenderVersions = uiState.itemRenderVersions,
                    onItemClick = onItemClick,
                    onDelete = { saved ->
                        scope.launch {
                            viewModel.deleteMovie(saved)
                            snackbarHostState.currentSnackbarData?.dismiss()
                            val result = snackbarHostState.showSnackbar(
                                message = deletedMessage,
                                actionLabel = undoLabel,
                                duration = SnackbarDuration.Long,
                            )
                            if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                                viewModel.restoreLastDeleted()
                            }
                        }
                    },
                    onIncrementEpisode = viewModel::incrementEpisode,
                )
            }
        }
    }
}

@Composable
private fun StatusFilterRow(
    activeFilter: WatchStatus?,
    onFilterSelected: (WatchStatus?) -> Unit,
) {
    val filters = listOf(
        null to stringResource(R.string.mylist_filter_all),
        WatchStatus.WANT_TO_WATCH to stringResource(R.string.mylist_filter_want),
        WatchStatus.WATCHING to stringResource(R.string.mylist_filter_watching),
        WatchStatus.WATCHED to stringResource(R.string.mylist_filter_watched),
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        filters.forEach { (status, label) ->
            FilterChip(
                selected = activeFilter == status,
                onClick = { onFilterSelected(status) },
                label = { Text(label) },
            )
        }
    }
}

@Composable
private fun MediaTypeFilterRow(
    activeFilter: String?,
    onFilterSelected: (String?) -> Unit,
) {
    val filters = listOf(
        null to stringResource(R.string.mylist_media_all),
        MyListViewModel.MEDIA_TYPE_MOVIE to stringResource(R.string.mylist_media_movies),
        MyListViewModel.MEDIA_TYPE_TV to stringResource(R.string.mylist_media_tv),
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 0.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        filters.forEach { (mediaType, label) ->
            FilterChip(
                selected = activeFilter == mediaType,
                onClick = { onFilterSelected(mediaType) },
                label = { Text(label) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MovieList(
    items: List<SavedMovie>,
    watchedEpisodeCounts: Map<Int, Int>,
    itemRenderVersions: Map<Int, Int>,
    onItemClick: (mediaType: String, tmdbId: Int) -> Unit,
    onDelete: (SavedMovie) -> Unit,
    onIncrementEpisode: (tmdbId: Int) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp, top = 8.dp),
    ) {
        items(
            items = items,
            key = { savedMovie ->
                "${savedMovie.movie.tmdbId}-${itemRenderVersions[savedMovie.movie.tmdbId] ?: 0}"
            },
        ) { savedMovie ->
            val mediaType = when (savedMovie.movie) {
                is Movie -> MyListViewModel.MEDIA_TYPE_MOVIE
                is TvShow -> MyListViewModel.MEDIA_TYPE_TV
            }
            val isTv = savedMovie.movie is TvShow
            val tmdbId = savedMovie.movie.tmdbId
            val watchedCount = watchedEpisodeCounts[tmdbId] ?: 0
            val totalEpisodes = (savedMovie.movie as? TvShow)?.numberOfEpisodes ?: 0
            SwipeToDismissItem(
                savedMovie = savedMovie,
                watchedEpisodes = if (isTv) watchedCount else null,
                totalEpisodes = if (isTv && totalEpisodes > 0) totalEpisodes else null,
                onDelete = { onDelete(savedMovie) },
                onClick = { onItemClick(mediaType, tmdbId) },
                onIncrementEpisode = if (isTv) {
                    { onIncrementEpisode(tmdbId) }
                } else {
                    null
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDismissItem(
    savedMovie: SavedMovie,
    watchedEpisodes: Int?,
    totalEpisodes: Int?,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    onIncrementEpisode: (() -> Unit)?,
) {
    var deleteTriggered by remember(savedMovie.movie.tmdbId) { mutableStateOf(false) }
    var itemWidthPx by remember(savedMovie.movie.tmdbId) { mutableFloatStateOf(1f) }
    val scope = rememberCoroutineScope()
    var dismissStateRef: androidx.compose.material3.SwipeToDismissBoxState? = null
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { targetValue ->
            val draggedFraction = (
                abs(
                    runCatching { dismissStateRef?.requireOffset() ?: 0f }
                        .getOrDefault(0f),
                ) / itemWidthPx
            ).coerceIn(0f, 1f)

            if (
                targetValue == SwipeToDismissBoxValue.EndToStart &&
                !deleteTriggered &&
                draggedFraction >= 0.80f
            ) {
                deleteTriggered = true
                onDelete()
                scope.launch {
                    dismissStateRef?.reset()
                    deleteTriggered = false
                }
            }
            false
        },
        positionalThreshold = { totalDistance -> totalDistance * 0.85f },
    )
    dismissStateRef = dismissState

    SwipeToDismissBox(
        state = dismissState,
        modifier = Modifier.onSizeChanged { size ->
            itemWidthPx = size.width.toFloat().coerceAtLeast(1f)
        },
        backgroundContent = {
            val progress = (
                abs(
                    runCatching { dismissState.requireOffset() }
                        .getOrDefault(0f),
                ) / itemWidthPx
            ).coerceIn(0f, 1f)
            val color by animateColorAsState(
                targetValue = if (progress > 0f) {
                    MaterialTheme.colorScheme.errorContainer
                } else {
                    MaterialTheme.colorScheme.surface
                },
                label = "swipe-bg",
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color.copy(alpha = 0.35f + (progress * 0.65f)))
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        },
        enableDismissFromStartToEnd = false,
    ) {
        MovieListItem(
            movie = savedMovie.movie,
            onClick = onClick,
            watchStatus = savedMovie.watchStatus,
            watchedEpisodes = watchedEpisodes,
            totalEpisodes = totalEpisodes,
            onIncrementEpisode = onIncrementEpisode,
            onDelete = onDelete,
        )
    }
}

@Composable
private fun EmptyState(
    hasStatusFilter: Boolean,
    hasMediaTypeFilter: Boolean,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (hasStatusFilter || hasMediaTypeFilter) {
                stringResource(R.string.mylist_empty_filtered)
            } else {
                stringResource(R.string.mylist_empty)
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
