package com.example.cinetracker.ui.screens.mylist

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cinetracker.R
import com.example.cinetracker.domain.model.Movie
import com.example.cinetracker.domain.model.SavedMovie
import com.example.cinetracker.domain.model.TvShow
import com.example.cinetracker.domain.model.WatchStatus
import com.example.cinetracker.ui.components.MovieListItem
import com.example.cinetracker.ui.language.AppLanguage
import com.example.cinetracker.ui.language.LanguageManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyListScreen(
    onItemClick: (mediaType: String, tmdbId: Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MyListViewModel = viewModel(factory = MyListViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val reorderEnabled = uiState.activeStatusFilter == null && uiState.activeMediaTypeFilter == null
    val context = LocalContext.current
    val currentLanguage = LanguageManager.currentLanguage(context)
    var languageMenuExpanded by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val deletedMessage = stringResource(R.string.mylist_deleted_snackbar)
    val undoLabel = stringResource(R.string.mylist_undo)

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
            ) {
                Text(
                    text = stringResource(R.string.mylist_title),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                    TextButton(onClick = { languageMenuExpanded = true }) {
                        Text(
                            text = currentLanguage.shortLabel,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    DropdownMenu(
                        expanded = languageMenuExpanded,
                        onDismissRequest = { languageMenuExpanded = false },
                    ) {
                        AppLanguage.entries.forEach { language ->
                            DropdownMenuItem(
                                text = { Text(stringResource(language.labelRes)) },
                                onClick = {
                                    languageMenuExpanded = false
                                    LanguageManager.updateLanguage(context, language)
                                },
                            )
                        }
                    }
                }
            }
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
                    reorderEnabled = reorderEnabled,
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
                    onToggleMovieWatched = viewModel::toggleMovieWatched,
                    onIncrementEpisode = viewModel::incrementEpisode,
                    onReorderItems = viewModel::persistReorder,
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
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        filters.forEach { (status, label) ->
            FilterChip(
                selected = activeFilter == status,
                onClick = { onFilterSelected(status) },
                label = { Text(text = label, maxLines = 1) },
                modifier = Modifier.wrapContentWidth(),
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
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 0.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        filters.forEach { (mediaType, label) ->
            FilterChip(
                selected = activeFilter == mediaType,
                onClick = { onFilterSelected(mediaType) },
                label = { Text(text = label, maxLines = 1) },
                modifier = Modifier.wrapContentWidth(),
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
    reorderEnabled: Boolean,
    onItemClick: (mediaType: String, tmdbId: Int) -> Unit,
    onDelete: (SavedMovie) -> Unit,
    onToggleMovieWatched: (SavedMovie) -> Unit,
    onIncrementEpisode: (tmdbId: Int) -> Unit,
    onReorderItems: (List<Int>) -> Unit,
) {
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val visibleItems = remember { mutableStateListOf<SavedMovie>() }
    var dragInProgress by remember { mutableStateOf(false) }
    var pendingReorderTmdbIds by remember { mutableStateOf<List<Int>?>(null) }

    LaunchedEffect(items, dragInProgress, pendingReorderTmdbIds) {
        val latestItemIds = items.map { it.movie.tmdbId }
        when {
            dragInProgress && pendingReorderTmdbIds != null -> {
                if (latestItemIds == pendingReorderTmdbIds) {
                    visibleItems.clear()
                    visibleItems.addAll(items)
                    pendingReorderTmdbIds = null
                    dragInProgress = false
                }
            }
            !dragInProgress -> {
                pendingReorderTmdbIds = null
                visibleItems.clear()
                visibleItems.addAll(items)
            }
        }
    }

    val dragDropState = remember(listState) {
        MyListDragDropState(
            listState = listState,
            onMove = { fromIndex, toIndex ->
                visibleItems.add(toIndex, visibleItems.removeAt(fromIndex))
            },
        )
    }

    LaunchedEffect(dragDropState.draggedIndex) {
        while (dragDropState.draggedIndex != null) {
            val scrollDelta = dragDropState.edgeScrollDelta(density)
            if (scrollDelta != 0f) {
                listState.scrollBy(scrollDelta)
                dragDropState.onViewportScrolled()
            }
            delay(16)
        }
    }

    val draggedItem = dragDropState.draggedTmdbId?.let { tmdbId ->
        visibleItems.firstOrNull { it.movie.tmdbId == tmdbId }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = !dragInProgress,
            contentPadding = PaddingValues(bottom = 16.dp, top = 8.dp),
        ) {
            itemsIndexed(
                items = visibleItems,
                key = { _, savedMovie ->
                    "${savedMovie.movie.tmdbId}-${itemRenderVersions[savedMovie.movie.tmdbId] ?: 0}"
                },
            ) { index, savedMovie ->
                val isDragged = dragDropState.draggedTmdbId == savedMovie.movie.tmdbId
                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            alpha = if (isDragged) 0f else 1f
                        }
                        .pointerInput(reorderEnabled, savedMovie.movie.tmdbId) {
                            if (!reorderEnabled) return@pointerInput
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    val currentIndex = visibleItems.indexOfFirst {
                                        it.movie.tmdbId == savedMovie.movie.tmdbId
                                    }
                                    if (currentIndex == -1) return@detectDragGesturesAfterLongPress
                                    dragInProgress = true
                                    dragDropState.onDragStart(currentIndex, savedMovie.movie.tmdbId)
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    dragDropState.onDrag(dragAmount.y)
                                },
                                onDragEnd = {
                                    dragDropState.onDragInterrupted()
                                    val reorderedIds = visibleItems.map { it.movie.tmdbId }
                                    if (reorderedIds == items.map { it.movie.tmdbId }) {
                                        dragInProgress = false
                                        pendingReorderTmdbIds = null
                                    } else {
                                        pendingReorderTmdbIds = reorderedIds
                                        onReorderItems(reorderedIds)
                                    }
                                },
                                onDragCancel = {
                                    dragDropState.onDragInterrupted()
                                    dragInProgress = false
                                    pendingReorderTmdbIds = null
                                },
                            )
                        },
                ) {
                    MovieListRow(
                        savedMovie = savedMovie,
                        watchedEpisodeCounts = watchedEpisodeCounts,
                        onItemClick = onItemClick,
                        onDelete = onDelete,
                        onToggleMovieWatched = onToggleMovieWatched,
                        onIncrementEpisode = onIncrementEpisode,
                    )
                }
            }
        }

        if (draggedItem != null) {
            Box(
                modifier = Modifier
                    .offset { IntOffset(0, dragDropState.overlayTop.roundToInt()) }
                    .zIndex(1f),
            ) {
                MovieListRow(
                    savedMovie = draggedItem,
                    watchedEpisodeCounts = watchedEpisodeCounts,
                    onItemClick = onItemClick,
                    onDelete = onDelete,
                    onToggleMovieWatched = onToggleMovieWatched,
                    onIncrementEpisode = onIncrementEpisode,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MovieListRow(
    savedMovie: SavedMovie,
    watchedEpisodeCounts: Map<Int, Int>,
    onItemClick: (mediaType: String, tmdbId: Int) -> Unit,
    onDelete: (SavedMovie) -> Unit,
    onToggleMovieWatched: (SavedMovie) -> Unit,
    onIncrementEpisode: (tmdbId: Int) -> Unit,
) {
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
        onToggleWatched = if (!isTv) {
            { onToggleMovieWatched(savedMovie) }
        } else {
            null
        },
        isWatched = savedMovie.watchStatus == WatchStatus.WATCHED,
        onIncrementEpisode = if (isTv) {
            { onIncrementEpisode(tmdbId) }
        } else {
            null
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDismissItem(
    savedMovie: SavedMovie,
    watchedEpisodes: Int?,
    totalEpisodes: Int?,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    onToggleWatched: (() -> Unit)?,
    isWatched: Boolean,
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
            userRating = savedMovie.userRating,
            watchedEpisodes = watchedEpisodes,
            totalEpisodes = totalEpisodes,
            onToggleWatched = onToggleWatched,
            isWatched = isWatched,
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

private class MyListDragDropState(
    private val listState: LazyListState,
    private val onMove: (fromIndex: Int, toIndex: Int) -> Unit,
) {
    var draggedIndex by mutableStateOf<Int?>(null)
        private set
    var draggedTmdbId by mutableStateOf<Int?>(null)
        private set
    var overlayTop by mutableFloatStateOf(0f)
        private set

    private var overlayHeight by mutableFloatStateOf(0f)
    private var overlayCenter by mutableFloatStateOf(0f)

    fun onDragStart(index: Int, tmdbId: Int) {
        val itemInfo = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index } ?: return
        draggedIndex = index
        draggedTmdbId = tmdbId
        overlayTop = itemInfo.offset.toFloat()
        overlayHeight = itemInfo.size.toFloat()
        overlayCenter = overlayTop + (overlayHeight / 2f)
    }

    fun onDrag(offsetY: Float) {
        if (draggedIndex == null) return
        overlayTop += offsetY
        overlayCenter += offsetY
        updateDraggedIndex()
    }

    fun edgeScrollDelta(density: androidx.compose.ui.unit.Density): Float {
        if (draggedIndex == null) return 0f
        val viewportStart = listState.layoutInfo.viewportStartOffset.toFloat()
        val viewportEnd = listState.layoutInfo.viewportEndOffset.toFloat()
        val threshold = with(density) { 72.dp.toPx() }
        val overlayBottom = overlayTop + overlayHeight

        return when {
            overlayBottom > viewportEnd - threshold -> {
                ((overlayBottom - (viewportEnd - threshold)) / 6f).coerceAtMost(18f)
            }
            overlayTop < viewportStart + threshold -> {
                ((overlayTop - (viewportStart + threshold)) / 6f).coerceAtLeast(-18f)
            }
            else -> 0f
        }
    }

    fun onViewportScrolled() {
        updateDraggedIndex()
    }

    fun onDragInterrupted() {
        draggedIndex = null
        draggedTmdbId = null
        overlayTop = 0f
        overlayHeight = 0f
        overlayCenter = 0f
    }

    private fun updateDraggedIndex() {
        val currentIndex = draggedIndex ?: return

        val previousItem = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == currentIndex - 1 }
        if (previousItem != null && overlayCenter < previousItem.offset + (previousItem.size / 2f)) {
            onMove(currentIndex, previousItem.index)
            draggedIndex = previousItem.index
            return
        }

        val nextItem = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == currentIndex + 1 }
        if (nextItem != null && overlayCenter > nextItem.offset + (nextItem.size / 2f)) {
            onMove(currentIndex, nextItem.index)
            draggedIndex = nextItem.index
        }
    }
}
