package com.gabs.cubo3x3.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.semantics.SemanticsProperties
import com.gabs.cubo3x3.data.progress.AlgorithmProgress
import com.gabs.cubo3x3.ui.theme.Cubo3x3Theme
import com.gabs.cubo3x3.cube.CaseSolver
import com.gabs.cubo3x3.cube.CaseSolution
import com.gabs.cubo3x3.cube.CubeState
import kotlinx.coroutines.awaitCancellation
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Rule
import org.junit.Test

class CasePreparationScreenTest {
    @get:Rule val rule = createComposeRule()

    private fun open(calculate: suspend (String, CubeState) -> CaseSolution = CaseSolver::calculate) {
        rule.setContent {
            Cubo3x3Theme(darkTheme = true) {
                CasePreparationFlow(AlgorithmCatalog.entry(AlgorithmCategory.F2L, 1), {}, calculate)
            }
        }
    }
    private fun editor() {
        rule.onNodeWithText("Configurar meu cubo").performScrollTo().performClick()
    }
    private fun paintInvalid() {
        rule.onNodeWithText("Vermelho").performScrollTo().performClick()
        rule.onNodeWithContentDescription("Face U adesivo 1: Amarelo")
            .performScrollTo().performClick()
    }

    private fun checkInlinePreparation(category: AlgorithmCategory, number: Int) {
        val entry = AlgorithmCatalog.entry(category, number)
        val preparation = AlgorithmCatalog.setupNotation(entry)
        rule.setContent {
            Cubo3x3Theme(darkTheme = true) {
                AlgorithmDetailScreen(category, number, AlgorithmProgress(),
                    { _, _ -> }, { _, _ -> }, {})
            }
        }
        rule.onNodeWithText("Resolução · caso → resolvido").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Preparação · resolvido → caso").performScrollTo().assertIsDisplayed()
        rule.onNodeWithContentDescription("Fórmula de preparação de ${entry.id}")
            .performScrollTo().assertTextEquals(preparation)
        rule.onNodeWithText("Copiar preparação").performScrollTo().performClick()
        rule.onNodeWithText("Preparação copiada").assertIsDisplayed()
        rule.runOnIdle {
            val context = androidx.test.platform.app.InstrumentationRegistry
                .getInstrumentation().targetContext
            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE)
                as android.content.ClipboardManager
            check(clipboard.primaryClip?.getItemAt(0)?.text?.toString() == preparation)
        }
        rule.onNodeWithText("0/" + com.gabs.cubo3x3.cube.MoveNotation
            .parseAlgorithm(entry.notation).size).assertExists()
        rule.onNodeWithText("Preparar este caso").performScrollTo().performClick()
        rule.onNodeWithText("Preparar desde resolvido").assertIsDisplayed()
        rule.onNodeWithText("Preparação · resolvido → caso").assertDoesNotExist()
        rule.onNodeWithText(preparation).performScrollTo().assertIsDisplayed()
    }

    @Test fun f2lShowsBothDirectionsAndCopiesPreparation() {
        checkInlinePreparation(AlgorithmCategory.F2L, 1)
    }

    @Test fun ollShowsBothDirectionsIncludingOrientation() {
        checkInlinePreparation(AlgorithmCategory.OLL, 2)
    }

    @Test fun pllShowsBothDirectionsAndKeepsPrimedDoubles() {
        checkInlinePreparation(AlgorithmCategory.PLL, 9)
    }

    @Test fun preparationHasItsOwnSequenceCopyAndAnimatedSteps() {
        open()
        rule.onNodeWithText("Preparar desde resolvido").assertIsDisplayed()
        rule.onNodeWithText("R U' R'").performScrollTo().assertIsDisplayed()
        rule.onNodeWithContentDescription("Próximo movimento").performScrollTo().performClick()
        rule.onNodeWithText("1/3").assertIsDisplayed()
        rule.onNodeWithContentDescription("Movimento anterior").performScrollTo().performClick()
        rule.onNodeWithText("0/3").assertIsDisplayed()
        rule.onNodeWithText("Copiar sequência").performScrollTo().performClick()
        rule.onNodeWithText("Sequência copiada").assertIsDisplayed()
    }

    @Test fun paintingValidatesCountsAndKeepsDraftAcrossBackNavigation() {
        open(); editor(); paintInvalid()
        rule.onNodeWithText("Calcular até este caso").performScrollTo().assertIsNotEnabled()
        rule.onNodeWithText("Cada cor precisa aparecer exatamente 9 vezes. Confira a pintura.")
            .performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("‹ Voltar").performClick()
        editor()
        rule.onNodeWithContentDescription("Face U adesivo 1: Vermelho")
            .performScrollTo().assertIsDisplayed()
        rule.onNodeWithContentDescription("Face U adesivo 5: Amarelo, centro fixo")
            .assertIsNotEnabled()
    }

    @Test fun liveCountsBlockSolverAndCorrectionReenablesCalculation() {
        val calculations = AtomicInteger()
        open { source, target ->
            calculations.incrementAndGet()
            CaseSolver.calculate(source, target)
        }
        editor(); paintInvalid()
        rule.onNodeWithText("Amarelo: 8/9 · falta 1").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Vermelho: 10/9 · sobra 1").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Calcular até este caso").performScrollTo().assertIsNotEnabled()
        rule.runOnIdle { check(calculations.get() == 0) }
        rule.onNodeWithText("Amarelo").performScrollTo().performClick()
        rule.onNodeWithContentDescription("Face U adesivo 1: Vermelho")
            .performScrollTo().performClick()
        rule.onNodeWithText("Configuração válida · pronta para calcular")
            .performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Calcular até este caso").performScrollTo().assertIsEnabled()
    }

    @Test fun incompatiblePiecesOfferReviewFacesAndAccessibleMarks() {
        open(); editor()
        rule.onNodeWithText("Vermelho").performScrollTo().performClick()
        rule.onNodeWithContentDescription("Face U adesivo 8: Amarelo")
            .performScrollTo().performClick()
        rule.onNodeWithText("F · Frente").performScrollTo().performClick()
        rule.onNodeWithText("Amarelo").performScrollTo().performClick()
        rule.onNodeWithContentDescription("Face F adesivo 6: Vermelho")
            .performScrollTo().performClick()
        rule.onNodeWithText("Há arestas repetidas ou com cores incompatíveis. Confira as seis faces.")
            .performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Revisar face U · Acima").performScrollTo().performClick()
        rule.onNodeWithContentDescription("Face U adesivo 8: Vermelho").assertIsDisplayed()
        rule.onNodeWithText("U · Acima").performScrollTo().assertIsSelected()
        rule.onNodeWithContentDescription("Face U adesivo 8: Vermelho").performScrollTo()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Peça para revisar"))
        rule.onNodeWithText("Calcular até este caso").performScrollTo().assertIsNotEnabled()
    }

    @Test fun paintingResetNeedsConfirmation() {
        open(); editor(); paintInvalid()
        rule.onNodeWithText("Voltar à pintura resolvida").performScrollTo().performClick()
        rule.onNodeWithText("Cancelar").performClick()
        rule.onNodeWithContentDescription("Face U adesivo 1: Vermelho")
            .performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Voltar à pintura resolvida").performScrollTo().performClick()
        rule.onNodeWithText("Reiniciar", useUnmergedTree = true).performClick()
        rule.onNodeWithContentDescription("Face U adesivo 1: Amarelo")
            .performScrollTo().assertIsDisplayed()
    }

    @Test fun solvedPaintingProducesVerifiedResultAndReturnsToEditor() {
        open(); editor()
        rule.onNodeWithText("Calcular até este caso").performScrollTo().performClick()
        rule.waitUntil(timeoutMillis = 20_000) {
            rule.onAllNodesWithText("Do meu cubo ao caso").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("Do meu cubo ao caso").assertIsDisplayed()
        rule.onNodeWithContentDescription("Próximo movimento").performScrollTo().performClick()
        rule.onNodeWithContentDescription("Reiniciar algoritmo").performScrollTo().performClick()
        rule.onNodeWithText("‹ Voltar").performClick()
        rule.onNodeWithText("Calcular até este caso").performScrollTo().assertIsDisplayed()
    }

    @Test fun cancelAndBackDiscardPendingCalculationAndKeepPainting() {
        val cancelled = AtomicInteger()
        open { _, _ ->
            try { awaitCancellation() } finally { cancelled.incrementAndGet() }
        }
        editor()
        rule.onNodeWithText("Calcular até este caso").performScrollTo().performClick()
        rule.onNodeWithContentDescription("Face U adesivo 1: Amarelo")
            .performScrollTo().assertIsNotEnabled()
        rule.onNodeWithText("Cancelar cálculo").performScrollTo().performClick()
        rule.waitUntil(5_000) { cancelled.get() == 1 }
        rule.onNodeWithText("Cálculo cancelado. A pintura foi mantida.")
            .performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Do meu cubo ao caso").assertDoesNotExist()
        rule.onNodeWithText("Calcular até este caso").performScrollTo().performClick()
        rule.onNodeWithText("‹ Voltar").performClick()
        rule.waitUntil(5_000) { cancelled.get() == 2 }
        editor()
        rule.onNodeWithContentDescription("Face U adesivo 1: Amarelo")
            .performScrollTo().assertIsDisplayed()
    }

    @Test fun calculationFailureKeepsEditorUsableForRetry() {
        val attempts = AtomicInteger()
        open { source, target ->
            if (attempts.incrementAndGet() == 1) error("Falha simulada de cálculo")
            CaseSolver.calculate(source, target)
        }
        editor()
        rule.onNodeWithText("R · Direita").performScrollTo().performClick()
        rule.onNodeWithText("Azul").performScrollTo().performClick()
        rule.onNodeWithText("Calcular até este caso").performScrollTo().performClick()
        rule.onNodeWithText("Falha simulada de cálculo").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Calcular até este caso").performScrollTo().assertIsEnabled().performClick()
        rule.waitUntil(timeoutMillis = 20_000) {
            rule.onAllNodesWithText("Do meu cubo ao caso").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("‹ Voltar").performClick()
        rule.onNodeWithText("R · Direita").performScrollTo().assertIsSelected()
        rule.onNodeWithText("Azul").performScrollTo().assertIsSelected()
        rule.runOnIdle { check(attempts.get() == 2) }
    }

    @Test fun restorationDuringCalculationKeepsEditorAndDoesNotResumeSearch() {
        val started = AtomicInteger()
        val cancelled = AtomicInteger()
        val restoration = StateRestorationTester(rule)
        restoration.setContent {
            Cubo3x3Theme(darkTheme = true) {
                CasePreparationFlow(AlgorithmCatalog.entry(AlgorithmCategory.F2L, 1), {},
                    calculate = { _, _ ->
                        started.incrementAndGet()
                        try { awaitCancellation() } finally { cancelled.incrementAndGet() }
                    })
            }
        }
        editor()
        rule.onNodeWithText("R · Direita").performScrollTo().performClick()
        rule.onNodeWithText("Azul").performScrollTo().performClick()
        rule.onNodeWithText("Calcular até este caso").performScrollTo().performClick()
        rule.waitUntil(5_000) { started.get() == 1 }
        restoration.emulateSavedInstanceStateRestore()
        rule.waitUntil(5_000) { cancelled.get() == 1 }
        rule.onNodeWithText("Calcular até este caso").performScrollTo().assertIsEnabled()
        rule.onNodeWithText("Cancelar cálculo").assertDoesNotExist()
        rule.onNodeWithText("R · Direita").performScrollTo().assertIsSelected()
        rule.onNodeWithText("Azul").performScrollTo().assertIsSelected()
        rule.onNodeWithContentDescription("Face R adesivo 1: Verde").performScrollTo().assertIsDisplayed()
        rule.runOnIdle { check(started.get() == 1) }
    }

    @Test fun undoRedoPaintingFollowsChangedFaceAndKeepsPalette() {
        open(); editor(); paintInvalid()
        rule.onNodeWithText("R · Direita").performScrollTo().performClick()
        rule.onNodeWithText("Azul").performScrollTo().performClick()
        rule.onNodeWithContentDescription("Face R adesivo 1: Verde").performScrollTo().performClick()
        rule.onNodeWithText("Desfazer pintura").performScrollTo().performClick()
        rule.onNodeWithContentDescription("Face R adesivo 1: Verde").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Desfazer pintura").performScrollTo().performClick()
        rule.onNodeWithText("U · Acima").performScrollTo().assertIsSelected()
        rule.onNodeWithText("Azul").performScrollTo().assertIsSelected()
        rule.onNodeWithContentDescription("Face U adesivo 1: Amarelo").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Refazer pintura").performScrollTo().performClick()
        rule.onNodeWithContentDescription("Face U adesivo 1: Vermelho").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Refazer pintura").performScrollTo().performClick()
        rule.onNodeWithText("R · Direita").performScrollTo().assertIsSelected()
        rule.onNodeWithContentDescription("Face R adesivo 1: Azul").performScrollTo().assertIsDisplayed()
    }

    @Test fun undoRedoPaintingSurvivesResultAndSavedStateRestoration() {
        val restoration = StateRestorationTester(rule)
        restoredFlow(restoration)
        editor(); paintInvalid()
        rule.onNodeWithText("Amarelo").performScrollTo().performClick()
        rule.onNodeWithContentDescription("Face U adesivo 1: Vermelho").performScrollTo().performClick()
        rule.onNodeWithText("Calcular até este caso").performScrollTo().performClick()
        rule.waitUntil(20_000) {
            rule.onAllNodesWithText("Do meu cubo ao caso").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("‹ Voltar").performClick()
        rule.onNodeWithText("Desfazer pintura").performScrollTo().performClick()
        restoration.emulateSavedInstanceStateRestore()
        rule.onNodeWithContentDescription("Face U adesivo 1: Vermelho").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Calcular até este caso").performScrollTo().assertIsNotEnabled()
        rule.onNodeWithText("Refazer pintura").performScrollTo().assertIsEnabled().performClick()
        rule.onNodeWithContentDescription("Face U adesivo 1: Amarelo").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Calcular até este caso").performScrollTo().assertIsEnabled()
    }

    @Test fun undoableResetAndBothHistoryActionsAreLockedWhileCalculating() {
        open { _, _ -> awaitCancellation() }
        editor(); paintInvalid()
        rule.onNodeWithText("Voltar à pintura resolvida").performScrollTo().performClick()
        rule.onNodeWithText("Cancelar").performClick()
        rule.onNodeWithContentDescription("Face U adesivo 1: Vermelho").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Voltar à pintura resolvida").performScrollTo().performClick()
        rule.onNodeWithText("Reiniciar", useUnmergedTree = true).performClick()
        rule.onNodeWithText("Desfazer pintura").performScrollTo().performClick()
        rule.onNodeWithContentDescription("Face U adesivo 1: Vermelho").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Refazer pintura").performScrollTo().performClick()
        // Cria uma ramificacao pendente mantendo o estado atual legal.
        paintInvalid()
        rule.onNodeWithText("Desfazer pintura").performScrollTo().performClick()
        rule.onNodeWithText("Desfazer pintura").assertIsEnabled()
        rule.onNodeWithText("Refazer pintura").assertIsEnabled()
        rule.onNodeWithText("Calcular até este caso").performScrollTo().performClick()
        rule.onNodeWithText("Desfazer pintura").performScrollTo().assertIsNotEnabled()
        rule.onNodeWithText("Refazer pintura").assertIsNotEnabled()
        rule.onNodeWithText("Cancelar cálculo").performScrollTo().performClick()
        rule.onNodeWithText("Desfazer pintura").performScrollTo().assertIsEnabled()
        rule.onNodeWithText("Refazer pintura").assertIsEnabled()
    }

    private fun restoredFlow(restoration: StateRestorationTester, disposals: AtomicInteger? = null) {
        restoration.setContent {
            Cubo3x3Theme(darkTheme = true) {
                DisposableEffect(Unit) { onDispose { disposals?.incrementAndGet() } }
                CasePreparationFlow(AlgorithmCatalog.entry(AlgorithmCategory.F2L, 1), {})
            }
        }
    }

    @Test fun restorationKeepsCompletedPreparationStepPaused() {
        val restoration = StateRestorationTester(rule)
        restoredFlow(restoration)
        rule.onNodeWithContentDescription("Próximo movimento").performScrollTo().performClick()
        rule.onNodeWithText("1/3").assertIsDisplayed()
        restoration.emulateSavedInstanceStateRestore()
        rule.onNodeWithText("1/3").performScrollTo().assertIsDisplayed()
        rule.onNodeWithContentDescription("Reproduzir algoritmo").performScrollTo().assertIsEnabled()
        rule.onNodeWithContentDescription("Próximo movimento").performClick()
        rule.onNodeWithText("2/3").assertIsDisplayed()
    }

    @Test fun interruptedForwardAndBackwardRestoreOnlyWholeCompletedSteps() {
        // Com o relogio pausado, separa salvar/descartar/restaurar por frames.
        // Sem isso, false/true de StateRestorationTester podem ser agrupados
        // antes da recomposicao e a tela antiga nunca chega a ser descartada.
        val restoration = StateRestorationTester(object : ComposeContentTestRule by rule {
            override fun <T> runOnIdle(action: () -> T): T {
                val result = rule.runOnIdle(action)
                if (!rule.mainClock.autoAdvance) {
                    rule.mainClock.advanceTimeByFrame()
                    rule.waitForIdle()
                }
                return result
            }
        })
        val disposals = AtomicInteger()
        restoredFlow(restoration, disposals)
        rule.onNodeWithContentDescription("Próximo movimento").performScrollTo()
        rule.mainClock.autoAdvance = false
        rule.onNodeWithContentDescription("Próximo movimento").performClick()
        rule.mainClock.advanceTimeBy(160)
        rule.onNodeWithText("0/3").assertExists()
        rule.onNodeWithContentDescription("Próximo movimento").assertIsNotEnabled()
        restoration.emulateSavedInstanceStateRestore()
        rule.runOnIdle { check(disposals.get() == 1) { "Restoration did not dispose the paused content: ${disposals.get()}" } }
        rule.mainClock.autoAdvance = true
        rule.onNodeWithText("0/3").performScrollTo().assertIsDisplayed()
        rule.onNodeWithContentDescription("Próximo movimento").performScrollTo().performClick()
        rule.onNodeWithText("1/3").assertIsDisplayed()
        rule.onNodeWithContentDescription("Movimento anterior").performScrollTo()
        rule.mainClock.autoAdvance = false
        rule.onNodeWithContentDescription("Movimento anterior").performClick()
        rule.mainClock.advanceTimeBy(160)
        rule.onNodeWithText("1/3").assertExists()
        rule.onNodeWithContentDescription("Movimento anterior").assertIsNotEnabled()
        restoration.emulateSavedInstanceStateRestore()
        rule.runOnIdle { check(disposals.get() == 2) { "Restoration did not dispose the paused content: ${disposals.get()}" } }
        rule.mainClock.autoAdvance = true
        rule.onNodeWithText("1/3").performScrollTo().assertIsDisplayed()
        rule.onNodeWithContentDescription("Movimento anterior").performScrollTo().performClick()
        rule.onNodeWithText("0/3").assertIsDisplayed()
    }

    @Test fun editorKeepsFaceAndPaletteAfterResultAndSavedStateRestore() {
        val restoration = StateRestorationTester(rule)
        restoredFlow(restoration)
        editor()
        rule.onNodeWithText("R · Direita").performScrollTo().performClick()
        rule.onNodeWithText("Azul").performScrollTo().performClick()
        rule.onNodeWithText("Calcular até este caso").performScrollTo().performClick()
        rule.waitUntil(timeoutMillis = 20_000) {
            rule.onAllNodesWithText("Do meu cubo ao caso").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("‹ Voltar").performClick()
        rule.onNodeWithText("R · Direita").performScrollTo().assertIsSelected()
        rule.onNodeWithText("Azul").performScrollTo().assertIsSelected()
        restoration.emulateSavedInstanceStateRestore()
        rule.onNodeWithText("R · Direita").performScrollTo().assertIsSelected()
        rule.onNodeWithText("Azul").performScrollTo().assertIsSelected()
    }

    @Test fun formulaAndPreparationKeepSeparateStepsWhenSwitchingWithinCase() {
        rule.setContent {
            Cubo3x3Theme(darkTheme = true) {
                AlgorithmDetailScreen(AlgorithmCategory.F2L, 1, AlgorithmProgress(),
                    { _, _ -> }, { _, _ -> }, {})
            }
        }
        rule.onNodeWithContentDescription("Próximo movimento").performScrollTo().performClick()
        rule.onNodeWithText("1/3").assertIsDisplayed()
        rule.onNodeWithText("Preparar este caso").performScrollTo().performClick()
        rule.onNodeWithText("0/3").performScrollTo().assertIsDisplayed()
        rule.onNodeWithContentDescription("Próximo movimento").performScrollTo().performClick()
        rule.onNodeWithText("1/3").assertIsDisplayed()
        rule.onNodeWithText("Voltar à fórmula do caso").performScrollTo().performClick()
        rule.onNodeWithText("Detalhe do algoritmo").assertIsDisplayed()
        rule.onNodeWithText("1/3").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Preparar este caso").performScrollTo().performClick()
        rule.onNodeWithText("Preparar desde resolvido").assertIsDisplayed()
        rule.onNodeWithText("1/3").performScrollTo().assertIsDisplayed()
    }
}
