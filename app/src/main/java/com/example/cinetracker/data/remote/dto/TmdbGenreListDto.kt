package com.example.cinetracker.data.remote.dto

import kotlinx.serialization.Serializable

/** Response from `/genre/movie/list` — used to resolve genre IDs to human-readable names. */
@Serializable
data class TmdbGenreListDto(
    val genres: List<TmdbGenreDto>,
)
