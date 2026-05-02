package com.example.cinetracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.cinetracker.R
import com.example.cinetracker.domain.model.MediaItem
import com.example.cinetracker.domain.model.TvShow

/**
 * Compact list row representing a single media item (movie or TV show).
 *
 * Layout: poster on the left (60×90 dp), then title / badge / year / overview
 * snippet on the right. TV shows display a small badge next to the year.
 *
 * When [episodeProgress] is provided (e.g. "5/24"), it is shown as a small
 * progress label. When [onIncrementEpisode] is non-null, a "+" button appears
 * at the trailing edge for MAL-style quick episode tracking.
 */
@Composable
fun MovieListItem(
    movie: MediaItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    episodeProgress: String? = null,
    onIncrementEpisode: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
) {
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
                verticalArrangement = Arrangement.spacedBy(4.dp),
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
                    if (movie is TvShow) {
                        MediaTypeBadge(label = stringResource(R.string.badge_tv))
                    }
                    if (episodeProgress != null) {
                        EpisodeProgressBadge(progress = episodeProgress)
                    }
                }
                if (movie.overview.isNotBlank()) {
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

/** Small badge showing episode watch progress, e.g. "5/24 ep". */
@Composable
private fun EpisodeProgressBadge(progress: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = progress,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}
