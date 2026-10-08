package com.gabs.cubo3x3.data.progress

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.gabs.cubo3x3.data.timer.SolveTimeDao
import com.gabs.cubo3x3.data.timer.SolveTimeEntity
import com.gabs.cubo3x3.data.timer.TimerSessionDao
import com.gabs.cubo3x3.data.timer.TimerSessionEntity
import com.gabs.cubo3x3.data.quiz.QuizRecordDao
import com.gabs.cubo3x3.data.quiz.QuizRecordEntity
import com.gabs.cubo3x3.data.custom.CustomAlgorithmDao
import com.gabs.cubo3x3.data.custom.CustomAlgorithmEntity
import com.gabs.cubo3x3.data.backup.BackupDao
import com.gabs.cubo3x3.data.quiz.CfopAttemptDao
import com.gabs.cubo3x3.data.quiz.CfopAttemptEntity
import com.gabs.cubo3x3.data.formula.PreferredFormulaEntity
import com.gabs.cubo3x3.data.formula.PreferredFormulaDao

@Database(
    entities = [
        AlgorithmProgressEntity::class,
        SolveTimeEntity::class,
        QuizRecordEntity::class,
        CustomAlgorithmEntity::class,
        TimerSessionEntity::class,
        CfopAttemptEntity::class,
        PreferredFormulaEntity::class,
    ],
    version = 9,
    exportSchema = true,
)
abstract class CuboDatabase : RoomDatabase() {
    abstract fun algorithmProgressDao(): AlgorithmProgressDao
    abstract fun solveTimeDao(): SolveTimeDao
    abstract fun timerSessionDao(): TimerSessionDao
    abstract fun quizRecordDao(): QuizRecordDao
    abstract fun customAlgorithmDao(): CustomAlgorithmDao
    abstract fun backupDao(): BackupDao
    abstract fun cfopAttemptDao(): CfopAttemptDao
    abstract fun preferredFormulaDao(): PreferredFormulaDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `solve_times` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`duration_millis` INTEGER NOT NULL, " +
                        "`recorded_at_epoch_millis` INTEGER NOT NULL)",
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `quiz_records` (" +
                        "`level` TEXT NOT NULL, " +
                        "`mode` TEXT NOT NULL, " +
                        "`best_score` INTEGER NOT NULL, " +
                        "`best_streak` INTEGER NOT NULL, " +
                        "`best_accuracy_percent` INTEGER NOT NULL, " +
                        "`games_played` INTEGER NOT NULL, " +
                        "`updated_at_epoch_millis` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`level`, `mode`))",
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `custom_algorithms` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT NOT NULL, " +
                        "`notation` TEXT NOT NULL, " +
                        "`tags` TEXT NOT NULL, " +
                        "`color_scheme` TEXT NOT NULL, " +
                        "`viewpoint` TEXT NOT NULL, " +
                        "`created_at_epoch_millis` INTEGER NOT NULL, " +
                        "`updated_at_epoch_millis` INTEGER NOT NULL)",
                )
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `solve_times` ADD COLUMN `scramble` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `solve_times` ADD COLUMN `comment` TEXT NOT NULL DEFAULT ''")
                db.execSQL(
                    "ALTER TABLE `solve_times` ADD COLUMN `penalty` TEXT NOT NULL DEFAULT 'NONE'",
                )
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `timer_sessions` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT NOT NULL, " +
                        "`created_at_epoch_millis` INTEGER NOT NULL)",
                )
                db.execSQL(
                    "INSERT OR IGNORE INTO `timer_sessions` " +
                        "(`id`, `name`, `created_at_epoch_millis`) VALUES (1, 'Principal', 0)",
                )
                db.execSQL(
                    "ALTER TABLE `solve_times` ADD COLUMN `session_id` INTEGER NOT NULL DEFAULT 1",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_solve_times_session_id` " +
                        "ON `solve_times` (`session_id`)",
                )
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `solve_times` ADD COLUMN `training_category` TEXT")
                db.execSQL("ALTER TABLE `solve_times` ADD COLUMN `training_case_number` INTEGER")
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS " +
                        "`index_solve_times_training_category_training_case_number` " +
                        "ON `solve_times` (`training_category`, `training_case_number`)",
                )
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `cfop_attempts` (" +
                        "`id` TEXT NOT NULL PRIMARY KEY, `category` TEXT NOT NULL, " +
                        "`case_number` INTEGER NOT NULL, `selected_case_number` INTEGER NOT NULL, " +
                        "`recorded_at_epoch_millis` INTEGER NOT NULL)",
                )
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `preferred_case_formulas` (" +
                        "`category` TEXT NOT NULL, `case_number` INTEGER NOT NULL, " +
                        "`notation` TEXT NOT NULL, `updated_at_epoch_millis` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`category`, `case_number`))",
                )
            }
        }

        @Volatile
        private var instance: CuboDatabase? = null

        private val createDefaultSessionCallback = object : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "INSERT OR IGNORE INTO `timer_sessions` " +
                        "(`id`, `name`, `created_at_epoch_millis`) VALUES (1, 'Principal', 0)",
                )
            }
        }

        fun getInstance(context: Context): CuboDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                CuboDatabase::class.java,
                "cubo3x3.db",
            ).addMigrations(
                MIGRATION_1_2,
                MIGRATION_2_3,
                MIGRATION_3_4,
                MIGRATION_4_5,
                MIGRATION_5_6,
                MIGRATION_6_7,
                MIGRATION_7_8,
                MIGRATION_8_9,
            ).addCallback(createDefaultSessionCallback)
                .build()
                .also { database -> instance = database }
        }
    }
}
