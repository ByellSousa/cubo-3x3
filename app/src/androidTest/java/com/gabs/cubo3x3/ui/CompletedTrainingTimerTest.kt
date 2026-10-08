package com.gabs.cubo3x3.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import com.gabs.cubo3x3.data.progress.AlgorithmProgress
import com.gabs.cubo3x3.data.timer.TimerSession
import com.gabs.cubo3x3.domain.timer.CfopTrainingMode
import com.gabs.cubo3x3.domain.timer.CfopTrainingPlan
import com.gabs.cubo3x3.ui.theme.Cubo3x3Theme
import java.util.concurrent.CopyOnWriteArrayList
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class CompletedTrainingTimerTest {
    @get:Rule val rule = createComposeRule()
    private val savedIds = CopyOnWriteArrayList<String>()
    private val plan = CfopTrainingPlan(
        mode = CfopTrainingMode.COMPLETED, extraCaseIds = setOf("F2L-1", "OLL-2"),
    )

    @Composable private fun Timer(
        selection: CfopTrainingPlan = plan,
        progress: Map<String, AlgorithmProgress> = emptyMap(),
    ) {
        Cubo3x3Theme(darkTheme = true) {
            TimerScreen(
                solveTimes = emptyList(), sessions = listOf(TimerSession(1, "Isolada", 0)),
                selectedSessionId = 1, inspectionEnabled = false, inspectionSoundsEnabled = false,
                onInspectionEnabledChange = {}, onInspectionSoundsEnabledChange = {},
                onSaveSolveTime = { duration, _, _, category, number ->
                    assertTrue("Expected a short measured attempt, not device uptime", duration in 1..10_000)
                    savedIds += "$category-$number"
                },
                onUpdateSolveTime = { _, _, _ -> }, onDeleteSolveTime = {}, onClearSolveTimes = {},
                onSelectSession = {}, onCreateSession = {}, onRenameSession = { _, _ -> },
                onDeleteSession = {}, initialTrainingPlan = selection, algorithmProgress = progress,
            )
        }
    }

    private fun waitPrepared() {
        rule.waitUntil(5_000) {
            rule.onAllNodesWithContentDescription("Cronômetro parado. Toque para iniciar")
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun finishOne() {
        waitPrepared()
        rule.onNodeWithTag("timer-touch-area").performClick()
        rule.onNodeWithContentDescription("Cronômetro em andamento. Toque para concluir").assertExists()
        Thread.sleep(60)
        rule.onNodeWithTag("timer-touch-area").performClick()
    }

    @Test fun everySelectedCaseIsPracticedOnceAndCompletionStopsTheRound() {
        rule.setContent { Timer() }
        finishOne()
        finishOne()
        rule.onNodeWithTag("timer-training-label").assertTextContains("2/2 praticados", substring = true)
        rule.onNodeWithText("Rodada concluída").assertIsDisplayed()
        rule.onNodeWithTag("timer-touch-area").performClick()
        assertEquals(setOf("F2L-1", "OLL-2"), savedIds.toSet())
        assertEquals(2, savedIds.size)
        rule.onNodeWithText("Repetir treino").performClick()
        waitPrepared()
        rule.onNodeWithTag("timer-training-label").assertTextContains("0/2 praticados", substring = true)
    }

    @Test fun skippingAndGoingBackNeverCountsAsPractice() {
        rule.setContent { Timer() }
        waitPrepared()
        rule.onNodeWithText("Novo", substring = false).performClick()
        waitPrepared()
        rule.onNodeWithText("Anterior").performClick()
        rule.onNodeWithTag("timer-training-label").assertTextContains("0/2 praticados", substring = true)
        assertTrue(savedIds.isEmpty())
        finishOne()
        finishOne()
        assertEquals(2, savedIds.distinct().size)
    }

    @Test fun recreationPreservesCoverageAndDoesNotSaveTwice() {
        val restoration = StateRestorationTester(rule)
        restoration.setContent { Timer() }
        finishOne()
        waitPrepared()
        restoration.emulateSavedInstanceStateRestore()
        waitPrepared()
        rule.onNodeWithTag("timer-training-label").assertTextContains("1/2 praticados", substring = true)
        assertEquals(1, savedIds.size)
        finishOne()
        assertEquals(2, savedIds.distinct().size)
    }

    @Test fun liveMarkChangesEnterOnlyOnRepeat() {
        val progress = mutableStateOf(mapOf("F2L-1" to AlgorithmProgress(isCompleted = true)))
        val selection = CfopTrainingPlan(mode = CfopTrainingMode.COMPLETED)
        rule.setContent { Timer(selection, progress.value) }
        waitPrepared()
        rule.runOnIdle { progress.value = progress.value +
            ("OLL-2" to AlgorithmProgress(isCompleted = true)) }
        finishOne()
        rule.onNodeWithTag("timer-training-label").assertTextContains("1/1 praticados", substring = true)
        rule.onNodeWithText("Repetir treino").performClick()
        waitPrepared()
        rule.onNodeWithTag("timer-training-label").assertTextContains("0/2 praticados", substring = true)
        assertEquals(listOf("F2L-1"), savedIds.toList())
    }

    @Test fun switchingTabsKeepsTheCurrentSetupAndCoverage() {
        rule.setContent {
            var hidden by rememberSaveable { mutableStateOf(false) }
            val holder = rememberSaveableStateHolder()
            Column(Modifier.fillMaxSize()) {
                TextButton(onClick = { hidden = !hidden }) { Text("Trocar aba no teste") }
                Box(Modifier.weight(1f)) {
                    if (!hidden) holder.SaveableStateProvider("timer") { Timer() }
                }
            }
        }
        finishOne()
        waitPrepared()
        val before = rule.onNodeWithTag("timer-training-label").fetchSemanticsNode().config[SemanticsProperties.Text]
        rule.onNodeWithText("Trocar aba no teste").performClick()
        rule.onNodeWithText("Trocar aba no teste").performClick()
        waitPrepared()
        assertEquals(before,
            rule.onNodeWithTag("timer-training-label").fetchSemanticsNode().config[SemanticsProperties.Text])
        assertEquals(1, savedIds.size)
        finishOne()
        assertEquals(2, savedIds.distinct().size)
    }
}
