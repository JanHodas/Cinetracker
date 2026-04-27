package com.example.cinetracker.data.remote.dto

import kotlinx.serialization.Serializable

/** Single TMDB genre as returned by `/genre/movie/list` and embedded in movie detail. */
@Serializable
data class TmdbGenreDto(
    val id: Int,
    val name: String,
)
