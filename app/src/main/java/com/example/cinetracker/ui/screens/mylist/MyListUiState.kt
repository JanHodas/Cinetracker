package com.example.cinetracker.ui.screens.mylist

import com.example.cinetracker.domain.model.SavedMovie
import com.example.cinetracker.domain.model.WatchStatus

/** UI state for the "My List" screen. */
data class MyListUiState(
    /** Currently selected watch-status filter. `null` = show all. */
    val activeStatusFilter: WatchStatus? = null,
    /** Currently selected media-type filter. `null` = show all. */
    val activeMediaTypeFilter: String? = null,
    /** Items matching the active filters. */
    val items: List<SavedMovie> = emptyList(),
)
