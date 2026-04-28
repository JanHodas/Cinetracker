package com.example.cinetracker.data.local

import androidx.room.TypeConverter
import com.example.cinetracker.domain.model.WatchStatus
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Room [TypeConverter]s for types that have no built-in column affinity.
 *
 * - **`List<String>` ↔ JSON** — genres are stored as a compact JSON array
 *   (e.g. `["Action","Drama"]`). Using kotlinx-serialization keeps it
 *   type-safe without manual splitting/joining.
 * - **[WatchStatus] ↔ String** — stored as the enum constant name
 *   (`WANT_TO_WATCH`, `WATCHING`, `WATCHED`).
 */
class Converters {

    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromStringList(value: List<String>): String = json.encodeToString(value)

    @TypeConverter
    fun toStringList(value: String): List<String> = json.decodeFromString(value)

    @TypeConverter
    fun fromWatchStatus(status: WatchStatus): String = status.name

    @TypeConverter
    fun toWatchStatus(value: String): WatchStatus = WatchStatus.valueOf(value)
}
