package com.example.cinetracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.example.cinetracker.R
import com.example.cinetracker.domain.util.TmdbImageUrl

/**
 * Coil-backed poster image with a movie-icon fallback for missing or failed loads.
 *
 * @param posterPath relative TMDB path (e.g. "/abc.jpg"); resolved via [TmdbImageUrl].
 * @param size the TMDB size segment to request — bigger sizes use more bandwidth.
 * @param cornerRadius rounded-corner radius applied uniformly.
 */
@Composable
fun AsyncMoviePoster(
    posterPath: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: String = TmdbImageUrl.POSTER_W342,
    cornerRadius: Dp = 8.dp,
) {
    val url = TmdbImageUrl.build(posterPath, size)
    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (url != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalPlatformContext.current)
                    .data(url)
                    .crossfade(true)
                    .build(),
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                imageVector = Icons.Filled.Movie,
                contentDescription = stringResource(R.string.poster_placeholder_description),
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
