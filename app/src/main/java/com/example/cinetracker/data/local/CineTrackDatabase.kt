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
    entities = [MovieEntity::class, WatchedEpisodeEntity::class],
    version = 5,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class CineTrackDatabase : RoomDatabase() {

    abstract fun movieDao(): MovieDao
    abstract fun watchedEpisodeDao(): WatchedEpisodeDao

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

        /**
         * Migration from v2 -> v3: adds `numberOfEpisodes` column to movies table
         * and creates the `watched_episodes` table for episode-level watch tracking.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE movies ADD COLUMN numberOfEpisodes INTEGER DEFAULT NULL")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS watched_episodes (" +
                        "tmdbId INTEGER NOT NULL, " +
                        "seasonNumber INTEGER NOT NULL, " +
                        "episodeNumber INTEGER NOT NULL, " +
                        "watchedAt INTEGER NOT NULL, " +
                        "PRIMARY KEY(tmdbId, seasonNumber, episodeNumber))",
                )
            }
        }

        /**
         * Migration from v3 -> v4: adds `runtime` column (INTEGER, nullable)
         * for storing film length or total TV show runtime in minutes.
         * Also clears the content-sync flag so the next app start fetches runtimes from TMDB.
         */
        fun migration3to4(context: Context) = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE movies ADD COLUMN runtime INTEGER DEFAULT NULL")
                context.getSharedPreferences("cine_track_preferences", Context.MODE_PRIVATE)
                    .edit()
                    .remove("last_synced_content_language")
                    .apply()
            }
        }

        /**
         * Migration from v4 -> v5: adds `runtime` column to watched_episodes table
         * for storing actual per-episode runtime in minutes.
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE watched_episodes ADD COLUMN runtime INTEGER DEFAULT NULL")
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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, migration3to4(context), MIGRATION_4_5)
                    .build()
                    .also { instance = it }
            }
    }
}
