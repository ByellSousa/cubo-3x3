package com.gabs.cubo3x3.data.backup

import com.gabs.cubo3x3.cube.CubeColorScheme
import com.gabs.cubo3x3.cube.CubeViewpoint
import com.gabs.cubo3x3.domain.reminder.ReminderDay
import com.gabs.cubo3x3.domain.reminder.ReminderSettings
import com.gabs.cubo3x3.ui.theme.ThemeMode
import com.gabs.cubo3x3.domain.quiz.CfopAttempt
import com.gabs.cubo3x3.domain.algorithm.PreferredCaseFormula
import com.gabs.cubo3x3.domain.timer.CfopTrainingPlan
import com.gabs.cubo3x3.domain.timer.CfopTrainingMode
import com.gabs.cubo3x3.ui.AlgorithmCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupCodecTest {
    @Test fun dailyGoalsRoundTripKeepsAllCollectionsAndReportsConfiguration() {
        val goals = com.gabs.cubo3x3.domain.training.DailyGoalSettings(true, 3, 12)
        val source = sampleBackup().copy(dailyGoals = goals, cfopAttempts = listOf(
            CfopAttempt("answer", "F2L", 1, 2, 900)),
            preferredFormulas = listOf(PreferredCaseFormula("F2L", 1, "R U R' U2'", 900)))
        val restored = BackupCodec.decode(BackupCodec.encode(source))
        assertEquals(source, restored)
        assertTrue(restored.summary().dailyGoalsEnabled)
        assertEquals(3, restored.summary().dailyRecognitionTarget)
        assertEquals(12, restored.summary().dailyExecutionTarget)
    }

    @Test fun allEightLegacyFormatsDisableGoalsWithoutInventingDailyCounters() {
        val source = sampleBackup().copy(
            dailyGoals = com.gabs.cubo3x3.domain.training.DailyGoalSettings(true, 5, 8),
            preferredFormulas = listOf(PreferredCaseFormula("F2L", 1, "R U R' U2'", 900)))
        val json = com.google.gson.JsonParser.parseString(BackupCodec.encode(source)).asJsonObject
        json.getAsJsonObject("settings").remove("dailyGoals")
        (1..8).forEach { version ->
            json.addProperty("schemaVersion", version)
            val restored = BackupCodec.decode(json.toString())
            assertEquals(com.gabs.cubo3x3.domain.training.DailyGoalSettings(), restored.dailyGoals)
            assertEquals(source.progress, restored.progress)
            assertEquals(source.customAlgorithms, restored.customAlgorithms)
            assertEquals(if (version >= 8) source.preferredFormulas else emptyList<PreferredCaseFormula>(),
                restored.preferredFormulas)
        }
    }

    @Test fun schemaNineRejectsMissingInvalidAndNonCanonicalDailyGoalsBeforeConfirmation() {
        listOf(null, "", "1|1|0|0", "1|0|101|0", "1|1|5|-1", "2|1|5|5",
            "1|true|5|5", "1|1|05|5", "1|1|5|5|", "1|1|5|5 ").forEach { value ->
            val json = com.google.gson.JsonParser.parseString(BackupCodec.encode(sampleBackup())).asJsonObject
            if (value == null) json.getAsJsonObject("settings").remove("dailyGoals")
            else json.getAsJsonObject("settings").addProperty("dailyGoals", value)
            expectFailure<BackupException.Invalid> { BackupCodec.decode(json.toString()) }
        }
    }

    @Test fun disabledZeroAndSingleEnabledTargetSurviveBackupRoundTrip() {
        listOf(com.gabs.cubo3x3.domain.training.DailyGoalSettings(false, 0, 0),
            com.gabs.cubo3x3.domain.training.DailyGoalSettings(true, 0, 100)).forEach { goals ->
            assertEquals(goals, BackupCodec.decode(BackupCodec.encode(sampleBackup().copy(dailyGoals = goals))).dailyGoals)
        }
    }

    @Test fun backupReconstructsConfusedPairsWithoutChangingOtherCollections() {
        val source = sampleBackup().copy(
            cfopAttempts = listOf(CfopAttempt("a", "F2L", 1, 2, 10),
                CfopAttempt("b", "F2L", 2, 1, 20), CfopAttempt("c", "OLL", 3, 5, 30)),
            preferredFormulas = listOf(PreferredCaseFormula("F2L", 1, "R U R' U2'", 900)),
        )
        val restored = BackupCodec.decode(BackupCodec.encode(source))
        val before = com.gabs.cubo3x3.domain.quiz.CfopConfusions.pairs(source.cfopAttempts)
        assertEquals(before, com.gabs.cubo3x3.domain.quiz.CfopConfusions.pairs(restored.cfopAttempts))
        assertEquals(source, restored)
        assertEquals(source.solveTimes, restored.solveTimes)
        assertEquals(source.progress, restored.progress)
    }

    @Test fun legacyBackupsOnlyReconstructPairsWhenTheyActuallyContainCfopHistory() {
        val source = sampleBackup().copy(cfopAttempts = listOf(
            CfopAttempt("wrong", "PLL", 1, 21, 10), CfopAttempt("reverse", "PLL", 21, 1, 20)))
        val json = com.google.gson.JsonParser.parseString(BackupCodec.encode(source)).asJsonObject
        (1..7).forEach { version ->
            json.addProperty("schemaVersion", version)
            val restored = BackupCodec.decode(json.toString())
            assertEquals(if (version >= 6) com.gabs.cubo3x3.domain.quiz.CfopConfusions.pairs(source.cfopAttempts)
                else emptyList<com.gabs.cubo3x3.domain.quiz.CfopConfusionPair>(),
                com.gabs.cubo3x3.domain.quiz.CfopConfusions.pairs(restored.cfopAttempts))
            // Older formats never carried these fields; their documented defaults still apply.
            val expectedTimes = source.solveTimes.map { it.copy(
                scramble = if (version >= 3) it.scramble else "",
                comment = if (version >= 3) it.comment else "",
                penalty = if (version >= 3) it.penalty else "NONE",
                sessionId = if (version >= 4) it.sessionId else 1,
                trainingCategory = if (version >= 5) it.trainingCategory else null,
                trainingCaseNumber = if (version >= 5) it.trainingCaseNumber else null,
            ) }
            assertEquals(expectedTimes, restored.solveTimes)
            assertEquals(source.progress, restored.progress)
        }
    }

    @Test fun preferredFormulasRoundTripPreservesDoublePrimesAndSummary() {
        val formula = PreferredCaseFormula("F2L", 1, "R U R' U2'", 900)
        val backup = sampleBackup().copy(preferredFormulas = listOf(formula))
        val restored = BackupCodec.decode(BackupCodec.encode(backup))
        assertEquals(backup, restored)
        assertEquals(1, restored.summary().preferredFormulaCount)
    }

    @Test fun allSevenLegacySchemasHaveNoPreferredFormulasAndKeepOtherData() {
        val json = com.google.gson.JsonParser.parseString(BackupCodec.encode(
            sampleBackup().copy(preferredFormulas = listOf(
                PreferredCaseFormula("F2L", 1, "R U R'", 900))))).asJsonObject
        (1..7).forEach { version ->
            json.addProperty("schemaVersion", version)
            val restored = BackupCodec.decode(json.toString())
            assertTrue(restored.preferredFormulas.isEmpty())
            assertEquals(0, restored.summary().preferredFormulaCount)
            assertTrue(restored.solveTimes.isNotEmpty())
        }
    }

    @Test fun schemaEightRequiresPreferredCollectionAndRejectsInvalidMembers() {
        val json = com.google.gson.JsonParser.parseString(BackupCodec.encode(sampleBackup())).asJsonObject
        json.remove("preferredFormulas")
        expectFailure<BackupException.Invalid> { BackupCodec.decode(json.toString()) }
        val good = PreferredCaseFormula("F2L", 1, "R U R'", 900)
        listOf(listOf(good, good), listOf(good.copy(category = "BAD")),
            listOf(good.copy(caseNumber = 42)), listOf(good.copy(updatedAtEpochMillis = -1)),
            listOf(good.copy(notation = "")), listOf(good.copy(notation = "U")),
            listOf(good.copy(notation = "R Q")), List(120) { good }).forEach { rows ->
            expectFailure<BackupException.Invalid> {
                BackupCodec.decode(BackupCodec.encode(sampleBackup().copy(preferredFormulas = rows)))
            }
        }
    }

    @Test fun preferredSemanticValidationRejectsYellowOnlyPllAndBrokenOllLayers() {
        val pll = com.gabs.cubo3x3.ui.AlgorithmCatalog.entry(AlgorithmCategory.PLL, 1)
        val oll = com.gabs.cubo3x3.ui.AlgorithmCatalog.entry(AlgorithmCategory.OLL, 1)
        listOf(PreferredCaseFormula("PLL", 1, pll.notation + " U", 900),
            PreferredCaseFormula("OLL", 1, oll.notation + " R", 900)).forEach {
            expectFailure<BackupException.Invalid> {
                BackupCodec.decode(BackupCodec.encode(sampleBackup().copy(preferredFormulas = listOf(it))))
            }
        }
    }

    @Test fun backupReconstructsIdenticalSpacedReviewWithoutPersistingAgenda() {
        val day = com.gabs.cubo3x3.domain.quiz.CfopSpacedReview.DAY_MILLIS
        val rows = listOf(CfopAttempt("round-0", "F2L", 1, 1, 0),
            CfopAttempt("round-1", "F2L", 1, 1, day),
            CfopAttempt("round-2", "OLL", 57, 1, day + 100))
        val backup = sampleBackup().copy(cfopAttempts = rows)
        val restored = BackupCodec.decode(BackupCodec.encode(backup))
        assertEquals(com.gabs.cubo3x3.domain.quiz.CfopSpacedReview.schedule(rows),
            com.gabs.cubo3x3.domain.quiz.CfopSpacedReview.schedule(restored.cfopAttempts))
        assertTrue(restored.solveTimes.isNotEmpty())
        assertEquals(backup.solveTimes, restored.solveTimes)
    }

    @Test fun schemaSixReconstructsSameAgendaAndLegacyWithoutAnswersHasNone() {
        val rows = listOf(CfopAttempt("round-0", "PLL", 21, 21, 900))
        val json = com.google.gson.JsonParser.parseString(
            BackupCodec.encode(sampleBackup().copy(cfopAttempts = rows))).asJsonObject
        json.addProperty("schemaVersion", 6)
        assertEquals(com.gabs.cubo3x3.domain.quiz.CfopSpacedReview.schedule(rows),
            com.gabs.cubo3x3.domain.quiz.CfopSpacedReview.schedule(
                BackupCodec.decode(json.toString()).cfopAttempts))
        json.addProperty("schemaVersion", 5)
        assertTrue(com.gabs.cubo3x3.domain.quiz.CfopSpacedReview.schedule(
            BackupCodec.decode(json.toString()).cfopAttempts).isEmpty())
    }

    @Test fun roundTripPreservesCfopAnswersAndSummary() {
        val backup = sampleBackup().copy(cfopAttempts = listOf(
            CfopAttempt("round-0", "F2L", 1, 2, 900),
            CfopAttempt("round-1", "OLL", 57, 57, 901),
        ))
        val restored = BackupCodec.decode(BackupCodec.encode(backup))
        assertEquals(backup, restored)
        assertEquals(2, restored.summary().cfopAttemptCount)
    }

    @Test fun legacySchemaFiveHasNoCfopHistory() {
        val json = BackupCodec.encode(sampleBackup())
            .replace("\"schemaVersion\": ${BackupCodec.SCHEMA_VERSION}", "\"schemaVersion\": 5")
        assertTrue(BackupCodec.decode(json).cfopAttempts.isEmpty())
    }

    @Test fun rejectsDuplicateOrInvalidCfopAnswersBeforeRestoration() {
        val good = CfopAttempt("round-0", "PLL", 1, 2, 900)
        listOf(listOf(good, good), listOf(good.copy(caseNumber = 22)),
            listOf(good.copy(selectedCaseNumber = 22)), listOf(good.copy(category = "OLLX")),
            listOf(good.copy(recordedAtEpochMillis = -1)), listOf(good.copy(id = "../bad")))
            .forEach { rows ->
                expectFailure<BackupException.Invalid> {
                    BackupCodec.decode(BackupCodec.encode(sampleBackup().copy(cfopAttempts = rows)))
                }
            }
    }

    @Test fun schemasWithRecognitionRequireCfopCollection() {
        (6..BackupCodec.SCHEMA_VERSION).forEach { version ->
            val json = com.google.gson.JsonParser.parseString(BackupCodec.encode(sampleBackup())).asJsonObject
            json.addProperty("schemaVersion", version)
            json.remove("cfopAttempts")
            expectFailure<BackupException.Invalid> { BackupCodec.decode(json.toString()) }
        }
    }

    @Test
    fun roundTripPreservesAllDataAndDoublePrimeNotation() {
        val original = sampleBackup()

        val json = BackupCodec.encode(original)
        val restored = BackupCodec.decode(json)

        assertEquals(original, restored)
        assertTrue(json.contains("U2'"))
        assertTrue(json.contains(BackupCodec.FORMAT_ID))
    }

    @Test
    fun rejectsIncompatibleSchemaVersion() {
        val json = BackupCodec.encode(sampleBackup())
            .replace("\"schemaVersion\": ${BackupCodec.SCHEMA_VERSION}", "\"schemaVersion\": 99")

        val error = expectFailure<BackupException.Incompatible> { BackupCodec.decode(json) }

        assertEquals(99, error.schemaVersion)
    }

    @Test
    fun rejectsDuplicatePersistentIdentifiers() {
        val backup = sampleBackup().let { value ->
            value.copy(solveTimes = value.solveTimes + value.solveTimes.first())
        }

        val error = expectFailure<BackupException.Invalid> {
            BackupCodec.decode(BackupCodec.encode(backup))
        }

        assertTrue(error.message.orEmpty().contains("duplicatas"))
    }

    @Test
    fun rejectsInvalidCustomNotation() {
        val backup = sampleBackup().let { value ->
            value.copy(
                customAlgorithms = value.customAlgorithms.map { it.copy(notation = "R Q") },
            )
        }

        val error = expectFailure<BackupException.Invalid> {
            BackupCodec.decode(BackupCodec.encode(backup))
        }

        assertTrue(error.message.orEmpty().contains("Notação inválida"))
    }

    @Test
    fun rejectsDocumentWithMissingCollections() {
        expectFailure<BackupException.Invalid> {
            BackupCodec.decode(
                """{"format":"${BackupCodec.FORMAT_ID}","schemaVersion":1}""",
            )
        }
    }

    @Test
    fun readsSchemaOneWithRemindersDisabledByDefault() {
        val legacy = """
            {
              "format": "${BackupCodec.FORMAT_ID}",
              "schemaVersion": 1,
              "appVersion": "0.8.0-backup",
              "exportedAtEpochMillis": 1750000000000,
              "settings": { "themeMode": "dark" },
              "progress": [],
              "solveTimes": [],
              "quizRecords": [],
              "customAlgorithms": []
            }
        """.trimIndent()

        val restored = BackupCodec.decode(legacy)

        assertEquals(ReminderSettings(), restored.reminderSettings)
    }

    @Test
    fun readsSchemaTwoTimesWithEmptyDetailsAndNoPenalty() {
        val legacy = BackupCodec.encode(sampleBackup())
            .replace("\"schemaVersion\": ${BackupCodec.SCHEMA_VERSION}", "\"schemaVersion\": 2")
            .replace(Regex(",\\s*\"scramble\":.*?\"penalty\":\"PLUS_TWO\""), "")

        val restored = BackupCodec.decode(legacy)

        assertEquals("", restored.solveTimes.single().scramble)
        assertEquals("", restored.solveTimes.single().comment)
        assertEquals("NONE", restored.solveTimes.single().penalty)
        assertEquals(1L, restored.solveTimes.single().sessionId)
    }

    @Test
    fun readsSchemaThreeIntoDefaultSession() {
        val legacy = BackupCodec.encode(sampleBackup())
            .replace("\"schemaVersion\": ${BackupCodec.SCHEMA_VERSION}", "\"schemaVersion\": 3")

        val restored = BackupCodec.decode(legacy)

        assertEquals(listOf("Principal"), restored.timerSessions.map { it.name })
        assertEquals(1L, restored.solveTimes.single().sessionId)
    }

    @Test
    fun readsSchemaFourWithoutTrainingOrigin() {
        val legacy = BackupCodec.encode(sampleBackup())
            .replace("\"schemaVersion\": ${BackupCodec.SCHEMA_VERSION}", "\"schemaVersion\": 4")

        val restored = BackupCodec.decode(legacy)

        assertEquals(null, restored.solveTimes.single().trainingCategory)
        assertEquals(null, restored.solveTimes.single().trainingCaseNumber)
    }

    @Test
    fun rejectsIncompleteTrainingOrigin() {
        val backup = sampleBackup().copy(
            solveTimes = sampleBackup().solveTimes.map {
                it.copy(trainingCaseNumber = null)
            },
        )

        val error = expectFailure<BackupException.Invalid> {
            BackupCodec.decode(BackupCodec.encode(backup))
        }

        assertTrue(error.message.orEmpty().contains("incompleto"))
    }

    @Test fun reusablePlanSurvivesBackupRoundTrip() {
        val plan = CfopTrainingPlan(
            mode = CfopTrainingMode.COMPLETED,
            completedCategories = setOf(AlgorithmCategory.F2L, AlgorithmCategory.OLL),
            extraCaseIds = setOf("F2L-1", "OLL-3"),
        )
        val backup = sampleBackup().copy(cfopTrainingPlan = plan)
        val decoded = BackupCodec.decode(BackupCodec.encode(backup))
        assertEquals(backup, decoded)
        assertEquals("COMPLETED", decoded.summary().trainingPlanMode)
    }

    @Test fun schemasOneThroughSixResetPlanToFreeWithoutLosingLegacyData() {
        (1..6).forEach { version ->
            val original = sampleBackup().copy(
                cfopAttempts = listOf(CfopAttempt("answer", "F2L", 1, 1, 900)),
                cfopTrainingPlan = CfopTrainingPlan(CfopTrainingMode.CASE, AlgorithmCategory.F2L, 2),
            )
            val json = com.google.gson.JsonParser.parseString(BackupCodec.encode(original)).asJsonObject
            json.addProperty("schemaVersion", version)
            json.getAsJsonObject("settings").remove("cfopTrainingPlan")
            val decoded = BackupCodec.decode(json.toString())
            assertEquals(CfopTrainingPlan(), decoded.cfopTrainingPlan)
            assertEquals(original.progress, decoded.progress)
            assertEquals(original.customAlgorithms, decoded.customAlgorithms)
            assertEquals(if (version >= 6) original.cfopAttempts else emptyList<CfopAttempt>(),
                decoded.cfopAttempts)
        }
    }

    @Test fun missingOrInvalidPlanIsRejectedBeforeConfirmation() {
        listOf(null, "", "1|UNKNOWN||0|F2L|", "1|COMPLETED||0|F2L|F2L-999",
            "1|COMPLETED||0|F2L,F2L|", "1|COMPLETED||0|F2L|F2L-1,F2L-1",
            "1|FREE||0|F2L,OLL,PLL|F2L-1").forEach { value ->
            val json = com.google.gson.JsonParser.parseString(BackupCodec.encode(sampleBackup())).asJsonObject
            val settings = json.getAsJsonObject("settings")
            if (value == null) settings.remove("cfopTrainingPlan")
            else settings.addProperty("cfopTrainingPlan", value)
            expectFailure<BackupException.Invalid> { BackupCodec.decode(json.toString()) }
        }
    }

    private fun sampleBackup() = BackupPackage(
        appVersion = "0.8.0-backup",
        exportedAtEpochMillis = 1_750_000_000_000,
        themeMode = ThemeMode.DARK,
        reminderSettings = ReminderSettings(
            enabled = true,
            hour = 20,
            minute = 15,
            days = setOf(ReminderDay.MONDAY, ReminderDay.FRIDAY),
        ),
        progress = listOf(
            BackupProgress("F2L", 1, isFavorite = true, isCompleted = false, 100),
            BackupProgress("OLL", 57, isFavorite = false, isCompleted = true, 101),
        ),
        timerSessions = listOf(BackupTimerSession(1, "Principal", 0)),
        solveTimes = listOf(
            BackupSolveTime(
                4,
                12_340,
                102,
                "R U2' R'",
                "Treino da manhã",
                "PLUS_TWO",
                1,
                "F2L",
                1,
            ),
        ),
        quizRecords = listOf(BackupQuizRecord("ADVANCED", "TIMED", 400, 4, 80, 2, 103)),
        customAlgorithms = listOf(
            BackupCustomAlgorithm(
                id = 7,
                name = "Caso com aspas \"teste\"",
                notation = "R U2' R'",
                tags = listOf("PLL", "rápido"),
                colorScheme = CubeColorScheme.HIGH_CONTRAST,
                viewpoint = CubeViewpoint.LEFT,
                createdAtEpochMillis = 104,
                updatedAtEpochMillis = 105,
            ),
        ),
    )

    private inline fun <reified T : Throwable> expectFailure(block: () -> Unit): T {
        return try {
            block()
            throw AssertionError("Expected ${T::class.java.simpleName}")
        } catch (error: Throwable) {
            if (error is T) error else throw error
        }
    }
}
