package com.example.cinetracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.cinetracker.R
import com.example.cinetracker.domain.model.MediaItem
import com.example.cinetracker.domain.model.TvShow
import com.example.cinetracker.domain.model.WatchStatus

/**
 * Compact list row representing a single media item (movie or TV show).
 *
 * TV shows can optionally display a MAL-style episode progress bar instead of
 * the overview snippet when [watchedEpisodes] and [totalEpisodes] are provided.
 */
@Composable
fun MovieListItem(
    movie: MediaItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    watchStatus: WatchStatus? = null,
    watchedEpisodes: Int? = null,
    totalEpisodes: Int? = null,
    onIncrementEpisode: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
) {
    val showEpisodeProgress = movie is TvShow && totalEpisodes != null && totalEpisodes > 0

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AsyncMoviePoster(
                posterPath = movie.posterPath,
                contentDescription = movie.title,
                modifier = Modifier
                    .width(60.dp)
                    .height(90.dp),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = movie.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val year = movie.releaseDate?.take(4)?.takeIf { it.length == 4 }
                    if (year != null) {
                        Text(
                            text = year,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    MediaTypeBadge(
                        label = stringResource(
                            if (movie is TvShow) R.string.badge_tv else R.string.badge_movie,
                        ),
                    )
                    watchStatus?.let { status ->
                        WatchStatusBadge(status = status)
                    }
                }

                if (showEpisodeProgress) {
                    EpisodeProgressSection(
                        watchedEpisodes = watchedEpisodes ?: 0,
                        totalEpisodes = totalEpisodes,
                    )
                } else if (movie.overview.isNotBlank()) {
                    Text(
                        text = movie.overview,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            if (onIncrementEpisode != null || onDelete != null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    if (onIncrementEpisode != null) {
                        IconButton(
                            onClick = onIncrementEpisode,
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = stringResource(R.string.mylist_increment_episode),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                    if (onDelete != null) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = stringResource(R.string.mylist_deleted_snackbar),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
    }
}

@Composable
private fun EpisodeProgressSection(
    watchedEpisodes: Int,
    totalEpisodes: Int,
) {
    val progress = if (totalEpisodes > 0) {
        watchedEpisodes.toFloat() / totalEpisodes.toFloat()
    } else {
        0f
    }.coerceIn(0f, 1f)

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(999.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
        Text(
            text = stringResource(
                R.string.mylist_episode_progress,
                watchedEpisodes,
                totalEpisodes,
            ),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** Small rounded label used to distinguish TV shows from movies in mixed lists. */
@Composable
private fun MediaTypeBadge(label: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

@Composable
private fun WatchStatusBadge(status: WatchStatus) {
    val (label, backgroundColor, contentColor) = when (status) {
        WatchStatus.WANT_TO_WATCH -> Triple(
            stringResource(R.string.detail_status_want),
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
        )
        WatchStatus.WATCHING -> Triple(
            stringResource(R.string.detail_status_watching),
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
        )
        WatchStatus.WATCHED -> Triple(
            stringResource(R.string.detail_status_watched),
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
        )
    }
}
