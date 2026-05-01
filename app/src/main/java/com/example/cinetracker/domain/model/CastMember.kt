package com.example.cinetracker.domain.model

/** Person credited in a movie or TV show cast list. */
data class CastMember(
    val id: Int,
    val name: String,
    val character: String,
    val profilePath: String? = null,
)
