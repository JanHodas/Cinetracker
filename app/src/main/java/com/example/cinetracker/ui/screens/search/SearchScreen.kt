package com.example.cinetracker.ui.screens.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cinetracker.R
import com.example.cinetracker.domain.model.Movie
import com.example.cinetracker.domain.model.TvShow
import com.example.cinetracker.ui.components.MovieListItem

@Composable
fun SearchScreen(
    onItemClick: (mediaType: String, tmdbId: Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = viewModel(factory = SearchViewModel.Factory),
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val errorMessage = uiState.errorMessage

    Column(modifier = modifier.fillMaxSize()) {
        SearchField(
            query = query,
            onQueryChange = viewModel::onQueryChange,
            onClear = viewModel::clearQuery,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )

        when {
            uiState.isIdle -> CenteredMessage(text = stringResource(R.string.search_state_idle))
            uiState.isLoading && uiState.items.isEmpty() -> CenteredProgress()
            errorMessage != null && uiState.items.isEmpty() -> ErrorState(
                message = errorMessage,
                onRetry = viewModel::retry,
            )
            uiState.totalResults == 0 -> CenteredMessage(text = stringResource(R.string.search_state_empty))
            else -> SearchResultsList(
                state = uiState,
                onItemClick = onItemClick,
                onAddToList = viewModel::addToWantToWatch,
                onPreviousPage = viewModel::goToPreviousPage,
                onNextPage = viewModel::goToNextPage,
            )
        }
    }
}

@Composable
private fun SearchResultsList(
    state: SearchUiState,
    onItemClick: (mediaType: String, tmdbId: Int) -> Unit,
    onAddToList: (com.example.cinetracker.domain.model.MediaItem) -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
        ) {
            items(
                items = state.items,
                key = { item ->
                    when (item) {
                        is Movie -> "movie-${item.tmdbId}"
                        is TvShow -> "tv-${item.tmdbId}"
                    }
                },
            ) { item ->
                val mediaType = when (item) {
                    is Movie -> "movie"
                    is TvShow -> "tv"
                }
                MovieListItem(
                    movie = item,
                    onClick = { onItemClick(mediaType, item.tmdbId) },
                    onAddToList = { onAddToList(item) },
                    isInList = item.tmdbId in state.savedTmdbIds,
                )
            }
        }

        if (state.totalPages > 1) {
            SearchPaginationBar(
                currentPage = state.currentPage,
                totalPages = state.totalPages,
                isLoadingMore = state.isLoadingMore,
                onPreviousPage = onPreviousPage,
                onNextPage = onNextPage,
            )
        }
    }
}

@Composable
private fun SearchPaginationBar(
    currentPage: Int,
    totalPages: Int,
    isLoadingMore: Boolean,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        OutlinedButton(
            onClick = onPreviousPage,
            enabled = currentPage > 1 && !isLoadingMore,
        ) {
            Text("<<")
        }
        if (isLoadingMore) {
            CircularProgressIndicator()
        } else {
            Text(
                text = "$currentPage / $totalPages",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Button(
            onClick = onNextPage,
            enabled = currentPage < totalPages && !isLoadingMore,
        ) {
            Text(">>")
        }
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
        singleLine = true,
        label = { Text(stringResource(R.string.search_field_label)) },
        placeholder = { Text(stringResource(R.string.search_field_placeholder)) },
        leadingIcon = {
            Icon(imageVector = Icons.Filled.Search, contentDescription = null)
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = onClear) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.search_clear_action),
                    )
                }
            }
        },
    )
}

@Composable
private fun CenteredMessage(text: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CenteredProgress() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.search_state_error),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.error,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text(stringResource(R.string.common_retry))
        }
    }
}
