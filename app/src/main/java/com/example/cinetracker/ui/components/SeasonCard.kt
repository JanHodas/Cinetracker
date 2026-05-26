package com.example.cinetracker.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.cinetracker.R
import com.example.cinetracker.domain.model.Season
import com.example.cinetracker.domain.util.TmdbImageUrl

/**
 * Expandable card displaying a single TV season with episode-level watch tracking.
 *
 * The header shows the season poster, name, episode count and a watched progress
 * indicator (e.g. "3/10"). When expanded, each episode row has a toggle icon
 * to mark/unmark it as watched.
 *
 * @param watchedEpisodes set of episode numbers within this season that are watched.
 * @param onToggleEpisode called with the episode number when the user taps the toggle.
 */
@Composable
fun SeasonCard(
    season: Season,
    watchedEpisodes: Set<Int> = emptySet(),
    onToggleEpisode: (episodeNumber: Int) -> Unit = {},
    onToggleSeasonWatched: (() -> Unit)? = null,
    canRateSeason: Boolean = false,
    onUpdateSeasonRating: ((Float?) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable(season.seasonNumber) { mutableStateOf(false) }
    val watchedCount = watchedEpisodes.size
    var seasonRating by rememberSaveable(season.seasonNumber) {
        mutableFloatStateOf(season.userRating ?: 5f)
    }

    LaunchedEffect(season.userRating) {
        seasonRating = season.userRating ?: 5f
    }

    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            // ── Season header ──────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AsyncMoviePoster(
                    posterPath = season.posterPath,
                    contentDescription = season.name,
                    size = TmdbImageUrl.POSTER_W342,
                    modifier = Modifier
                        .width(72.dp)
                        .aspectRatio(2f / 3f),
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = season.name,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(R.string.detail_episode_count, season.episodeCount),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    season.tmdbRating?.let { rating ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.tertiary,
                            )
                            Text(
                                text = stringResource(R.string.detail_rating_format, rating),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    if (watchedCount > 0 || season.episodes.isNotEmpty()) {
                        Text(
                            text = stringResource(
                                R.string.detail_episode_progress,
                                watchedCount,
                                season.episodeCount,
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    if (season.overview.isNotBlank()) {
                        Text(
                            text = season.overview,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (canRateSeason && season.userRating != null) {
                        Text(
                            text = stringResource(R.string.detail_season_rating_value, season.userRating),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                if (onToggleSeasonWatched != null) {
                    val allWatched = watchedCount >= season.episodeCount && season.episodeCount > 0
                    IconButton(
                        onClick = onToggleSeasonWatched,
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            imageVector = if (allWatched) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                            contentDescription = stringResource(
                                if (allWatched) {
                                    R.string.detail_unmark_season_watched
                                } else {
                                    R.string.detail_mark_season_watched
                                },
                            ),
                            tint = if (allWatched) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
            }

            // ── Episode list ───────────────────────────────────────
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (season.episodes.isEmpty()) {
                        Text(
                            text = stringResource(R.string.detail_no_episodes),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        if (canRateSeason && onUpdateSeasonRating != null) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(
                                    text = stringResource(R.string.detail_season_rating_title),
                                    style = MaterialTheme.typography.titleSmall,
                                )
                                Text(
                                    text = stringResource(R.string.detail_season_rating_value, seasonRating),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Slider(
                                    value = seasonRating,
                                    onValueChange = { seasonRating = it },
                                    onValueChangeFinished = { onUpdateSeasonRating(seasonRating) },
                                    valueRange = 1f..10f,
                                    steps = 8,
                                )
                            }
                        }
                        season.episodes.forEach { episode ->
                            val isWatched = episode.episodeNumber in watchedEpisodes

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Text(
                                        text = stringResource(
                                            R.string.detail_episode_label,
                                            episode.episodeNumber,
                                            episode.name,
                                        ),
                                        style = MaterialTheme.typography.titleSmall,
                                        modifier = Modifier.weight(1f),
                                    )
                                    IconButton(
                                        onClick = { onToggleEpisode(episode.episodeNumber) },
                                        modifier = Modifier.size(36.dp),
                                    ) {
                                        Icon(
                                            imageVector = if (isWatched) {
                                                Icons.Filled.CheckCircle
                                            } else {
                                                Icons.Outlined.Circle
                                            },
                                            contentDescription = stringResource(
                                                R.string.detail_toggle_episode,
                                            ),
                                            tint = if (isWatched) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                            modifier = Modifier.size(20.dp),
                                        )
                                    }
                                }

                                Column(modifier = Modifier.fillMaxWidth()) {
                                    val metadata = buildList {
                                        episode.airDate?.let(::add)
                                        episode.runtime?.let {
                                            add(stringResource(R.string.detail_runtime_minutes, it))
                                        }
                                    }.joinToString(" | ")
                                    if (metadata.isNotBlank()) {
                                        Text(
                                            text = metadata,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    if (episode.overview.isNotBlank()) {
                                        Text(
                                            text = episode.overview,
                                            style = MaterialTheme.typography.bodyMedium,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
