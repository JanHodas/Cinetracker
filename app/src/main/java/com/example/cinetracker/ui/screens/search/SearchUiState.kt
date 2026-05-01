package com.example.cinetracker.ui.screens.search

import com.example.cinetracker.domain.model.MediaItem

/**
 * Discrete UI states for the search screen. Modeled as a sealed interface so the
 * Composable's `when` is exhaustively checked at compile time.
 */
sealed interface SearchUiState {
    /** Initial state — query is empty or shorter than the minimum search length. */
    data object Idle : SearchUiState

    /** Network call in flight. */
    data object Loading : SearchUiState

    /** Query returned at least one match (movies and/or TV shows). */
    data class Success(val results: List<MediaItem>) : SearchUiState

    /** Query was valid but TMDB returned zero results. */
    data object Empty : SearchUiState

    /** A failure occurred (network, parsing, auth). */
    data class Error(val message: String) : SearchUiState
}
