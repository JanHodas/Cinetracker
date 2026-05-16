package com.example.cinetracker.ui.screens.stats

import com.example.cinetracker.domain.model.WatchStatus

data class StatsUiState(
    val activeStatusFilter: WatchStatus? = null,
    val activeMediaTypeFilter: String? = null,
    val totalCount: Int = 0,
    val statusCounts: Map<WatchStatus, Int> = emptyMap(),
    val averageRating: Float? = null,
    val topGenres: List<Pair<String, Int>> = emptyList(),
)
