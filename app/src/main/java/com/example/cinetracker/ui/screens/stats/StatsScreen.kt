package com.example.cinetracker.ui.screens.stats

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cinetracker.R
import com.example.cinetracker.domain.model.WatchStatus
import com.example.cinetracker.ui.language.AppLanguage
import com.example.cinetracker.ui.language.LanguageManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    modifier: Modifier = Modifier,
    viewModel: StatsViewModel = viewModel(factory = StatsViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val currentLanguage = LanguageManager.currentLanguage(context)
    var languageMenuExpanded by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val importSuccessMsg = stringResource(R.string.stats_import_success, 0)
    val importErrorMsg = stringResource(R.string.stats_import_error)

    LaunchedEffect(Unit) {
        viewModel.backupEvents.collect { event ->
            when (event) {
                is BackupEvent.ImportSuccess -> snackbarHostState.showSnackbar(
                    context.getString(R.string.stats_import_success, event.count),
                )
                is BackupEvent.ImportError -> snackbarHostState.showSnackbar(importErrorMsg)
            }
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        viewModel.exportData { json ->
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(json.toByteArray(Charsets.UTF_8))
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            val json = context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.bufferedReader().readText()
            } ?: return@launch
            viewModel.importData(json)
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 4.dp),
            ) {
                Text(
                    text = stringResource(R.string.stats_title),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.align(Alignment.CenterStart),
                )
                Row(
                    modifier = Modifier.align(Alignment.CenterEnd),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = { exportLauncher.launch("cinetrack_export.json") }) {
                        Text(stringResource(R.string.stats_export))
                    }
                    TextButton(onClick = { importLauncher.launch(arrayOf("application/json")) }) {
                        Text(stringResource(R.string.stats_import))
                    }
                    Box {
                        TextButton(
                            onClick = { languageMenuExpanded = true },
                        ) {
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

            if (uiState.totalCount == 0 && uiState.activeStatusFilter == null && uiState.activeMediaTypeFilter == null) {
                EmptyStats()
            } else {
                StatsContent(state = uiState)
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
        StatsViewModel.MEDIA_TYPE_MOVIE to stringResource(R.string.mylist_media_movies),
        StatsViewModel.MEDIA_TYPE_TV to stringResource(R.string.mylist_media_tv),
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

@Composable
private fun StatsContent(
    state: StatsUiState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (state.totalCount == 0) {
            Text(
                text = stringResource(R.string.mylist_empty_filtered),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
            return
        }

        StatsCard {
            StatRow(
                label = stringResource(R.string.stats_total_items),
                value = state.totalCount.toString(),
                highlight = true,
            )

            if (state.activeStatusFilter == null) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                StatRow(
                    label = stringResource(R.string.stats_want_to_watch),
                    value = (state.statusCounts[WatchStatus.WANT_TO_WATCH] ?: 0).toString(),
                )
                StatRow(
                    label = stringResource(R.string.stats_watching),
                    value = (state.statusCounts[WatchStatus.WATCHING] ?: 0).toString(),
                )
                StatRow(
                    label = stringResource(R.string.stats_watched),
                    value = (state.statusCounts[WatchStatus.WATCHED] ?: 0).toString(),
                )
            }
        }

        StatsCard {
            val hours = state.totalRuntimeMinutes / 60
            val minutes = state.totalRuntimeMinutes % 60
            StatRow(
                label = stringResource(R.string.stats_total_runtime),
                value = stringResource(R.string.stats_runtime_hours, hours, minutes),
                highlight = true,
            )
        }

        StatsCard {
            Text(
                text = stringResource(R.string.stats_average_rating),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (state.averageRating != null) {
                Text(
                    text = stringResource(R.string.stats_average_rating_value, state.averageRating),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
            } else {
                Text(
                    text = stringResource(R.string.stats_no_ratings),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        StatsCard {
            Text(
                text = stringResource(R.string.stats_top_genres),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (state.topGenres.isEmpty()) {
                Text(
                    text = stringResource(R.string.stats_no_genres),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                state.topGenres.take(5).forEach { (genre, count) ->
                    StatRow(
                        label = genre,
                        value = stringResource(R.string.stats_genre_count, count),
                    )
                }
            }
        }
    }
}

@Composable
private fun StatsCard(
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

@Composable
private fun StatRow(
    label: String,
    value: String,
    highlight: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = if (highlight) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = value,
            style = if (highlight) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge,
            fontWeight = if (highlight) FontWeight.Bold else FontWeight.Normal,
            color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun EmptyStats(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.stats_empty),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
