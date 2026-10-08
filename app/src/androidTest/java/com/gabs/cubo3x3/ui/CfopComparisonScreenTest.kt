package com.gabs.cubo3x3.ui

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.Density
import com.gabs.cubo3x3.domain.quiz.CfopAttempt
import com.gabs.cubo3x3.ui.theme.Cubo3x3Theme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Synthetic history and callbacks only. Never reads the user's Room/DataStore. */
class CfopComparisonScreenTest {
    @get:Rule val rule = createComposeRule()
    private fun answer(id: String, target: Int = 1, chosen: Int = 2,
        category: String = "F2L", at: Long = 0) = CfopAttempt(id, category, target, chosen, at)
    private fun ready(tag: String = "comparison-count") = rule.waitUntil(10_000) {
        rule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
    }
    private fun compare(id: String) {
        ready()
        rule.onNodeWithTag("cfop-comparison-list").performScrollToNode(hasTestTag("compare-$id"))
        rule.onNodeWithTag("compare-$id").performScrollTo().performClick()
    }

    @Test fun correctOnlyHistoryExplainsEmptyPairsWithoutOfferingInventedComparison() {
        rule.setContent { Cubo3x3Theme(darkTheme = true) {
            CfopComparisonScreen(listOf(answer("correct", 1, 1)), AlgorithmCategory.F2L, {}, {})
        } }
        ready()
        rule.onNodeWithTag("comparison-count").assertTextEquals("0 pares em F2L")
        rule.onNodeWithTag("comparison-empty").performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("compare-F2L-1-2").assertDoesNotExist()
    }

    @Test fun reviewEntryDisplaysBothDirectionsAndDoesNotRecordPractice() {
        var saves = 0
        val rows = listOf(answer("a"), answer("b", 2, 1))
        rule.setContent { Cubo3x3Theme(darkTheme = true) {
            CfopReviewScreen(rows, { saves++ }, {}, clock = { 100 })
        } }
        rule.onNodeWithTag("review-compare").performScrollTo().performClick()
        compare("F2L-1-2")
        rule.onNodeWithText("Vi o caso 1 e escolhi o 2: 1").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Vi o caso 2 e escolhi o 1: 1").performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("comparison-formulas-toggle").performScrollTo().performClick()
        rule.onNodeWithTag("comparison-formula-F2L-1").assertTextEquals(
            AlgorithmCatalog.entry(AlgorithmCategory.F2L, 1).notation)
        rule.runOnIdle { assertEquals(0, saves); assertEquals(2, rows.size) }
    }

    @Test fun categoryFilterDoesNotMixIdenticalNumbersAcrossStages() {
        rule.setContent { Cubo3x3Theme(darkTheme = false) {
            CfopComparisonScreen(listOf(answer("f"), answer("o", category = "OLL"),
                answer("p", category = "PLL")), AlgorithmCategory.F2L, {}, {})
        } }
        ready()
        rule.onNodeWithTag("comparison-filter-PLL").performClick()
        rule.onNodeWithTag("comparison-count").assertTextEquals("1 par em PLL")
        compare("PLL-1-2")
        rule.onNodeWithTag("comparison-title").assertTextEquals("PLL · Casos 1 e 2")
        rule.onNodeWithTag("comparison-formulas-toggle").performScrollTo()
        rule.onNodeWithTag("comparison-diagram-PLL-1").assertExists()
        rule.onNodeWithTag("comparison-diagram-PLL-2").assertExists()
        rule.onNodeWithTag("comparison-formula-PLL-1").assertDoesNotExist()
    }

    @Test fun pairFilterAndListScrollSurviveBackFromComparisonAndRecreation() {
        val rows = (1..20).map { answer("p-$it", it, it + 1, "PLL") }
        val restoration = StateRestorationTester(rule)
        restoration.setContent { Cubo3x3Theme(darkTheme = true) {
            CfopComparisonScreen(rows, AlgorithmCategory.F2L, {}, {})
        } }
        ready()
        rule.onNodeWithTag("comparison-filter-PLL").performClick()
        compare("PLL-16-17")
        rule.onNodeWithTag("comparison-back-list").performClick()
        ready("compare-PLL-16-17")
        rule.onNodeWithTag("compare-PLL-16-17").assertIsDisplayed()
        restoration.emulateSavedInstanceStateRestore()
        ready("compare-PLL-16-17")
        rule.onNodeWithTag("compare-PLL-16-17").assertIsDisplayed()
        rule.onNodeWithTag("compare-F2L-16-17").assertDoesNotExist()
    }

    @Test fun asynchronousHistoryRefreshKeepsTheDeepFilteredListPosition() {
        val rows = mutableStateOf((1..20).map { answer("p-$it", it, it + 1, "PLL") })
        rule.setContent { Cubo3x3Theme(darkTheme = true) {
            CfopComparisonScreen(rows.value, AlgorithmCategory.PLL, {}, {})
        } }
        ready()
        rule.onNodeWithTag("cfop-comparison-list")
            .performScrollToNode(hasTestTag("compare-PLL-16-17"))
        rule.onNodeWithTag("compare-PLL-16-17").assertIsDisplayed()
        rule.runOnIdle { rows.value += answer("new-oll-pair", category = "OLL", at = 100) }
        ready("compare-PLL-16-17")
        rule.onNodeWithTag("compare-PLL-16-17").assertIsDisplayed()
        rule.onNodeWithTag("compare-OLL-1-2").assertDoesNotExist()
        rule.runOnIdle { assertEquals(21, rows.value.size) }
    }

