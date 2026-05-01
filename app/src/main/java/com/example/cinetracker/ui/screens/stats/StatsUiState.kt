package com.example.cinetracker.ui.screens.stats

import com.example.cinetracker.domain.model.WatchStatus

/**
 * UI state for the Stats screen. Always populated (never loading/error)
 * because all data comes from local Room queries with sensible defaults.
 */
data class StatsUiState(
    val totalCount: Int = 0,
    val movieStats: MediaStatsSection = MediaStatsSection(),
    val tvStats: MediaStatsSection = MediaStatsSection(),
)

data class MediaStatsSection(
    val totalCount: Int = 0,
    val statusCounts: Map<WatchStatus, Int> = emptyMap(),
    val averageRating: Float? = null,
    val topGenres: List<Pair<String, Int>> = emptyList(),
)
