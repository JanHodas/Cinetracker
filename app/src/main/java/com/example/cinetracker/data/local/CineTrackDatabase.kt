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
    entities = [MovieEntity::class, WatchedEpisodeEntity::class, SeasonRatingEntity::class],
    version = 9,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class CineTrackDatabase : RoomDatabase() {

    abstract fun movieDao(): MovieDao
    abstract fun watchedEpisodeDao(): WatchedEpisodeDao
    abstract fun seasonRatingDao(): SeasonRatingDao

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

        /**
         * Migration from v5 -> v6: creates the `season_ratings` table
         * for storing user ratings per TV season.
         */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS season_ratings (" +
                        "tmdbId INTEGER NOT NULL, " +
                        "seasonNumber INTEGER NOT NULL, " +
                        "userRating REAL, " +
                        "ratedAt INTEGER NOT NULL, " +
                        "PRIMARY KEY(tmdbId, seasonNumber))",
                )
            }
        }

        /**
         * Migration from v6 -> v7: adds `sortOrder` column for user-defined
         * list ordering via drag and drop.
         */
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE movies ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0")
            }
        }

        /**
         * Migration from v7 -> v8: converts the earlier `displayOrder`-based draft
         * schema into the final `sortOrder` schema used by drag and drop.
         */
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `movies_new` (" +
                        "`tmdbId` INTEGER NOT NULL, " +
                        "`title` TEXT NOT NULL, " +
                        "`overview` TEXT NOT NULL, " +
                        "`posterPath` TEXT, " +
                        "`backdropPath` TEXT, " +
                        "`releaseDate` TEXT, " +
                        "`genres` TEXT NOT NULL, " +
                        "`tmdbRating` REAL, " +
                        "`watchStatus` TEXT NOT NULL, " +
                        "`userRating` REAL, " +
                        "`note` TEXT NOT NULL, " +
                        "`dateAdded` INTEGER NOT NULL, " +
                        "`mediaType` TEXT NOT NULL DEFAULT 'movie', " +
                        "`numberOfSeasons` INTEGER, " +
                        "`numberOfEpisodes` INTEGER, " +
                        "`runtime` INTEGER, " +
                        "`sortOrder` INTEGER NOT NULL DEFAULT 0, " +
                        "PRIMARY KEY(`tmdbId`))",
                )
                db.execSQL(
                    "INSERT INTO `movies_new` (" +
                        "`tmdbId`, `title`, `overview`, `posterPath`, `backdropPath`, `releaseDate`, " +
                        "`genres`, `tmdbRating`, `watchStatus`, `userRating`, `note`, `dateAdded`, " +
                        "`mediaType`, `numberOfSeasons`, `numberOfEpisodes`, `runtime`, `sortOrder`) " +
                        "SELECT " +
                        "`tmdbId`, `title`, `overview`, `posterPath`, `backdropPath`, `releaseDate`, " +
                        "`genres`, `tmdbRating`, `watchStatus`, `userRating`, `note`, `dateAdded`, " +
                        "`mediaType`, `numberOfSeasons`, `numberOfEpisodes`, `runtime`, " +
                        "(SELECT COUNT(*) FROM movies older " +
                        "WHERE older.displayOrder > movies.displayOrder " +
                        "OR (older.displayOrder = movies.displayOrder AND older.dateAdded > movies.dateAdded)) " +
                        "FROM movies",
                )
                db.execSQL("DROP TABLE `movies`")
                db.execSQL("ALTER TABLE `movies_new` RENAME TO `movies`")
            }
        }

        /**
         * Migration from v8 -> v9: adds cached per-season episode counts for
         * offline TV episode progress actions.
         */
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE movies ADD COLUMN seasonEpisodeCounts TEXT NOT NULL DEFAULT '[]'")
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
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        migration3to4(context),
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7,
                        MIGRATION_7_8,
                        MIGRATION_8_9,
                    )
                    .build()
                    .also { instance = it }
            }
    }
}
