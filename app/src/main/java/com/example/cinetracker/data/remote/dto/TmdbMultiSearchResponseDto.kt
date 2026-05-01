package com.example.cinetracker.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Paginated response wrapper returned by `/search/multi`. */
@Serializable
data class TmdbMultiSearchResponseDto(
    val page: Int,
    val results: List<TmdbMultiSearchResultDto>,
    @SerialName("total_pages") val totalPages: Int,
    @SerialName("total_results") val totalResults: Int,
)
