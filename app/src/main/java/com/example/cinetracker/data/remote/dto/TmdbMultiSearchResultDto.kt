package com.example.cinetracker.data.remote.dto

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Polymorphic result from `/search/multi`. TMDB returns movies, TV shows, and persons
 * in the same list, distinguished only by the `media_type` JSON field.
 *
 * Deserialization dispatches to the correct subtype via [TmdbMultiSearchResultSerializer].
 * Person results (and any future unknown types) deserialize to [Unknown] and are filtered
 * out by the repository layer.
 */
@Serializable(with = TmdbMultiSearchResultSerializer::class)
sealed interface TmdbMultiSearchResultDto {

    /** Movie result — same shape as `/search/movie` plus `media_type`. */
    @Serializable
    data class MovieResult(
        val id: Int,
        val title: String = "",
        val overview: String = "",
        @SerialName("poster_path") val posterPath: String? = null,
        @SerialName("backdrop_path") val backdropPath: String? = null,
        @SerialName("release_date") val releaseDate: String? = null,
        @SerialName("genre_ids") val genreIds: List<Int> = emptyList(),
        @SerialName("vote_average") val voteAverage: Float = 0f,
    ) : TmdbMultiSearchResultDto

    /** TV show result — same shape as a TV search hit plus `media_type`. */
    @Serializable
    data class TvResult(
        val id: Int,
        val name: String = "",
        val overview: String = "",
        @SerialName("poster_path") val posterPath: String? = null,
        @SerialName("backdrop_path") val backdropPath: String? = null,
        @SerialName("first_air_date") val firstAirDate: String? = null,
        @SerialName("genre_ids") val genreIds: List<Int> = emptyList(),
        @SerialName("vote_average") val voteAverage: Float = 0f,
    ) : TmdbMultiSearchResultDto

    /** Catch-all for person results and unknown future media types. */
    @Serializable
    data object Unknown : TmdbMultiSearchResultDto
}

/**
 * Chooses the concrete [TmdbMultiSearchResultDto] subtype based on the `media_type` field
 * present in each element of the `/search/multi` response.
 */
internal object TmdbMultiSearchResultSerializer :
    JsonContentPolymorphicSerializer<TmdbMultiSearchResultDto>(TmdbMultiSearchResultDto::class) {

    override fun selectDeserializer(
        element: JsonElement,
    ): DeserializationStrategy<TmdbMultiSearchResultDto> {
        val mediaType = element.jsonObject["media_type"]?.jsonPrimitive?.content
        return when (mediaType) {
            "movie" -> TmdbMultiSearchResultDto.MovieResult.serializer()
            "tv" -> TmdbMultiSearchResultDto.TvResult.serializer()
            else -> TmdbMultiSearchResultDto.Unknown.serializer()
        }
    }
}
