package com.gabs.cubo3x3.ui

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.gabs.cubo3x3.domain.quiz.CfopAttempt
import com.gabs.cubo3x3.ui.theme.Cubo3x3Theme
import java.util.concurrent.ConcurrentHashMap
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class CfopRecognitionScreenTest {
    @get:Rule val rule = createComposeRule()
    private val answers = ConcurrentHashMap<String, CfopAttempt>()

    private fun open() {
        rule.setContent {
            Cubo3x3Theme(darkTheme = true) {
                CfopRecognitionScreen(emptyList(), { answers.putIfAbsent(it.id, it) }, {})
            }
        }
    }

    @Test fun notationQuizRemainsDefaultAndRecognitionIsSeparate() {
        rule.setContent {
            Cubo3x3Theme(darkTheme = true) {
                QuizHubScreen(emptyMap(), { _, _, _ -> }, emptyList(), {}, {})
            }
        }
        rule.onNodeWithText("Quiz visual de notação").assertIsDisplayed()
        rule.onNodeWithText("Reconhecer CFOP").performClick()
        rule.onNodeWithText("Reconhecimento de casos CFOP").assertIsDisplayed()
        rule.onNodeWithText("Notação").performClick()
        rule.onNodeWithText("Quiz visual de notação").assertIsDisplayed()
    }

    @Test fun confirmedHistoryReplacementDiscardsOldRoundWithoutReplayingAnswer() {
        val revision = mutableStateOf(0L)
        rule.setContent {
            Cubo3x3Theme(darkTheme = true) {
                QuizHubScreen(emptyMap(), { _, _, _ -> }, emptyList(),
                    { answers.putIfAbsent(it.id, it) }, {}, historyRevision = revision.value)
            }
        }
        rule.onNodeWithText("Reconhecer CFOP").performClick()
        rule.onNodeWithText("Começar reconhecimento").performScrollTo().performClick()
        rule.onNodeWithTag("cfop-option-0").performScrollTo().performClick()
        rule.onNodeWithText("Notação").performClick()
        rule.runOnIdle { answers.clear(); revision.value += 1 }
        rule.onNodeWithText("Reconhecer CFOP").performClick()
        rule.onNodeWithText("Reconhecimento de casos CFOP").assertIsDisplayed()
        rule.onNodeWithTag("cfop-option-0").assertDoesNotExist()
        rule.runOnIdle { assertTrue(answers.isEmpty()) }
    }

    @Test fun ollAndPllUseTopDiagramsWithoutAnswerNumber() {
        open()
        rule.onNodeWithText("OLL").performClick()
        rule.onNodeWithText("Começar reconhecimento").performScrollTo().performClick()
        rule.onNodeWithContentDescription("Vista superior do padrão OLL amarelo").assertIsDisplayed()
        rule.onAllNodes(hasText("Caso OLL", substring = true)).assertCountEquals(0)
        rule.onNodeWithText("Encerrar rodada").performScrollTo().performClick()
        rule.onNodeWithText("Encerrar").performClick()
        rule.onNodeWithText("PLL").performClick()
        rule.onNodeWithText("Começar reconhecimento").performScrollTo().performClick()
        rule.onNodeWithContentDescription("Vista superior PLL com setas de permutação").assertIsDisplayed()
    }

    @Test fun answerLocksButtonsAndPersistsForCorrectTarget() {
        open()
        rule.onNodeWithText("Começar reconhecimento").performScrollTo().performClick()
        rule.onNodeWithTag("cfop-option-0").performScrollTo().performClick()
        rule.onNodeWithTag("cfop-option-0").assertIsNotEnabled()
        rule.onNodeWithText("Estudar este caso").performScrollTo().assertIsDisplayed()
        rule.runOnIdle {
            assertEquals(1, answers.size)
            val answer = answers.values.single()
            assertEquals("F2L", answer.category)
            answer.validate()
        }
        rule.onNodeWithText("Próximo padrão").performScrollTo().performClick()
        rule.onNodeWithTag("cfop-option-0").performScrollTo().assertIsEnabled()
    }

    @Test fun questionAndAnswerSurviveSavedStateRestoration() {
        val restoration = StateRestorationTester(rule)
        restoration.setContent {
            Cubo3x3Theme(darkTheme = true) {
                CfopRecognitionScreen(emptyList(), { answers.putIfAbsent(it.id, it) }, {})
            }
        }
        rule.onNodeWithText("Começar reconhecimento").performScrollTo().performClick()
        rule.onNodeWithTag("cfop-option-0").performScrollTo().performClick()
        val before = answers.values.toList()
        restoration.emulateSavedInstanceStateRestore()
        rule.onNodeWithTag("cfop-option-0").performScrollTo().assertIsNotEnabled()
        rule.runOnIdle { assertEquals(before, answers.values.toList()) }
        rule.onNodeWithText("Estudar este caso").performScrollTo().assertIsDisplayed()
    }

    @Test fun returningFromCasePreservesQuizQuestionAndFeedback() {
        rule.setContent {
            Cubo3x3Theme(darkTheme = false) {
                var detail by rememberSaveable { mutableStateOf(false) }
                val holder = rememberSaveableStateHolder()
                if (detail) TextButton(onClick = { detail = false }) { Text("Voltar ao quiz") }
                else holder.SaveableStateProvider("quiz") {
                    QuizHubScreen(emptyMap(), { _, _, _ -> }, emptyList(),
                        { answers.putIfAbsent(it.id, it) }, { detail = true })
                }
            }
        }
        rule.onNodeWithText("Reconhecer CFOP").performClick()
        rule.onNodeWithText("Começar reconhecimento").performScrollTo().performClick()
        rule.onNodeWithTag("cfop-option-0").performScrollTo().performClick()
        rule.onNodeWithText("Estudar este caso").performScrollTo().performClick()
        rule.onNodeWithText("Voltar ao quiz").performClick()
        rule.onNodeWithTag("cfop-option-0").performScrollTo().assertIsNotEnabled()
        rule.onNodeWithText("Estudar este caso").performScrollTo().assertIsDisplayed()
        rule.runOnIdle { assertEquals(1, answers.size) }
    }
}
