package com.example.cinetracker.ui.screens.detail

import com.example.cinetracker.domain.model.Movie

/** Discrete UI states for the movie detail screen. */
sealed interface DetailUiState {
    data object Loading : DetailUiState
    data class Success(val movie: Movie) : DetailUiState
    data class Error(val message: String) : DetailUiState
}
