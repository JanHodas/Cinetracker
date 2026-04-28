package com.example.cinetracker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * Room database holding the user's personal movie collection.
 *
 * A single [MovieEntity] table stores both the TMDB metadata (poster, genres …)
 * and the user-specific fields (watch status, rating, note). This keeps the
 * schema simple — the app never needs relational joins for its core use-cases.
 */
@Database(
    entities = [MovieEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class CineTrackDatabase : RoomDatabase() {

    abstract fun movieDao(): MovieDao

    companion object {
        private const val DATABASE_NAME = "cinetrack.db"

        @Volatile
        private var instance: CineTrackDatabase? = null

        /**
         * Returns the singleton database instance, creating it on first access.
         * Thread-safe via double-checked locking.
         */
        fun getInstance(context: Context): CineTrackDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    CineTrackDatabase::class.java,
                    DATABASE_NAME,
                ).build().also { instance = it }
            }
    }
}
