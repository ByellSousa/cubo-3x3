package com.gabs.cubo3x3.ui

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.gabs.cubo3x3.domain.quiz.CfopAttempt
import com.gabs.cubo3x3.domain.quiz.CfopSpacedReview
import com.gabs.cubo3x3.ui.theme.Cubo3x3Theme
import java.util.concurrent.ConcurrentHashMap
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Only local fixtures/callbacks: no personal Room or DataStore access. */
class CfopSpacedReviewScreenTest {
    @get:Rule val rule = createComposeRule()
    private val day = CfopSpacedReview.DAY_MILLIS
    private fun answer(id: String, case: Int = 1, correct: Boolean = true,
        at: Long = 0L, category: String = "F2L") =
        CfopAttempt(id, category, case, if (correct) case else if (case == 1) 2 else 1, at)

    @Test fun correctRecentCaseShowsFutureAgendaButDoesNotOfferDueTraining() {
        rule.setContent { Cubo3x3Theme(darkTheme = true) {
            CfopReviewScreen(listOf(answer("correct")), {}, {}, clock = { 100L })
        } }
        rule.onNodeWithText("1 caso praticado · 0 para revisar").assertIsDisplayed()
        rule.onNodeWithText("Nenhuma revisão vencida em F2L.", substring = true).assertIsDisplayed()
        rule.onNodeWithTag("review-start").assertDoesNotExist()
        rule.onNodeWithTag("review-next-date").assertIsDisplayed()
        rule.onNodeWithTag("review-show-upcoming").performScrollTo().performClick()
        rule.onNodeWithTag("upcoming-F2L-1").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Intervalo atual: 1 dia").performScrollTo().assertIsDisplayed()
    }

    @Test fun dueCorrectCaseReturnsEvenWithoutRecentErrors() {
        val rows = listOf(answer("first"), answer("second", at = day))
        rule.setContent { Cubo3x3Theme(darkTheme = false) {
            CfopReviewScreen(rows, {}, {}, clock = { 4 * day })
        } }
        rule.onNodeWithText("1 caso praticado · 1 para revisar").assertIsDisplayed()
        rule.onNodeWithTag("review-F2L-1").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Intervalo atual: 3 dias").performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("review-mode-errors").performScrollTo().performClick()
        rule.onNodeWithText("Nenhum erro recente em F2L.", substring = true).assertIsDisplayed()
        rule.onNodeWithTag("review-start").assertDoesNotExist()
    }

    @Test fun correctedErrorRemainsInOriginalErrorModeBeforeSpacedDeadline() {
        val rows = listOf(answer("wrong", correct = false), answer("correct", at = 100))
        rule.setContent { Cubo3x3Theme(darkTheme = true) {
            CfopReviewScreen(rows, {}, {}, clock = { 200L })
        } }
        rule.onNodeWithText("1 caso praticado · 0 para revisar").assertIsDisplayed()
        rule.onNodeWithTag("review-mode-errors").performClick()
        rule.onNodeWithText("1 caso praticado · 1 para revisar").assertIsDisplayed()
        rule.onNodeWithTag("review-F2L-1").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("1 erro nas últimas 2 respostas").assertIsDisplayed()
    }

    @Test fun upcomingCategoryExpansionAndScrollSurviveDetailAndRecreation() {
        val rows = (1..21).map { answer("pll-$it", it, category = "PLL") }
        val restoration = StateRestorationTester(rule)
        restoration.setContent { Cubo3x3Theme(darkTheme = true) {
            var opened by rememberSaveable { mutableStateOf(false) }
            val holder = rememberSaveableStateHolder()
            if (opened) TextButton(onClick = { opened = false }) { Text("Voltar à revisão") }
            else holder.SaveableStateProvider("review") {
                CfopReviewScreen(rows, {}, { opened = true }, clock = { 100L })
            }
        } }
        rule.onNodeWithText("PLL").performClick()
        rule.onNodeWithTag("review-show-upcoming").performScrollTo().performClick()
        rule.onNodeWithTag("cfop-review").performScrollToNode(hasText("Estudar PLL-16"))
        rule.onNodeWithText("Estudar PLL-16").performScrollTo().performClick()
        rule.onNodeWithText("Voltar à revisão").performClick()
        rule.onNodeWithText("Estudar PLL-16").assertIsDisplayed()
        restoration.emulateSavedInstanceStateRestore()
        rule.onNodeWithText("Estudar PLL-16").assertIsDisplayed()
        rule.onNodeWithTag("cfop-review").performScrollToNode(hasTestTag("review-show-upcoming"))
        rule.onNodeWithTag("review-show-upcoming").performScrollTo()
        rule.onNodeWithText("Ocultar próximas revisões").assertIsDisplayed()
    }

