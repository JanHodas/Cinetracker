package com.example.cinetracker.ui.screens.detail

import com.example.cinetracker.domain.model.CastMember
import com.example.cinetracker.domain.model.MediaItem
import com.example.cinetracker.domain.model.Season

/** Discrete UI states for the media detail screen. */
sealed interface DetailUiState {
    data object Loading : DetailUiState
    data class Success(
        val mediaItem: MediaItem,
        val cast: List<CastMember> = emptyList(),
        val seasons: List<Season> = emptyList(),
        /** Set of (seasonNumber, episodeNumber) pairs that the user has watched. */
        val watchedEpisodes: Set<Pair<Int, Int>> = emptySet(),
        /** Per-season user ratings keyed by season number. */
        val seasonRatings: Map<Int, Float?> = emptyMap(),
    ) : DetailUiState
    data class Error(val message: String) : DetailUiState
}
