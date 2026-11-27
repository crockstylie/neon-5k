package fr.antoinehory.neon5k.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import fr.antoinehory.neon5k.data.local.dao.AchievementDao
import fr.antoinehory.neon5k.data.local.dao.DecisionLogDao
import fr.antoinehory.neon5k.data.local.dao.MatchDao
import fr.antoinehory.neon5k.data.local.dao.StatsDao
import fr.antoinehory.neon5k.data.local.entity.AchievementEntity
import fr.antoinehory.neon5k.data.local.entity.DecisionLogEntity
import fr.antoinehory.neon5k.data.local.entity.PlayerStatsEntity
import fr.antoinehory.neon5k.data.local.entity.SavedMatchEntity

/**
 * Base de données Room principale de Neon 5K (`neon_5k.db`).
 */
@Database(
    entities = [
        SavedMatchEntity::class,
        PlayerStatsEntity::class,
        DecisionLogEntity::class,
        AchievementEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun matchDao(): MatchDao
    abstract fun statsDao(): StatsDao
    abstract fun decisionLogDao(): DecisionLogDao
    abstract fun achievementDao(): AchievementDao

    companion object {
        private const val DB_NAME = "neon_5k.db"

        /**
         * Migration non-destructive v2 -> v3 : garantit que la table `saved_matches` possède exactement
         * les 10 colonnes JSON officielles (`rulesJson`, `playersJson`, `turnJson`) en préservant toutes les lignes.
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `saved_matches_new` (
                        `matchUuid` TEXT NOT NULL,
                        `createdAtEpochMs` INTEGER NOT NULL,
                        `updatedAtEpochMs` INTEGER NOT NULL,
                        `isFinished` INTEGER NOT NULL,
                        `winnerPlayerId` INTEGER,
                        `isInstantWin` INTEGER NOT NULL,
                        `totalRollsInMatch` INTEGER NOT NULL,
                        `rulesJson` TEXT NOT NULL,
                        `playersJson` TEXT NOT NULL,
                        `turnJson` TEXT NOT NULL,
                        PRIMARY KEY(`matchUuid`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `saved_matches_new` (
                        `matchUuid`, `createdAtEpochMs`, `updatedAtEpochMs`, `isFinished`,
                        `winnerPlayerId`, `isInstantWin`, `totalRollsInMatch`,
                        `rulesJson`, `playersJson`, `turnJson`
                    )
                    SELECT
                        `matchUuid`, `createdAtEpochMs`, `updatedAtEpochMs`, `isFinished`,
                        `winnerPlayerId`, `isInstantWin`, `totalRollsInMatch`,
                        `rulesJson`, `playersJson`, `turnJson`
                    FROM `saved_matches`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `saved_matches`")
                db.execSQL("ALTER TABLE `saved_matches_new` RENAME TO `saved_matches`")
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DB_NAME
                )
                    .addMigrations(MIGRATION_2_3)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
