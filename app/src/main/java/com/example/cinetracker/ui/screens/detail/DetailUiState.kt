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
    ) : DetailUiState
    data class Error(val message: String) : DetailUiState
}
