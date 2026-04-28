package com.example.cinetracker.ui.screens.mylist

import com.example.cinetracker.domain.model.SavedMovie
import com.example.cinetracker.domain.model.WatchStatus

/** UI state for the "My List" screen. */
data class MyListUiState(
    /** Currently selected filter tab. `null` = show all. */
    val activeFilter: WatchStatus? = null,
    /** Movies matching the active filter. */
    val movies: List<SavedMovie> = emptyList(),
)
