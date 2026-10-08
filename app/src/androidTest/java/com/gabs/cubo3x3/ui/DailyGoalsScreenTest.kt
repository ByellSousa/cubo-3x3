package com.gabs.cubo3x3.ui

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.gabs.cubo3x3.data.timer.SolvePenalty
import com.gabs.cubo3x3.data.timer.SolveTime
import com.gabs.cubo3x3.domain.quiz.CfopAttempt
import com.gabs.cubo3x3.domain.training.DailyGoalSettings
import com.gabs.cubo3x3.ui.theme.Cubo3x3Theme
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Synthetic callbacks and histories only; never accesses personal Room or DataStore. */
class DailyGoalsScreenTest {
    @get:Rule val rule = createComposeRule()
    private val zone = ZoneId.of("America/Sao_Paulo")
    private val now = Instant.parse("2026-10-08T12:00:00Z").toEpochMilli()
    private fun answer(id: String = "a", at: Long = now, chosen: Int = 1) =
        CfopAttempt(id, "F2L", 1, chosen, at)
    private fun solve(id: Long = 1, category: String? = "F2L", penalty: SolvePenalty = SolvePenalty.DNF) =
        SolveTime(id, 1_000, now, "R", "", penalty, 1, category, if (category == null) null else 1)
    private fun click(tag: String) = rule.onNodeWithTag(tag).performScrollTo().performClick()
    private fun input(tag: String, value: String) =
        rule.onNodeWithTag(tag).performScrollTo().performTextReplacement(value)
    private fun waitText(tag: String, value: String) = rule.waitUntil(10_000) {
        rule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() &&
            rule.onAllNodes(hasTestTag(tag) and hasText(value)).fetchSemanticsNodes().isNotEmpty()
    }

    @Test fun defaultsAreDisabledAndChangesRequireExplicitSave() {
        val saved = mutableStateOf(DailyGoalSettings())
        var calls = 0
        rule.setContent { Cubo3x3Theme(darkTheme = true) {
            DailyGoalsScreen(saved.value, emptyList(), emptyList(),
                { saved.value = it; calls++ }, {}, { now }, { zone })
        } }
        rule.onNodeWithTag("daily-goals-enabled").assertIsOff()
        click("daily-goals-enabled")
        input("daily-goals-recognition-input", "3")
        input("daily-goals-execution-input", "8")
        rule.runOnIdle { assertEquals(0, calls); assertFalse(saved.value.enabled) }
        click("daily-goals-save")
        waitText("daily-goals-status", "Metas salvas.")
        rule.runOnIdle { assertEquals(DailyGoalSettings(true, 3, 8), saved.value); assertEquals(1, calls) }
    }

    @Test fun invalidZeroPairAndOutOfRangeTargetsDisableSave() {
        var calls = 0
        rule.setContent { Cubo3x3Theme(darkTheme = true) {
            DailyGoalsScreen(DailyGoalSettings(), emptyList(), emptyList(), { calls++ }, {}, { now }, { zone })
        } }
        click("daily-goals-enabled")
        input("daily-goals-recognition-input", "0")
        input("daily-goals-execution-input", "0")
        rule.onNodeWithTag("daily-goals-save").performScrollTo().assertIsNotEnabled()
        rule.onNodeWithTag("daily-goals-invalid").assertExists()
        input("daily-goals-recognition-input", "101")
        rule.onNodeWithTag("daily-goals-save").performScrollTo().assertIsNotEnabled()
        input("daily-goals-recognition-input", "1")
        rule.onNodeWithTag("daily-goals-save").performScrollTo().assertIsEnabled()
        rule.runOnIdle { assertEquals(0, calls) }
    }

    @Test fun disablingKeepsConfiguredTargetsAndDoesNotClearPractice() {
        val saved = mutableStateOf(DailyGoalSettings(true, 2, 4))
        val rows = listOf(answer())
        rule.setContent { Cubo3x3Theme(darkTheme = false) {
            DailyGoalsScreen(saved.value, rows, listOf(solve()), { saved.value = it }, {}, { now }, { zone })
        } }
        click("daily-goals-enabled")
        click("daily-goals-save")
        waitText("daily-goals-status", "Metas salvas.")
        rule.runOnIdle { assertEquals(DailyGoalSettings(false, 2, 4), saved.value); assertEquals(1, rows.size) }
    }

