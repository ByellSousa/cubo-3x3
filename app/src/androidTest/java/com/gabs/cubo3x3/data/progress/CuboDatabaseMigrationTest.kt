package com.gabs.cubo3x3.data.progress

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CuboDatabaseMigrationTest {
    @Test
    fun migrate8To9PreservesAllSixTablesAndCreatesEmptyPreferredFormulas() {
        helper.createDatabase("migration-test-v8", 8).apply {
            execSQL("INSERT INTO algorithm_progress VALUES ('F2L', 1, 1, 1, 123)")
            execSQL("INSERT INTO timer_sessions VALUES (1, 'Principal', 0)")
            execSQL("INSERT INTO solve_times VALUES (1, 1000, 123, 'R U', 'nota', 'NONE', 1, 'OLL', 2)")
            execSQL("INSERT INTO quiz_records VALUES ('ESSENTIAL', 'PRACTICE', 100, 1, 100, 1, 123)")
            execSQL("INSERT INTO custom_algorithms VALUES (1, 'Meu', ?, '', 'STANDARD', 'FRONT', 123, 123)",
                arrayOf("U2'"))
            execSQL("INSERT INTO cfop_attempts VALUES ('answer', 'OLL', 2, 3, 123)")
            close()
        }
        helper.runMigrationsAndValidate("migration-test-v8", 9, true, CuboDatabase.MIGRATION_8_9).apply {
            val expected = mapOf(
                "algorithm_progress" to listOf("F2L", "1", "1", "1", "123"),
                "timer_sessions" to listOf("1", "Principal", "0"),
                "solve_times" to listOf("1", "1000", "123", "R U", "nota", "NONE", "1", "OLL", "2"),
                "quiz_records" to listOf("ESSENTIAL", "PRACTICE", "100", "1", "100", "1", "123"),
                "custom_algorithms" to listOf("1", "Meu", "U2'", "", "STANDARD", "FRONT", "123", "123"),
                "cfop_attempts" to listOf("answer", "OLL", "2", "3", "123"),
            )
            expected.forEach { (table, row) ->
                query("SELECT * FROM $table").use { cursor ->
                    assertEquals(1, cursor.count)
                    cursor.moveToFirst()
                    assertEquals(row, (0 until cursor.columnCount).map { cursor.getString(it) })
                }
            }
            query("SELECT COUNT(*) FROM preferred_case_formulas").use {
                it.moveToFirst(); assertEquals(0, it.getInt(0))
            }
            close()
        }
    }

    @Test
    fun fullMigrationChainTo9KeepsOriginalProgressWithoutInventingPreferences() {
        helper.createDatabase("migration-chain-v1-v9", 1).apply {
            execSQL("INSERT INTO algorithm_progress VALUES ('PLL', 21, 1, 1, 999)")
            close()
        }
        helper.runMigrationsAndValidate("migration-chain-v1-v9", 9, true,
            CuboDatabase.MIGRATION_1_2, CuboDatabase.MIGRATION_2_3, CuboDatabase.MIGRATION_3_4,
            CuboDatabase.MIGRATION_4_5, CuboDatabase.MIGRATION_5_6, CuboDatabase.MIGRATION_6_7,
            CuboDatabase.MIGRATION_7_8, CuboDatabase.MIGRATION_8_9,
        ).apply {
            query("SELECT * FROM algorithm_progress").use {
                it.moveToFirst()
                assertEquals(listOf("PLL", "21", "1", "1", "999"),
                    (0 until it.columnCount).map { index -> it.getString(index) })
            }
            query("SELECT COUNT(*) FROM preferred_case_formulas").use {
                it.moveToFirst(); assertEquals(0, it.getInt(0))
            }
            query("SELECT name FROM timer_sessions WHERE id = 1").use {
                it.moveToFirst(); assertEquals("Principal", it.getString(0))
            }
            close()
        }
    }

    @Test
    fun migrate7To8PreservesAllExistingTablesAndCreatesEmptyCfopHistory() {
        helper.createDatabase("migration-test-v7", 7).apply {
            execSQL("INSERT INTO algorithm_progress VALUES ('F2L', 1, 1, 0, 123)")
            execSQL("INSERT INTO timer_sessions VALUES (1, 'Principal', 0)")
            execSQL("INSERT INTO solve_times VALUES (1, 1000, 123, 'R U', 'nota', 'NONE', 1, 'OLL', 2)")
            execSQL("INSERT INTO quiz_records VALUES ('ESSENTIAL', 'PRACTICE', 100, 1, 100, 1, 123)")
            execSQL("INSERT INTO custom_algorithms VALUES (1, 'Meu', ?, '', 'STANDARD', 'FRONT', 123, 123)",
                arrayOf("U2'"))
            close()
        }
        helper.runMigrationsAndValidate("migration-test-v7", 8, true, CuboDatabase.MIGRATION_7_8).apply {
            listOf("algorithm_progress", "timer_sessions", "solve_times", "quiz_records",
                "custom_algorithms").forEach {
                query("SELECT COUNT(*) FROM $it").use { cursor ->
                    cursor.moveToFirst(); assertEquals(1, cursor.getInt(0))
                }
            }
            query("SELECT comment, training_category FROM solve_times").use {
                it.moveToFirst(); assertEquals("nota", it.getString(0)); assertEquals("OLL", it.getString(1))
            }
            query("SELECT COUNT(*) FROM cfop_attempts").use { it.moveToFirst(); assertEquals(0, it.getInt(0)) }
            close()
        }
    }

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        CuboDatabase::class.java,
    )

    @Test
    fun migrate1To2PreservesProgressAndCreatesSolveTimes() {
        helper.createDatabase(TEST_DATABASE, 1).apply {
            execSQL(
                "INSERT INTO algorithm_progress " +
                    "(category, case_number, is_favorite, is_completed, updated_at_epoch_millis) " +
                    "VALUES ('OLL', 7, 1, 0, 1234)",
            )
            close()
        }

        helper.runMigrationsAndValidate(
            TEST_DATABASE,
            2,
            true,
            CuboDatabase.MIGRATION_1_2,
        ).apply {
            query(
                "SELECT is_favorite, is_completed FROM algorithm_progress " +
                    "WHERE category = 'OLL' AND case_number = 7",
            ).use { cursor ->
                cursor.moveToFirst()
                assertEquals(1, cursor.getInt(0))
                assertEquals(0, cursor.getInt(1))
            }
            query("SELECT COUNT(*) FROM solve_times").use { cursor ->
                cursor.moveToFirst()
                assertEquals(0, cursor.getInt(0))
            }
            close()
        }
    }

    @Test
    fun migrate2To3PreservesExistingDataAndCreatesQuizRecords() {
        helper.createDatabase(TEST_DATABASE_V2, 2).apply {
            execSQL(
                "INSERT INTO solve_times (duration_millis, recorded_at_epoch_millis) " +
                    "VALUES (12345, 5678)",
            )
            close()
        }

        helper.runMigrationsAndValidate(
            TEST_DATABASE_V2,
            3,
            true,
            CuboDatabase.MIGRATION_2_3,
        ).apply {
            query("SELECT duration_millis FROM solve_times").use { cursor ->
                cursor.moveToFirst()
                assertEquals(12345L, cursor.getLong(0))
            }
            query("SELECT COUNT(*) FROM quiz_records").use { cursor ->
                cursor.moveToFirst()
                assertEquals(0, cursor.getInt(0))
            }
            close()
        }
    }

    @Test
    fun migrate3To4PreservesQuizRecordsAndCreatesCustomAlgorithms() {
        helper.createDatabase(TEST_DATABASE_V3, 3).apply {
            execSQL(
                "INSERT INTO quiz_records " +
                    "(level, mode, best_score, best_streak, best_accuracy_percent, " +
                    "games_played, updated_at_epoch_millis) " +
                    "VALUES ('ADVANCED', 'TIMED', 500, 4, 80, 2, 100)",
            )
            close()
        }

        helper.runMigrationsAndValidate(
            TEST_DATABASE_V3,
            4,
            true,
            CuboDatabase.MIGRATION_3_4,
        ).apply {
            query("SELECT best_score FROM quiz_records").use { cursor ->
                cursor.moveToFirst()
                assertEquals(500, cursor.getInt(0))
            }
            query("SELECT COUNT(*) FROM custom_algorithms").use { cursor ->
                cursor.moveToFirst()
                assertEquals(0, cursor.getInt(0))
            }
            close()
        }
    }

    @Test
    fun migrate4To5PreservesTimesAndAddsTimerDetails() {
        helper.createDatabase(TEST_DATABASE_V4, 4).apply {
            execSQL(
                "INSERT INTO solve_times (duration_millis, recorded_at_epoch_millis) " +
                    "VALUES (9876, 123456)",
            )
            close()
        }

        helper.runMigrationsAndValidate(
            TEST_DATABASE_V4,
            5,
            true,
            CuboDatabase.MIGRATION_4_5,
        ).apply {
            query(
                "SELECT duration_millis, scramble, comment, penalty FROM solve_times",
            ).use { cursor ->
                cursor.moveToFirst()
                assertEquals(9876L, cursor.getLong(0))
                assertEquals("", cursor.getString(1))
                assertEquals("", cursor.getString(2))
                assertEquals("NONE", cursor.getString(3))
            }
            close()
        }
    }

    @Test
    fun migrate5To6PreservesTimesInDefaultSession() {
        helper.createDatabase(TEST_DATABASE_V5, 5).apply {
            execSQL(
                "INSERT INTO solve_times " +
                    "(duration_millis, recorded_at_epoch_millis, scramble, comment, penalty) " +
                    "VALUES (7654, 456789, 'R U', 'antigo', 'NONE')",
            )
            close()
        }

        helper.runMigrationsAndValidate(
            TEST_DATABASE_V5,
            6,
            true,
            CuboDatabase.MIGRATION_5_6,
        ).apply {
            query("SELECT name FROM timer_sessions WHERE id = 1").use { cursor ->
                cursor.moveToFirst()
                assertEquals("Principal", cursor.getString(0))
            }
            query("SELECT duration_millis, session_id FROM solve_times").use { cursor ->
                cursor.moveToFirst()
                assertEquals(7654L, cursor.getLong(0))
                assertEquals(1L, cursor.getLong(1))
            }
            close()
        }
    }

    @Test
    fun migrate6To7PreservesTimesAndAddsOptionalTrainingOrigin() {
        helper.createDatabase(TEST_DATABASE_V6, 6).apply {
            execSQL(
                "INSERT INTO timer_sessions (id, name, created_at_epoch_millis) " +
                    "VALUES (1, 'Principal', 0)",
            )
            execSQL(
                "INSERT INTO solve_times " +
                    "(duration_millis, recorded_at_epoch_millis, scramble, comment, penalty, session_id) " +
                    "VALUES (6543, 567890, 'R U', '', 'NONE', 1)",
            )
            close()
        }

        helper.runMigrationsAndValidate(
            TEST_DATABASE_V6,
            7,
            true,
            CuboDatabase.MIGRATION_6_7,
        ).apply {
            query(
                "SELECT duration_millis, training_category, training_case_number FROM solve_times",
            ).use { cursor ->
                cursor.moveToFirst()
                assertEquals(6543L, cursor.getLong(0))
                assertEquals(true, cursor.isNull(1))
                assertEquals(true, cursor.isNull(2))
            }
            close()
        }
    }

    private companion object {
        const val TEST_DATABASE = "migration-test"
        const val TEST_DATABASE_V2 = "migration-test-v2"
        const val TEST_DATABASE_V3 = "migration-test-v3"
        const val TEST_DATABASE_V4 = "migration-test-v4"
        const val TEST_DATABASE_V5 = "migration-test-v5"
        const val TEST_DATABASE_V6 = "migration-test-v6"
    }
}