    @Test fun activeSnapshotIgnoresNewDueCasesAndHistoryChangesAcrossRestore() {
        var now = day - 1
        val history = mutableStateOf(listOf(answer("wrong", correct = false), answer("future", 2)))
        val saved = ConcurrentHashMap<String, CfopAttempt>()
        val restoration = StateRestorationTester(rule)
        restoration.setContent { Cubo3x3Theme(darkTheme = true) {
            CfopReviewScreen(history.value, { saved.putIfAbsent(it.id, it) }, {}, clock = { now })
        } }
        rule.onNodeWithTag("review-start").performScrollTo().performClick()
        rule.onNodeWithText("F2L · Pergunta 1 de 1 · 0 acertos").assertIsDisplayed()
        rule.onNodeWithTag("cfop-option-0").performScrollTo().performClick()
        rule.runOnIdle {
            now = day + 1
            history.value += answer("new-error", 3, correct = false, at = day)
        }
        restoration.emulateSavedInstanceStateRestore()
        rule.onNodeWithTag("cfop-option-0").performScrollTo().assertIsNotEnabled()
        rule.onNodeWithText("Ver resultado").performScrollTo().performClick()
        rule.onNodeWithText("Rodada concluída").assertIsDisplayed()
        rule.onNodeWithText("Voltar à revisão").performClick()
        rule.onNodeWithText("3 casos praticados · 3 para revisar").assertIsDisplayed()
        rule.runOnIdle {
            assertEquals(1, saved.size)
            assertEquals(1, saved.values.single().caseNumber)
        }
    }

    @Test fun startRechecksClockSoRollbackCannotStartStaleDueQueue() {
        var now = day
        rule.setContent { Cubo3x3Theme(darkTheme = true) {
            CfopReviewScreen(listOf(answer("correct")), {}, {}, clock = { now })
        } }
        rule.onNodeWithTag("review-start").performScrollTo().assertIsDisplayed()
        rule.runOnIdle { now = 0 }
        rule.onNodeWithTag("review-start").performClick()
        rule.onNodeWithTag("cfop-recognition").assertDoesNotExist()
        rule.onNodeWithText("1 caso praticado · 0 para revisar").assertIsDisplayed()
    }

    @Test fun returningToForegroundRefreshesDueQueueWithoutNewAnswer() {
        val owner = object : LifecycleOwner {
            override val lifecycle = LifecycleRegistry.createUnsafe(this)
        }
        owner.lifecycle.currentState = Lifecycle.State.RESUMED
        var now = 0L
        rule.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                Cubo3x3Theme(darkTheme = true) {
                    CfopReviewScreen(listOf(answer("correct")), {}, {}, clock = { now })
                }
            }
        }
        rule.onNodeWithText("1 caso praticado · 0 para revisar").assertIsDisplayed()
        rule.runOnIdle {
            owner.lifecycle.currentState = Lifecycle.State.CREATED
            now = day
        }
        rule.runOnIdle { owner.lifecycle.currentState = Lifecycle.State.RESUMED }
        rule.onNodeWithText("1 caso praticado · 1 para revisar").assertIsDisplayed()
        rule.onNodeWithTag("review-start").performScrollTo().assertIsDisplayed()
    }
}