    @Test fun studyRoundTripAndRecreationKeepSelectedPairFormulasAndEnlargement() {
        val restoration = StateRestorationTester(rule)
        var openedId = ""
        restoration.setContent { Cubo3x3Theme(darkTheme = true) {
            var studying by rememberSaveable { mutableStateOf(false) }
            val holder = rememberSaveableStateHolder()
            if (studying) TextButton(onClick = { studying = false }) { Text("Voltar do estudo") }
            else holder.SaveableStateProvider("comparison") {
                CfopComparisonScreen(listOf(answer("a")), AlgorithmCategory.F2L,
                    { openedId = it.id; studying = true }, {})
            }
        } }
        compare("F2L-1-2")
        rule.onNodeWithTag("comparison-enlarge").performScrollTo().performClick()
        rule.onNodeWithTag("comparison-formulas-toggle").performScrollTo().performClick()
        rule.onNodeWithTag("comparison-study-F2L-2").performScrollTo().performClick()
        rule.runOnIdle { assertEquals("F2L-2", openedId) }
        rule.onNodeWithText("Voltar do estudo").performClick()
        rule.onNodeWithTag("comparison-study-F2L-2").assertIsDisplayed()
        restoration.emulateSavedInstanceStateRestore()
        rule.onNodeWithTag("comparison-study-F2L-2").assertIsDisplayed()
        rule.onNodeWithTag("comparison-formula-F2L-2").assertTextEquals(
            AlgorithmCatalog.entry(AlgorithmCategory.F2L, 2).notation)
        rule.onNodeWithTag("comparison-layout-stacked").assertExists()
        rule.onNodeWithTag("comparison-formulas-toggle").performScrollTo()
            .assertTextEquals("Ocultar fórmulas originais")
    }

    @Test fun liveHistoryCanRetirePairButCannotSilentlyReplaceTheOpenCases() {
        val rows = mutableStateOf(listOf(answer("wrong")))
        rule.setContent { Cubo3x3Theme(darkTheme = true) {
            CfopComparisonScreen(rows.value, AlgorithmCategory.F2L, {}, {})
        } }
        compare("F2L-1-2")
        rule.runOnIdle {
            rows.value += (1..10).map { answer("correct-$it", 1, 1, at = it.toLong()) } +
                answer("new-pair", 3, 4, at = 100)
        }
        rule.waitUntil(10_000) {
            rule.onAllNodesWithTag("comparison-retired").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithTag("comparison-title").assertTextEquals("F2L · Casos 1 e 2")
        rule.onNodeWithTag("comparison-back-list").performClick()
        ready()
        rule.onNodeWithTag("compare-F2L-3-4").performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("compare-F2L-1-2").assertDoesNotExist()
    }

    @Test fun enlargedSystemTextUsesVerticalComparisonAutomatically() {
        rule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.6f)) {
                Cubo3x3Theme(darkTheme = true) {
                    CfopComparisonScreen(listOf(answer("a")), AlgorithmCategory.F2L, {}, {})
                }
            }
        }
        compare("F2L-1-2")
        rule.onNodeWithTag("comparison-enlarge").performScrollTo()
        rule.onNodeWithTag("comparison-layout-stacked").assertExists()
        rule.onNodeWithTag("comparison-layout-side").assertDoesNotExist()
    }

    @Test fun returningToReviewPreservesItsModeCategoryAndDoesNotSaveAnAnswer() {
        var saves = 0
        rule.setContent { Cubo3x3Theme(darkTheme = true) {
            CfopReviewScreen(listOf(answer("o", 5, 6, "OLL")), { saves++ }, {}, clock = { 100 })
        } }
        rule.onNodeWithTag("review-mode-errors").performClick()
        rule.onNodeWithText("OLL").performClick()
        rule.onNodeWithTag("review-compare").performScrollTo().performClick()
        compare("OLL-5-6")
        rule.onNodeWithTag("comparison-back-list").performClick()
        rule.onNodeWithTag("comparison-back-review").performScrollTo().performClick()
        rule.onNodeWithTag("review-compare").assertIsDisplayed()
        rule.onNodeWithTag("review-mode-errors").performScrollTo().assertIsSelected()
        rule.onNodeWithTag("review-OLL-5").performScrollTo().assertIsDisplayed()
        rule.runOnIdle { assertEquals(0, saves) }
    }

    @Test fun backupHistoryRevisionInvalidatesSavedComparisonWithoutMutatingAttempts() {
        var revision by mutableLongStateOf(0L)
        val rows = listOf(answer("a"))
        rule.setContent { Cubo3x3Theme(darkTheme = true) {
            QuizHubScreen(emptyMap(), { _, _, _ -> }, rows, {}, {}, revision)
        } }
        rule.onNodeWithText("Revisão").performClick()
        rule.onNodeWithTag("review-compare").performScrollTo().performClick()
        compare("F2L-1-2")
        rule.runOnIdle { revision++ }
        rule.onNodeWithTag("cfop-comparison-detail").assertDoesNotExist()
        rule.onNodeWithText("Revisão").performClick()
        rule.onNodeWithTag("review-compare").performScrollTo().performClick()
        ready()
        rule.onNodeWithTag("cfop-comparison-list").assertIsDisplayed()
        rule.onNodeWithTag("cfop-comparison-detail").assertDoesNotExist()
        rule.runOnIdle { assertEquals(1, rows.size) }
    }
}