    @Test fun draftsSurviveRecreationButBackDoesNotSave() {
        val restoration = StateRestorationTester(rule)
        var saves = 0
        var backs = 0
        restoration.setContent { Cubo3x3Theme(darkTheme = true) {
            DailyGoalsScreen(DailyGoalSettings(), emptyList(), emptyList(), { saves++ },
                { backs++ }, { now }, { zone })
        } }
        input("daily-goals-recognition-input", "12")
        restoration.emulateSavedInstanceStateRestore()
        rule.onNodeWithTag("daily-goals-recognition-input").performScrollTo().assertTextContains("12")
        click("daily-goals-back")
        rule.runOnIdle { assertEquals(0, saves); assertEquals(1, backs) }
    }

    @Test fun savedHistoryIncludesErrorsAndDnfNotFreeAndNeverDoubleCountsIds() {
        val rows = listOf(answer(), answer(), answer("wrong", chosen = 2))
        val times = listOf(solve(), solve(), solve(2, category = null))
        rule.setContent { Cubo3x3Theme(darkTheme = true) {
            DailyGoalsScreen(DailyGoalSettings(true, 1, 1), rows, times, {}, {}, { now }, { zone })
        } }
        waitText("daily-goals-recognition", "Respostas CFOP: 2 / 1")
        rule.onNodeWithTag("daily-goals-execution").assertTextEquals("Tentativas dirigidas CFOP: 1 / 1")
        rule.onNodeWithTag("daily-goals-reached").assertExists()
    }

    @Test fun historyRemovalRefreshesBothCountersAndCompletionMessage() {
        val rows = mutableStateOf(listOf(answer()))
        val times = mutableStateOf(listOf(solve()))
        rule.setContent { Cubo3x3Theme(darkTheme = true) {
            DailyGoalsScreen(DailyGoalSettings(true, 1, 1), rows.value, times.value, {}, {}, { now }, { zone })
        } }
        waitText("daily-goals-recognition", "Respostas CFOP: 1 / 1")
        rule.runOnIdle { rows.value = emptyList(); times.value = emptyList() }
        waitText("daily-goals-recognition", "Respostas CFOP: 0 / 1")
        rule.onNodeWithTag("daily-goals-execution").assertTextEquals("Tentativas dirigidas CFOP: 0 / 1")
        rule.onNodeWithTag("daily-goals-reached").assertDoesNotExist()
    }

    @Test fun resumeAfterMidnightRefreshesWithoutAddingHistory() {
        val owner = object : LifecycleOwner { override val lifecycle = LifecycleRegistry.createUnsafe(this) }
        owner.lifecycle.currentState = Lifecycle.State.RESUMED
        var clock = now
        rule.setContent { CompositionLocalProvider(LocalLifecycleOwner provides owner) {
            Cubo3x3Theme(darkTheme = true) {
                DailyGoalsScreen(DailyGoalSettings(true, 1, 0), listOf(answer()), emptyList(),
                    {}, {}, { clock }, { zone })
            }
        } }
        waitText("daily-goals-recognition", "Respostas CFOP: 1 / 1")
        rule.runOnIdle {
            owner.lifecycle.currentState = Lifecycle.State.CREATED
            clock = Instant.parse("2026-10-09T03:00:00Z").toEpochMilli()
        }
        rule.runOnIdle { owner.lifecycle.currentState = Lifecycle.State.RESUMED }
        waitText("daily-goals-date", "Hoje · 09/10/2026")
        rule.onNodeWithTag("daily-goals-recognition").assertTextEquals("Respostas CFOP: 0 / 1")
    }

    @Test fun failedSaveShowsErrorAndLeavesSavedConfigurationUntouched() {
        val original = DailyGoalSettings()
        rule.setContent { Cubo3x3Theme(darkTheme = true) {
            DailyGoalsScreen(original, emptyList(), emptyList(), { error("synthetic failure") }, {}, { now }, { zone })
        } }
        click("daily-goals-enabled")
        click("daily-goals-save")
        waitText("daily-goals-status", "Não foi possível salvar. Reabra esta tela e tente novamente.")
        rule.onNodeWithTag("daily-goals-save").assertIsEnabled()
        rule.runOnIdle { assertFalse(original.enabled) }
    }

    @Test fun backupGenerationDiscardsOldDraftEvenWhenRestoredSettingsAreIdentical() {
        val revision = mutableLongStateOf(0)
        rule.setContent { Cubo3x3Theme(darkTheme = true) {
            key(revision.longValue) {
                DailyGoalsScreen(DailyGoalSettings(), emptyList(), emptyList(), {}, {}, { now }, { zone })
            }
        } }
        input("daily-goals-recognition-input", "12")
        rule.runOnIdle { revision.longValue++ }
        rule.onNodeWithTag("daily-goals-recognition-input").performScrollTo().assertTextContains("5")
    }
}
