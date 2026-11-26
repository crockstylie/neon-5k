package fr.antoinehory.neon5k.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import fr.antoinehory.neon5k.data.local.dao.DecisionLogDao
import fr.antoinehory.neon5k.data.local.dao.MatchDao
import fr.antoinehory.neon5k.data.local.dao.StatsDao
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
        DecisionLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun matchDao(): MatchDao
    abstract fun statsDao(): StatsDao
    abstract fun decisionLogDao(): DecisionLogDao

    companion object {
        private const val DB_NAME = "neon_5k.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DB_NAME
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
