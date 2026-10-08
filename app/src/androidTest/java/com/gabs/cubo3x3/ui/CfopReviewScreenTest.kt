package com.gabs.cubo3x3.ui

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.gabs.cubo3x3.domain.quiz.CfopAttempt
import com.gabs.cubo3x3.ui.theme.Cubo3x3Theme
import java.util.concurrent.ConcurrentHashMap
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class CfopReviewScreenTest {
    @get:Rule val rule = createComposeRule()
    private val answers = ConcurrentHashMap<String, CfopAttempt>()
    private fun wrong(id: String, case: Int, category: String = "F2L", at: Long = 1) =
        CfopAttempt(id, category, case, if (case == 1) 2 else 1, at)

    @Test fun emptyReviewExplainsHowToCreateHistory() {
        rule.setContent { Cubo3x3Theme(darkTheme = true) { CfopReviewScreen(emptyList(), {}, {}) } }
        rule.onNodeWithText("0 casos praticados · 0 para revisar").assertIsDisplayed()
        rule.onNodeWithText("Ainda não há respostas F2L.", substring = true).assertIsDisplayed()
        rule.onAllNodes(hasText("Treinar revisão", substring = true)).assertCountEquals(0)
    }

    @Test fun categoryFilterAndActualStatisticsAreDisplayed() {
        val rows = listOf(wrong("f2l", 1), wrong("oll", 5, "OLL"))
        rule.setContent { Cubo3x3Theme(darkTheme = false) { CfopReviewScreen(rows, {}, {}) } }
        rule.onNodeWithTag("review-mode-errors").performClick()
        rule.onNodeWithText("1 caso praticado · 1 para revisar").assertIsDisplayed()
        rule.onNodeWithTag("review-F2L-1").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("1 erro na última resposta").assertIsDisplayed()
        rule.onNodeWithText("OLL").performScrollTo().performClick()
        rule.onNodeWithTag("review-OLL-5").performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("review-F2L-1").assertDoesNotExist()
    }

    @Test fun singleCaseRoundCanFinishAndReturnToUpdatedReview() {
        rule.setContent { Cubo3x3Theme(darkTheme = true) {
            CfopReviewScreen(listOf(wrong("before", 1)), { answers.putIfAbsent(it.id, it) }, {})
        } }
        rule.onNodeWithText("Treinar revisão (1 caso)").performScrollTo().performClick()
        rule.onNodeWithText("F2L · Pergunta 1 de 1 · 0 acertos").assertIsDisplayed()
        rule.onNodeWithTag("cfop-option-0").performScrollTo().performClick()
        rule.onNodeWithText("Ver resultado").performScrollTo().performClick()
        rule.onNodeWithText("Rodada concluída").assertIsDisplayed()
        rule.onNodeWithText("Voltar à revisão").performClick()
        rule.onNodeWithText("Revisão espaçada CFOP").performScrollTo().assertIsDisplayed()
        rule.runOnIdle { assertEquals(1, answers.size); assertEquals(1, answers.values.single().caseNumber) }
    }

    @Test fun queueSnapshotSurvivesLiveHistoryChangesAndRestoration() {
        val history = mutableStateOf(listOf(wrong("a", 1, at = 1), wrong("b", 2, at = 2)))
        val restoration = StateRestorationTester(rule)
        restoration.setContent { Cubo3x3Theme(darkTheme = true) {
            CfopReviewScreen(history.value, { answers.putIfAbsent(it.id, it) }, {})
        } }
        rule.onNodeWithText("Treinar revisão (2 casos)").performScrollTo().performClick()
        rule.onNodeWithTag("cfop-option-0").performScrollTo().performClick()
        rule.runOnIdle { history.value = emptyList() }
        restoration.emulateSavedInstanceStateRestore()
        rule.onNodeWithTag("cfop-option-0").performScrollTo().assertIsNotEnabled()
        rule.onNodeWithText("Próximo padrão").performScrollTo().performClick()
        rule.onNodeWithText("F2L · Pergunta 2 de 2", substring = true).performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("cfop-option-0").performScrollTo().performClick()
        rule.runOnIdle { assertEquals(listOf(1, 2), answers.values.map { it.caseNumber }.sorted()) }
    }

    @Test fun reviewCategoryAndScrollSurviveOpeningCase() {
        val history = (1..21).map { wrong("pll-$it", it, "PLL", it.toLong()) }
        rule.setContent { Cubo3x3Theme(darkTheme = true) {
            var opened by rememberSaveable { mutableStateOf(false) }
            val holder = rememberSaveableStateHolder()
            if (opened) TextButton(onClick = { opened = false }) { Text("Voltar à revisão") }
            else holder.SaveableStateProvider("review") {
                CfopReviewScreen(history, {}, { opened = true })
            }
        } }
        rule.onNodeWithText("PLL").performClick()
        rule.onNodeWithTag("cfop-review").performScrollToNode(hasText("Estudar PLL-16"))
        rule.onNodeWithText("Estudar PLL-16").performScrollTo().performClick()
        rule.onNodeWithText("Voltar à revisão").performClick()
        rule.onNodeWithText("Estudar PLL-16").assertIsDisplayed()
    }
}
