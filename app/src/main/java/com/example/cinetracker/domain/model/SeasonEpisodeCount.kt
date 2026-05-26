package com.example.cinetracker.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class SeasonEpisodeCount(
    val seasonNumber: Int,
    val episodeCount: Int,
)
