package com.gabs.cubo3x3.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.gabs.cubo3x3.data.timer.SolvePenalty
import com.gabs.cubo3x3.data.timer.SolveTime
import com.gabs.cubo3x3.ui.theme.Cubo3x3Theme
import org.junit.Rule
import org.junit.Test

class CfopExecutionPanelTest {
    @get:Rule val rule = createComposeRule()
    private fun solve(id: Long, category: String, case: Int,
        penalty: SolvePenalty = SolvePenalty.NONE) =
        SolveTime(id, 1_000, id, "", "", penalty,
            trainingCategory = category, trainingCaseNumber = case)

    @Test fun filtersCasesAndShowsCorrectMeanPlusTwoAndEvolution() {
        rule.setContent { Cubo3x3Theme(darkTheme = true) {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                CfopPerformancePanel(listOf(solve(1, "F2L", 1),
                    solve(2, "F2L", 1, SolvePenalty.PLUS_TWO), solve(3, "OLL", 1)))
            }
        } }
        rule.onNodeWithTag("execution-show-cases").performScrollTo().performClick()
        rule.onNodeWithText("Melhor: 1.00 · Média válida: 2.00").performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("execution-evolution-F2L-1").performScrollTo().performClick()
        rule.onNodeWithContentDescription("Evolução da execução F2L caso 1").performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("execution-category-OLL").performScrollTo().performClick()
        rule.onNodeWithTag("execution-case-F2L-1").assertDoesNotExist()
        rule.onNodeWithTag("execution-case-OLL-1").performScrollTo().assertIsDisplayed()
    }

    @Test fun allDnfCaseHasNoInventedGraphOrMean() {
        rule.setContent { Cubo3x3Theme(darkTheme = false) {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                CfopPerformancePanel(listOf(solve(1, "PLL", 1, SolvePenalty.DNF)))
            }
        } }
        rule.onNodeWithTag("execution-show-cases").performScrollTo().performClick()
        rule.onNodeWithText("Melhor: DNF · Média válida: DNF").performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("execution-evolution-PLL-1").performScrollTo().performClick()
        rule.onNodeWithText("Ainda não há tempos válidos neste caso.").performScrollTo().assertIsDisplayed()
    }
}
