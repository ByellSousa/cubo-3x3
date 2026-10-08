package com.gabs.cubo3x3.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertTrue
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performImeAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gabs.cubo3x3.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTestApi::class)
class Cubo3x3AppTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun navigatesToMoreAndChangesTheme() {
        composeRule.waitUntilAtLeastOneExists(hasText("Mais"), timeoutMillis = 10_000)
        composeRule.onNodeWithText("Mais").performClick()
        composeRule.onNodeWithText("Aparência").assertIsDisplayed()
        composeRule.onNodeWithText("Escuro").performClick()
        composeRule.waitUntilAtLeastOneExists(
            hasText("Escuro") and isSelected(),
            timeoutMillis = 10_000,
        )
        composeRule.onNodeWithText("Escuro").assertIsSelected()
    }

    @Test
    fun opensApprovedCubePrototypeFromHome() {
        composeRule.waitUntilAtLeastOneExists(
            hasText("Abrir protótipo do cubo  ›"),
            timeoutMillis = 10_000,
        )
        composeRule.onNodeWithText("Abrir protótipo do cubo  ›").performClick()
        composeRule.onNodeWithText("Protótipo do cubo").assertIsDisplayed()
        composeRule.onNodeWithText("Reproduzir").assertIsDisplayed()
    }

    @Test
    fun opensF2lCatalogAndAlgorithmDetail() {
        composeRule.waitUntilAtLeastOneExists(hasText("Algoritmos"), timeoutMillis = 10_000)
        composeRule.onNodeWithText("Algoritmos").performClick()
        composeRule.onNodeWithContentDescription("Abrir F2L").performClick()
        composeRule.onNodeWithText("41 casos únicos").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Abrir F2L caso 1").performClick()
        composeRule.onNodeWithText("F2L 1").assertIsDisplayed()
        composeRule.onNodeWithText("R U R'").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Cubo 3x3 animado").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Reproduzir algoritmo").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Próximo movimento").performClick()
        composeRule.onNodeWithText("1/3").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Movimento anterior").performClick()
        composeRule.onNodeWithText("0/3").assertIsDisplayed()
    }

    @Test
    fun opensF2lCatalogDirectlyFromHomeShortcut() {
        composeRule.waitUntilAtLeastOneExists(
            hasContentDescription("Abrir F2L"),
            timeoutMillis = 10_000,
        )
        composeRule.onNodeWithContentDescription("Abrir F2L").performClick()
        composeRule.onNodeWithText("41 casos únicos").assertIsDisplayed()
    }

    @Test
    fun returnsToPreviousCatalogScrollPositionAfterOpeningCase() {
        composeRule.waitUntilAtLeastOneExists(hasText("Algoritmos"), timeoutMillis = 10_000)
        composeRule.onNodeWithText("Algoritmos").performClick()
        composeRule.onNodeWithContentDescription("Abrir F2L").performClick()
        composeRule.onNodeWithTag("algorithm-case-list")
            .performScrollToNode(hasContentDescription("Abrir F2L caso 30"))
        composeRule.onNodeWithContentDescription("Abrir F2L caso 30").performClick()
        composeRule.onNodeWithText("F2L 30").assertIsDisplayed()

        composeRule.onNodeWithText("‹ Voltar").performClick()

        composeRule.onNodeWithContentDescription("Abrir F2L caso 30").assertIsDisplayed()
    }

    @Test
    fun keepsCatalogSearchWhenReturningFromRealDetail() {
        composeRule.waitUntilAtLeastOneExists(hasContentDescription("Abrir F2L"), 10_000)
        composeRule.onNodeWithContentDescription("Abrir F2L").performClick()
        composeRule.onNodeWithContentDescription("Buscar casos").performTextInput("caso 30")
        composeRule.onNodeWithContentDescription("Buscar casos").performImeAction()
        composeRule.onNodeWithContentDescription("Abrir F2L caso 30").performClick()
        composeRule.onNodeWithText("F2L 30").assertIsDisplayed()
        composeRule.onNodeWithText("‹ Voltar").performClick()
        composeRule.onNodeWithContentDescription("Buscar casos").assertTextContains("caso 30")
        composeRule.onNodeWithText("1 de 41 casos").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Abrir F2L caso 30").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Abrir F2L caso 1").assertDoesNotExist()
    }

    @Test
    fun opensOllCatalogWithYellowTopDiagram() {
        composeRule.waitUntilAtLeastOneExists(hasText("Algoritmos"), timeoutMillis = 10_000)
        composeRule.onNodeWithText("Algoritmos").performClick()
        composeRule.onNodeWithContentDescription("Abrir OLL").performClick()
        composeRule.onNodeWithText("57 casos únicos").assertIsDisplayed()
        composeRule.waitUntilAtLeastOneExists(
            hasContentDescription("Vista superior do padrão OLL amarelo"),
            timeoutMillis = 10_000,
        )
    }

    @Test
    fun opensPllCatalogWithPermutationArrows() {
        composeRule.waitUntilAtLeastOneExists(hasText("Algoritmos"), timeoutMillis = 10_000)
        composeRule.onNodeWithText("Algoritmos").performClick()
        composeRule.onNodeWithContentDescription("Abrir PLL").performClick()
        composeRule.onNodeWithText("21 casos únicos").assertIsDisplayed()
        composeRule.waitUntilAtLeastOneExists(
            hasContentDescription("Vista superior PLL com setas de permutação"),
            timeoutMillis = 10_000,
        )
    }

    @Test
    fun opensPllDetailWithAnimatedLastLayerCube() {
        composeRule.waitUntilAtLeastOneExists(hasText("Algoritmos"), timeoutMillis = 10_000)
        composeRule.onNodeWithText("Algoritmos").performClick()
        composeRule.onNodeWithContentDescription("Abrir PLL").performClick()
        composeRule.onNodeWithContentDescription("Abrir PLL caso 1").performClick()
        composeRule.onNodeWithText("PLL 1").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Cubo 3x3 animado").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Reproduzir algoritmo").assertIsDisplayed()
    }

    @Test
    fun configuresAndStartsTimedAdvancedQuiz() {
        composeRule.waitUntilAtLeastOneExists(hasText("Quiz"), timeoutMillis = 10_000)
        composeRule.onNodeWithText("Quiz").performClick()
        composeRule.onNodeWithText("Quiz visual de notação").assertIsDisplayed()
        composeRule.onNodeWithText("Avançado").performClick()
        composeRule.onNodeWithText("60 segundos").performClick()
        composeRule.onNodeWithText("Começar").performClick()
        composeRule.onNodeWithText("Qual notação representa este movimento?").assertIsDisplayed()
        composeRule.onNodeWithContentDescription(
            "Demonstração animada do movimento, repetida continuamente",
        ).assertIsDisplayed()
        composeRule.onNodeWithText("0 pontos").assertIsDisplayed()
    }

    @Test
    fun showsDoubleTurnBadgeOnThirdEssentialQuestion() {
        composeRule.waitUntilAtLeastOneExists(hasText("Quiz"), timeoutMillis = 10_000)
        composeRule.onNodeWithText("Quiz").performClick()
        composeRule.onNodeWithText("Começar").performClick()

        composeRule.onNodeWithText("U").performClick()
        composeRule.onNodeWithText("Próxima").performClick()
        composeRule.onNodeWithText("U'").performClick()
        composeRule.onNodeWithText("Próxima").performClick()

        composeRule.onNodeWithText("2×").assertIsDisplayed()
        composeRule.onNodeWithContentDescription(
            "Demonstração animada de movimento duplo, repetida continuamente",
        ).assertIsDisplayed()
    }

    @Test
    fun createsCustomAlgorithmFromVisualBuilder() {
        val testName = "Teste visual ${System.currentTimeMillis()}"
        composeRule.waitUntilAtLeastOneExists(hasText("Mais"), timeoutMillis = 10_000)
        composeRule.onNodeWithText("Mais").performClick()
        composeRule.onNode(hasScrollAction())
            .performScrollToNode(hasText("Algoritmos personalizados"))
        composeRule.onNodeWithText("Algoritmos personalizados").performClick()
        composeRule.onNodeWithText("Criar algoritmo").performClick()
        composeRule.onNodeWithText("Nome obrigatório").performTextInput(testName)
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("R"))
        composeRule.onNodeWithText("R").performClick()
        composeRule.onNode(hasScrollAction())
            .performScrollToNode(hasText("Salvar algoritmo"))
        composeRule.onNodeWithText("Salvar algoritmo").performClick()
        composeRule.waitUntilAtLeastOneExists(hasText(testName), timeoutMillis = 10_000)
        composeRule.onNodeWithText(testName).assertIsDisplayed()
    }

    @Test
    fun opensBackupAndRestoreScreen() {
        composeRule.waitUntilAtLeastOneExists(hasText("Mais"), timeoutMillis = 10_000)
        composeRule.onNodeWithText("Mais").performClick()
        composeRule.onNode(hasScrollAction())
            .performScrollToNode(hasText("Backup e restauração"))
        composeRule.onNodeWithText("Backup e restauração").performClick()
        composeRule.onNodeWithText("Exportar backup").assertIsDisplayed()
        composeRule.onNodeWithText("Importar backup").assertIsDisplayed()
        composeRule.onNode(hasScrollAction())
            .performScrollToNode(hasText("Transferir tempos e sessões"))
        composeRule.onNodeWithText("Transferir tempos e sessões").performClick()
        composeRule.onNodeWithText("Importar do csTimer").assertIsDisplayed()
        composeRule.onNodeWithText("Exportar para csTimer").assertIsDisplayed()
    }

    @Test
    fun opensLocalReminderScreen() {
        composeRule.waitUntilAtLeastOneExists(hasText("Mais"), timeoutMillis = 10_000)
        composeRule.onNodeWithText("Mais").performClick()
        composeRule.onNodeWithText("Lembretes locais").performClick()
        composeRule.onNodeWithText("Lembrar de praticar").assertIsDisplayed()
        composeRule.onNodeWithText("Salvar lembrete").assertIsDisplayed()
    }

    @Test
    fun timerStartsAndStopsFromLargeTouchArea() {
        composeRule.waitUntilAtLeastOneExists(hasText("Cronômetro"), timeoutMillis = 10_000)
        composeRule.onNodeWithText("Cronômetro").performClick()
        composeRule.waitUntilAtLeastOneExists(
            hasContentDescription("Cronômetro parado. Toque para iniciar"),
            timeoutMillis = 60_000,
        )
        composeRule.onNodeWithContentDescription("Cronômetro parado. Toque para iniciar")
            .also { node ->
                val area = node.fetchSemanticsNode().boundsInRoot
                val screen = composeRule.onRoot().fetchSemanticsNode().boundsInRoot
                assertTrue("Idle timer should occupy most of the screen", area.height > screen.height * 0.60f)
            }
            .performTouchInput { click(Offset(width * 0.1f, height * 0.1f)) }
        composeRule.waitUntilAtLeastOneExists(
            hasContentDescription("Cronômetro em andamento. Toque para concluir"),
            timeoutMillis = 10_000,
        )
        composeRule.onNodeWithContentDescription("Cronômetro em andamento. Toque para concluir")
            .also { node ->
                val area = node.fetchSemanticsNode().boundsInRoot
                val screen = composeRule.onRoot().fetchSemanticsNode().boundsInRoot
                assertTrue("Running timer should fill the screen", area.height > screen.height * 0.90f)
                composeRule.onNodeWithText("Mais").assertDoesNotExist()
                composeRule.onNodeWithText("Opções").assertDoesNotExist()
            }
            .performTouchInput { click(Offset(width * 0.9f, height * 0.9f)) }
        composeRule.waitUntilAtLeastOneExists(
            hasContentDescription("Cronômetro parado. Toque para iniciar"),
            timeoutMillis = 10_000,
        )
        composeRule.onNodeWithText("Histórico").assertIsDisplayed()
    }

    @Test
    fun timerOpensScrambleAsTwoDimensionalCubeNet() {
        composeRule.waitUntilAtLeastOneExists(hasText("Cronômetro"), timeoutMillis = 10_000)
        composeRule.onNodeWithText("Cronômetro").performClick()
        composeRule.waitUntilAtLeastOneExists(
            hasContentDescription("Cronômetro parado. Toque para iniciar"),
            timeoutMillis = 60_000,
        )
        composeRule.onNodeWithText("Cubo 2D").performClick()

        composeRule.onNodeWithText("Cubo após o embaralhamento").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Planificação 2D do cubo embaralhado")
            .assertIsDisplayed()
    }

    @Test
    fun timerOpensRecordsAnalysisAndSeparateHistory() {
        composeRule.waitUntilAtLeastOneExists(hasText("Cronômetro"), timeoutMillis = 10_000)
        composeRule.onNodeWithText("Cronômetro").performClick()
        composeRule.waitUntilAtLeastOneExists(hasText("Análise"), timeoutMillis = 60_000)
        composeRule.onNodeWithText("Análise").performClick()

        composeRule.onNodeWithText("Recordes e análise").assertIsDisplayed()
        composeRule.onNodeWithText("Recordes pessoais").assertIsDisplayed()
        composeRule.onNodeWithText("ao100").assertIsDisplayed()
        composeRule.onNodeWithText("Tudo").assertIsDisplayed()
        composeRule.onNodeWithText("Fechar").performClick()
        composeRule.onNodeWithText("Histórico").assertIsDisplayed()
        composeRule.onNodeWithText("Histórico").performClick()
        composeRule.onNodeWithText("Histórico de tempos").assertIsDisplayed()
        composeRule.onNodeWithText("Atual").assertIsDisplayed()
        composeRule.onNodeWithText("Fechar").performClick()
        composeRule.onNodeWithText("Opções").assertIsDisplayed()
    }

    @Test
    fun timerConfiguresDirectedCfopTrainingByCategory() {
        composeRule.waitUntilAtLeastOneExists(hasText("Cronômetro"), timeoutMillis = 10_000)
        composeRule.onNodeWithText("Cronômetro").performClick()
        composeRule.onNodeWithText("Opções").performClick()
        composeRule.waitUntilAtLeastOneExists(
            hasContentDescription("Configurar treino CFOP"),
            timeoutMillis = 60_000,
        )
        composeRule.onNodeWithContentDescription("Configurar treino CFOP").performClick()
        composeRule.onNodeWithText("Treino CFOP direcionado").assertIsDisplayed()
        composeRule.onNodeWithText("F2L").performClick()
        composeRule.onNodeWithText("Aplicar").performClick()

        composeRule.waitUntilAtLeastOneExists(
            hasText("F2L · Todos os casos", substring = true),
            timeoutMillis = 10_000,
        )
        composeRule.onNodeWithText("F2L · Todos os casos", substring = true).assertIsDisplayed()
    }

    @Test
    fun timerCanStartSolveFromOptionalInspection() {
        composeRule.waitUntilAtLeastOneExists(hasText("Cronômetro"), timeoutMillis = 10_000)
        composeRule.onNodeWithText("Cronômetro").performClick()
        composeRule.waitUntilAtLeastOneExists(
            hasContentDescription("Cronômetro parado. Toque para iniciar"),
            timeoutMillis = 60_000,
        )
        composeRule.onNodeWithText("Opções").performClick()
        composeRule.onNodeWithText("Inspeção 15s").performClick()
        composeRule.waitUntilAtLeastOneExists(
            hasText("Inspeção 15s") and isSelected(),
            timeoutMillis = 10_000,
        )
        composeRule.onNodeWithText("Fechar").performClick()
        composeRule.onNodeWithContentDescription("Cronômetro parado. Toque para iniciar")
            .performClick()
        composeRule.waitUntilAtLeastOneExists(
            hasContentDescription("Inspeção em andamento. Toque para iniciar a resolução"),
            timeoutMillis = 10_000,
        )
        composeRule.onNodeWithContentDescription(
            "Inspeção em andamento. Toque para iniciar a resolução",
        ).performClick()
        composeRule.waitUntilAtLeastOneExists(
            hasContentDescription("Cronômetro em andamento. Toque para concluir"),
            timeoutMillis = 10_000,
        )
        composeRule.onNodeWithContentDescription("Cronômetro em andamento. Toque para concluir")
            .performClick()
        composeRule.waitUntilAtLeastOneExists(
            hasContentDescription("Cronômetro parado. Toque para iniciar"),
            timeoutMillis = 10_000,
        )
        composeRule.onNodeWithText("Opções").performClick()
        composeRule.onNodeWithText("Inspeção 15s").performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            runCatching {
                composeRule.onNodeWithText("Inspeção 15s").assertIsNotSelected()
            }.isSuccess
        }
    }

    @Test
    fun timerBackRequiresConfirmationAndRestoresControlsAfterCancel() {
        composeRule.waitUntilAtLeastOneExists(hasText("Cronômetro"), timeoutMillis = 10_000)
        composeRule.onNodeWithText("Cronômetro").performClick()
        composeRule.waitUntilAtLeastOneExists(
            hasContentDescription("Cronômetro parado. Toque para iniciar"), timeoutMillis = 60_000)
        composeRule.onNodeWithContentDescription("Cronômetro parado. Toque para iniciar").performClick()
        composeRule.waitUntilAtLeastOneExists(
            hasContentDescription("Cronômetro em andamento. Toque para concluir"), timeoutMillis = 10_000)
        composeRule.activity.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.onNodeWithText("Cancelar tentativa?").assertIsDisplayed()
        composeRule.onNodeWithText("Continuar").performClick()
        composeRule.onNodeWithContentDescription("Cronômetro em andamento. Toque para concluir")
            .assertIsDisplayed()
        composeRule.activity.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.onNodeWithText("Cancelar tentativa").performClick()
        composeRule.onNodeWithText("Opções").assertIsDisplayed()
        composeRule.onNodeWithText("Mais").assertIsDisplayed()
    }
}
