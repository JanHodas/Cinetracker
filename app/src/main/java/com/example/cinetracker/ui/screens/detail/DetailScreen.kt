package com.example.cinetracker.ui.screens.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BookmarkRemove
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cinetracker.R
import com.example.cinetracker.domain.model.MediaItem
import com.example.cinetracker.domain.model.SavedMovie
import com.example.cinetracker.domain.model.Season
import com.example.cinetracker.domain.model.TvShow
import com.example.cinetracker.domain.model.WatchStatus
import com.example.cinetracker.domain.util.TmdbImageUrl
import com.example.cinetracker.ui.components.AsyncMoviePoster
import com.example.cinetracker.ui.components.SeasonCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DetailViewModel = viewModel(factory = DetailViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val savedState by viewModel.savedState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val savedMessage = stringResource(R.string.detail_saved_snackbar)
    val removedMessage = stringResource(R.string.detail_removed_snackbar)

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                DetailEvent.MovieSaved -> snackbarHostState.showSnackbar(savedMessage)
                DetailEvent.MovieRemoved -> snackbarHostState.showSnackbar(removedMessage)
            }
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.detail_back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when (val state = uiState) {
                is DetailUiState.Loading -> CenteredProgress()
                is DetailUiState.Error -> ErrorState(
                    message = state.message,
                    onRetry = viewModel::retry,
                )
                is DetailUiState.Success -> SuccessContent(
                    mediaItem = state.mediaItem,
                    seasons = state.seasons,
                    savedMovie = savedState,
                    onSaveToList = viewModel::saveToList,
                    onUpdateStatus = viewModel::updateStatus,
                    onUpdateRating = viewModel::updateRating,
                    onUpdateNote = viewModel::updateNote,
                    onRemoveFromList = viewModel::removeFromList,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SuccessContent(
    mediaItem: MediaItem,
    seasons: List<Season>,
    savedMovie: SavedMovie?,
    onSaveToList: (WatchStatus) -> Unit,
    onUpdateStatus: (WatchStatus) -> Unit,
    onUpdateRating: (Float?) -> Unit,
    onUpdateNote: (String) -> Unit,
    onRemoveFromList: () -> Unit,
) {
    val scrollState = rememberScrollState()
    val isSaved = savedMovie != null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
    ) {
        BackdropHeader(mediaItem = mediaItem)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = mediaItem.title,
                style = MaterialTheme.typography.headlineSmall,
            )

            MetadataRow(mediaItem = mediaItem)

            if (mediaItem.genres.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.detail_genres_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    mediaItem.genres.forEach { genre ->
                        AssistChip(onClick = { }, label = { Text(genre) })
                    }
                }
            }

            Text(
                text = stringResource(R.string.detail_overview_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = mediaItem.overview.ifBlank { stringResource(R.string.detail_no_overview) },
                style = MaterialTheme.typography.bodyMedium,
            )

            if (mediaItem is TvShow) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Text(
                    text = stringResource(R.string.detail_seasons_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                if (seasons.isEmpty()) {
                    Text(
                        text = stringResource(R.string.detail_no_seasons),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        seasons.forEach { season ->
                            SeasonCard(season = season)
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            Text(
                text = stringResource(R.string.detail_status_title),
                style = MaterialTheme.typography.titleMedium,
            )

            WatchStatusSelector(
                currentStatus = savedMovie?.watchStatus,
                onStatusSelected = { status ->
                    if (isSaved) onUpdateStatus(status) else onSaveToList(status)
                },
            )

            AnimatedVisibility(visible = savedMovie?.watchStatus == WatchStatus.WATCHED) {
                RatingSection(
                    currentRating = savedMovie?.userRating,
                    onRatingChanged = onUpdateRating,
                )
            }

            AnimatedVisibility(visible = isSaved) {
                NoteSection(
                    currentNote = savedMovie?.note.orEmpty(),
                    onNoteChanged = onUpdateNote,
                )
            }

            if (isSaved) {
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedButton(
                    onClick = onRemoveFromList,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = Icons.Filled.BookmarkRemove,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(stringResource(R.string.detail_remove_from_list))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WatchStatusSelector(
    currentStatus: WatchStatus?,
    onStatusSelected: (WatchStatus) -> Unit,
) {
    val options = listOf(
        WatchStatus.WANT_TO_WATCH to stringResource(R.string.detail_status_want),
        WatchStatus.WATCHING to stringResource(R.string.detail_status_watching),
        WatchStatus.WATCHED to stringResource(R.string.detail_status_watched),
    )

    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (status, label) ->
            SegmentedButton(
                selected = currentStatus == status,
                onClick = { onStatusSelected(status) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
            ) {
                Text(label, maxLines = 1)
            }
        }
    }
}

@Composable
private fun RatingSection(
    currentRating: Float?,
    onRatingChanged: (Float?) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(R.string.detail_your_rating),
            style = MaterialTheme.typography.titleMedium,
        )
        var sliderValue by rememberSaveable { mutableFloatStateOf(currentRating ?: 5f) }

        LaunchedEffect(currentRating) {
            if (currentRating != null) sliderValue = currentRating
        }

        Text(
            text = stringResource(R.string.detail_rating_value, sliderValue),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Slider(
            value = sliderValue,
            onValueChange = { sliderValue = it },
            onValueChangeFinished = { onRatingChanged(sliderValue) },
            valueRange = 1f..10f,
            steps = 8,
        )
    }
}

@Composable
private fun NoteSection(
    currentNote: String,
    onNoteChanged: (String) -> Unit,
) {
    var draft by rememberSaveable(currentNote) { mutableStateOf(currentNote) }

    OutlinedTextField(
        value = draft,
        onValueChange = { newValue ->
            draft = newValue
            onNoteChanged(newValue)
        },
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.detail_note_label)) },
        placeholder = { Text(stringResource(R.string.detail_note_placeholder)) },
        minLines = 2,
        maxLines = 4,
    )
}

@Composable
private fun BackdropHeader(mediaItem: MediaItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        AsyncMoviePoster(
            posterPath = mediaItem.posterPath,
            contentDescription = mediaItem.title,
            size = TmdbImageUrl.POSTER_W500,
            modifier = Modifier
                .width(140.dp)
                .aspectRatio(2f / 3f),
        )
    }
}

@Composable
private fun MetadataRow(mediaItem: MediaItem) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val year = mediaItem.releaseDate?.take(4)?.takeIf { it.length == 4 }
        if (year != null) {
            Text(
                text = year,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        mediaItem.tmdbRating?.let { rating ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.tertiary,
                )
                Spacer(modifier = Modifier.size(4.dp))
                Text(
                    text = stringResource(R.string.detail_rating_format, rating),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        if (mediaItem is TvShow) {
            Text(
                text = stringResource(R.string.detail_season_count, mediaItem.numberOfSeasons),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.detail_episode_count, mediaItem.numberOfEpisodes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
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
            text = stringResource(R.string.detail_state_error),
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
