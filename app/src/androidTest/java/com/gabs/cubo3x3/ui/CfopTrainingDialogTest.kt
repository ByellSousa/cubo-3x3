package com.gabs.cubo3x3.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.gabs.cubo3x3.data.progress.AlgorithmProgress
import com.gabs.cubo3x3.domain.timer.CfopTrainingMode
import com.gabs.cubo3x3.domain.timer.CfopTrainingPlan
import com.gabs.cubo3x3.ui.theme.Cubo3x3Theme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class CfopTrainingDialogTest {
    @get:Rule val rule = createComposeRule()
    private var applied: CfopTrainingPlan? = null
    private val completed = CfopTrainingPlan(mode = CfopTrainingMode.COMPLETED)

    @Test fun emptyPoolNeedsCompletedMarksOrNewCases() {
        rule.setContent { Cubo3x3Theme(darkTheme = true) {
            CfopTrainingDialog(completed, emptyMap(), {}, { applied = it })
        } }
        rule.onNodeWithTag("training-apply").assertIsNotEnabled()
        rule.onNodeWithTag("training-case-F2L-2").performScrollTo().performClick()
        rule.onNodeWithTag("training-selection-summary").performScrollTo()
            .assertTextEquals("0 concluídos + 1 novo · 1 caso")
        rule.onNodeWithTag("training-apply").performClick()
        assertEquals(setOf("F2L-2"), applied?.extraCaseIds)
    }

    @Test fun combinesCompletedAndExtrasAndFiltersCategories() {
        val progress = mapOf(
            "F2L-1" to AlgorithmProgress(isCompleted = true),
            "OLL-1" to AlgorithmProgress(isCompleted = true),
        )
        rule.setContent { Cubo3x3Theme(darkTheme = false) {
            CfopTrainingDialog(completed, progress, {}, { applied = it })
        } }
        rule.onNodeWithTag("training-category-OLL").performScrollTo().performClick()
        rule.onNodeWithTag("training-category-PLL").performClick()
        rule.onNodeWithTag("training-case-F2L-2").performScrollTo().performClick()
        rule.onNodeWithTag("training-selection-summary").performScrollTo()
            .assertTextEquals("1 concluído + 1 novo · 2 casos")
        rule.onNodeWithTag("training-apply").performClick()
        assertEquals(setOf(AlgorithmCategory.F2L), applied?.completedCategories)
        assertEquals(listOf("F2L-1", "F2L-2"), applied?.entries(progress)?.map { it.id })
        assertTrue(progress.getValue("F2L-1").isCompleted)
        assertFalse("F2L-2" in progress)
    }

    @Test fun recreationPreservesSelectionAndOnlyApplyCommits() {
        val restoration = StateRestorationTester(rule)
        restoration.setContent { Cubo3x3Theme(darkTheme = true) {
            CfopTrainingDialog(completed, emptyMap(), {}, { applied = it })
        } }
        rule.onNodeWithTag("training-case-F2L-2").performScrollTo().performClick()
        restoration.emulateSavedInstanceStateRestore()
        rule.onNodeWithTag("training-case-F2L-2").performScrollTo().assertIsSelected()
        assertNull(applied)
        rule.onNodeWithTag("training-apply").performClick()
        assertEquals(setOf("F2L-2"), applied?.extraCaseIds)
    }

    @Test fun noSelectedCategoriesIsDisabledAndCancelDoesNotApply() {
        var dismissed = false
        rule.setContent { Cubo3x3Theme(darkTheme = true) {
            CfopTrainingDialog(completed.copy(extraCaseIds = setOf("F2L-1")),
                emptyMap(), { dismissed = true }, { applied = it })
        } }
        AlgorithmCategory.entries.forEach {
            rule.onNodeWithTag("training-category-${it.name}").performScrollTo().performClick()
        }
        rule.onNodeWithTag("training-apply").assertIsNotEnabled()
        rule.onNodeWithText("Cancelar").performClick()
        assertTrue(dismissed)
        assertNull(applied)
    }

    @Test fun specificCaseAndFreeModesRemainAvailable() {
        rule.setContent { Cubo3x3Theme(darkTheme = false) {
            CfopTrainingDialog(CfopTrainingPlan(), emptyMap(), {}, { applied = it })
        } }
        rule.onNodeWithTag("training-mode-CASE").performScrollTo().performClick()
        rule.onNodeWithText("PLL").performClick()
        rule.onNodeWithTag("training-case-PLL-21").performScrollTo().performClick()
        rule.onNodeWithTag("training-apply").performClick()
        assertEquals(CfopTrainingPlan(CfopTrainingMode.CASE, AlgorithmCategory.PLL, 21), applied)
        rule.onNodeWithTag("training-mode-FREE").performScrollTo().performClick()
        rule.onNodeWithTag("training-apply").performClick()
        assertEquals(CfopTrainingPlan(), applied)
    }
}
