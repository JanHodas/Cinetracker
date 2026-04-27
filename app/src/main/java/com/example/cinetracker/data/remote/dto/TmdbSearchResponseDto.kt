package com.example.cinetracker.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Paginated response wrapper returned by `/search/movie`. */
@Serializable
data class TmdbSearchResponseDto(
    val page: Int,
    val results: List<TmdbMovieDto>,
    @SerialName("total_pages") val totalPages: Int,
    @SerialName("total_results") val totalResults: Int,
)
