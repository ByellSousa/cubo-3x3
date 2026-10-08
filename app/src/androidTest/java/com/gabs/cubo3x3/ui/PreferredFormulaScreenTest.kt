package com.gabs.cubo3x3.ui

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.gabs.cubo3x3.data.progress.AlgorithmProgress
import com.gabs.cubo3x3.domain.algorithm.PreferredCaseFormula
import com.gabs.cubo3x3.ui.theme.Cubo3x3Theme
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Fixtures only; no real preference/database access. */
class PreferredFormulaScreenTest {
    @get:Rule val rule = createComposeRule()
    private val entry = AlgorithmCatalog.entry(AlgorithmCategory.F2L, 1)
    private val personal = PreferredCaseFormula("F2L", 1, "R U R' U2'", 900)
    private fun waitFor(tag: String) = rule.waitUntil(10_000) {
        rule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
    }
    private fun checkFormula() {
        rule.onNodeWithTag("preferred-formula-check").performClick()
        waitFor("preferred-formula-valid")
    }

    @Test fun validPersonalFormulaIsCheckedBeforeSavingAndPreservesDoublePrime() {
        val saved = AtomicReference<String?>(null)
        rule.setContent { Cubo3x3Theme(darkTheme = true) {
            PreferredFormulaDialog(entry, null, { saved.set(it) }, {})
        } }
        rule.onNodeWithTag("preferred-formula-save").assertIsNotEnabled()
        rule.onNodeWithTag("preferred-formula-input").performTextReplacement(personal.notation)
        checkFormula()
        rule.onNodeWithTag("preferred-formula-save").performClick()
        rule.waitUntil(10_000) { saved.get() != null }
        assertEquals(personal.notation, saved.get())
    }

    @Test fun wrongFormulaCannotBeSavedDespiteValidMoveSyntax() {
        val saved = AtomicReference<String?>(null)
        rule.setContent { Cubo3x3Theme(darkTheme = false) {
            PreferredFormulaDialog(entry, null, { saved.set(it) }, {})
        } }
        rule.onNodeWithTag("preferred-formula-input").performTextReplacement("U")
        rule.onNodeWithTag("preferred-formula-check").performClick()
        waitFor("preferred-formula-message")
        rule.onNodeWithTag("preferred-formula-save").assertIsNotEnabled()
        rule.runOnIdle { assertNull(saved.get()) }
    }

    @Test fun editingAfterVerificationRequiresFreshCheck() {
        rule.setContent { Cubo3x3Theme(darkTheme = true) {
            PreferredFormulaDialog(entry, null, {}, {})
        } }
        checkFormula()
        rule.onNodeWithTag("preferred-formula-save").assertIsEnabled()
        rule.onNodeWithTag("preferred-formula-input").performTextReplacement("R U R' U")
        rule.onNodeWithTag("preferred-formula-valid").assertDoesNotExist()
        rule.onNodeWithTag("preferred-formula-save").assertIsNotEnabled()
    }

    @Test fun draftRestoresButMustBeCheckedAgainAndDiscardIsConfirmed() {
        val closed = AtomicReference(false)
        val restoration = StateRestorationTester(rule)
        restoration.setContent { Cubo3x3Theme(darkTheme = true) {
            PreferredFormulaDialog(entry, null, {}, { closed.set(true) })
        } }
        rule.onNodeWithTag("preferred-formula-input").performTextReplacement(personal.notation)
        checkFormula()
        restoration.emulateSavedInstanceStateRestore()
        rule.onNodeWithTag("preferred-formula-input").assertTextContains(personal.notation)
        rule.onNodeWithTag("preferred-formula-save").assertIsNotEnabled()
        rule.onNodeWithText("Cancelar").performClick()
        rule.onNodeWithText("Descartar alterações?").assertIsDisplayed()
        rule.onNodeWithText("Continuar editando").performClick()
        rule.runOnIdle { assertFalse(closed.get()) }
        rule.onNodeWithText("Cancelar").performClick()
        rule.onNodeWithText("Descartar", substring = false).performClick()
        rule.runOnIdle { assertTrue(closed.get()) }
    }

