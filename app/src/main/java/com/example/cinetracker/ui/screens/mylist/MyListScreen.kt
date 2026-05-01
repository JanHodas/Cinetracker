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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
                    onItemClick = onItemClick,
                    onDelete = { saved ->
                        viewModel.deleteMovie(saved)
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = deletedMessage,
                                duration = SnackbarDuration.Short,
                            )
                        }
                    },
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
    onItemClick: (mediaType: String, tmdbId: Int) -> Unit,
    onDelete: (SavedMovie) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp, top = 8.dp),
    ) {
        items(
            items = items,
            key = { it.movie.tmdbId },
        ) { savedMovie ->
            val mediaType = when (savedMovie.movie) {
                is Movie -> MyListViewModel.MEDIA_TYPE_MOVIE
                is TvShow -> MyListViewModel.MEDIA_TYPE_TV
            }
            SwipeToDismissItem(
                savedMovie = savedMovie,
                onDelete = { onDelete(savedMovie) },
                onClick = { onItemClick(mediaType, savedMovie.movie.tmdbId) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDismissItem(
    savedMovie: SavedMovie,
    onDelete: () -> Unit,
    onClick: () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState()

    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
            onDelete()
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val color by animateColorAsState(
                targetValue = if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) {
                    MaterialTheme.colorScheme.errorContainer
                } else {
                    MaterialTheme.colorScheme.surface
                },
                label = "swipe-bg",
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color)
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
