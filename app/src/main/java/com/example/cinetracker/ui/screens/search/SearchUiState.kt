package com.example.cinetracker.ui.screens.search

import com.example.cinetracker.domain.model.MediaItem

data class SearchUiState(
    val isIdle: Boolean = true,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val items: List<MediaItem> = emptyList(),
    val currentPage: Int = 1,
    val totalPages: Int = 0,
    val totalResults: Int = 0,
    val savedTmdbIds: Set<Int> = emptySet(),
    val errorMessage: String? = null,
)