    @Test fun preferredPlaybackSwitchesToOriginalAndResetsAtCanonicalCase() {
        rule.setContent { Cubo3x3Theme(darkTheme = true) {
            AlgorithmDetailScreen(entry.category, entry.number, AlgorithmProgress(), { _, _ -> },
                { _, _ -> }, {}, preferredFormula = personal, onPreferredFormulaChange = { _, _ -> })
        } }
        rule.onNodeWithTag("case-active-formula").performScrollTo().assertTextEquals(personal.notation)
        rule.onNodeWithContentDescription("Próximo movimento").performScrollTo().performClick()
        rule.mainClock.advanceTimeBy(1_000)
        rule.onNodeWithText("1/4").performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("formula-original").performScrollTo().performClick()
        rule.onNodeWithTag("case-active-formula").assertTextEquals(entry.notation)
        rule.onNodeWithText("0/3").performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("formula-preferred").performScrollTo().performClick()
        rule.onNodeWithText("0/4").performScrollTo().assertIsDisplayed()
        rule.onNodeWithContentDescription("Fórmula de preparação de F2L-1")
            .performScrollTo().assertTextEquals(AlgorithmCatalog.setupNotation(entry))
    }

    @Test fun personalInverseIsLabeledAsReturnFromResultNotSolvedSetup() {
        rule.setContent { Cubo3x3Theme(darkTheme = false) {
            AlgorithmDetailScreen(entry.category, entry.number, AlgorithmProgress(), { _, _ -> },
                { _, _ -> }, {}, preferredFormula = personal)
        } }
        rule.onNodeWithTag("formula-personal-inverse").performScrollTo().assertTextEquals("U2' R U' R'")
        rule.onNodeWithText("Voltar ao caso · resultado → caso").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Não é necessariamente uma preparação", substring = true)
            .performScrollTo().assertIsDisplayed()
    }

    @Test fun switchingResetsEvenWhenPreferredNotationMatchesOriginal() {
        rule.setContent { Cubo3x3Theme(darkTheme = true) {
            AlgorithmDetailScreen(entry.category, entry.number, AlgorithmProgress(), { _, _ -> },
                { _, _ -> }, {}, preferredFormula = personal.copy(notation = entry.notation))
        } }
        rule.onNodeWithContentDescription("Próximo movimento").performScrollTo().performClick()
        rule.mainClock.advanceTimeBy(1_000)
        rule.onNodeWithText("1/3").performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("formula-original").performScrollTo().performClick()
        rule.onNodeWithText("0/3").performScrollTo().assertIsDisplayed()
    }

    @Test fun removalIsConfirmedAndDoesNotChangeStudyMarks() {
        val current = mutableStateOf<PreferredCaseFormula?>(personal)
        rule.setContent { Cubo3x3Theme(darkTheme = true) {
            AlgorithmDetailScreen(entry.category, entry.number,
                AlgorithmProgress(isFavorite = true, isCompleted = true), { _, _ -> error("Marca alterada") },
                { _, _ -> error("Marca alterada") }, {}, preferredFormula = current.value,
                onPreferredFormulaChange = { _, value ->
                    check(value == null); current.value = null
                })
        } }
        rule.onNodeWithTag("formula-remove").performScrollTo().performClick()
        rule.onNodeWithText("Cancelar").performClick()
        rule.runOnIdle { assertNotNull(current.value) }
        rule.onNodeWithTag("formula-remove").performClick()
        rule.onNodeWithTag("formula-confirm-remove").performClick()
        rule.waitUntil(10_000) { current.value == null }
        rule.onNodeWithTag("case-active-formula").performScrollTo().assertTextEquals(entry.notation)
        rule.onNodeWithText("★ Favorito").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("✓ Concluído").assertIsDisplayed()
    }

    @Test fun saveFailureIsShownWithoutClosingOrReplacingPreference() {
        val closed = AtomicReference(false)
        rule.setContent { Cubo3x3Theme(darkTheme = true) {
            PreferredFormulaDialog(entry, personal.notation, { error("Falha simulada") },
                { closed.set(true) })
        } }
        checkFormula()
        rule.onNodeWithTag("preferred-formula-save").performClick()
        waitFor("preferred-formula-message")
        rule.onNodeWithTag("preferred-formula-message").assertTextEquals("Falha simulada")
        rule.runOnIdle { assertFalse(closed.get()) }
    }
}
