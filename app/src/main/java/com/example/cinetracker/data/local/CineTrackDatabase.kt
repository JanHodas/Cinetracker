package com.example.cinetracker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room database holding the user's personal media collection (movies and TV shows).
 *
 * A single [MovieEntity] table stores both the TMDB metadata (poster, genres …)
 * and the user-specific fields (watch status, rating, note). The `mediaType`
 * column (`"movie"` / `"tv"`) discriminates between movies and TV shows.
 */
@Database(
    entities = [MovieEntity::class],
    version = 2,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class CineTrackDatabase : RoomDatabase() {

    abstract fun movieDao(): MovieDao

    companion object {
        private const val DATABASE_NAME = "cinetrack.db"

        /**
         * Migration from v1 → v2: adds `mediaType` (TEXT, default "movie") and
         * `numberOfSeasons` (INTEGER, nullable) columns for TV show support.
         * Existing rows are automatically treated as movies via the defaults.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE movies ADD COLUMN mediaType TEXT NOT NULL DEFAULT 'movie'")
                db.execSQL("ALTER TABLE movies ADD COLUMN numberOfSeasons INTEGER DEFAULT NULL")
            }
        }

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
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { instance = it }
            }
    }
}
