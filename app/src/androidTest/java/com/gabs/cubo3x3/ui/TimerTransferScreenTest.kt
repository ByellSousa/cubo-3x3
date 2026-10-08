package com.gabs.cubo3x3.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.gabs.cubo3x3.data.transfer.TimerTransferPreview
import com.gabs.cubo3x3.data.transfer.TransferredSession
import com.gabs.cubo3x3.ui.theme.Cubo3x3Theme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TimerTransferScreenTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun previewRequiresConfirmationAndExposesSkippedSessions() {
        var confirmations = 0
        composeRule.setContent {
            Cubo3x3Theme(darkTheme = true) {
                TimerTransferScreen(
                    state = TimerTransferUiState(importPreview = TimerTransferPreview(
                        sessions = listOf(TransferredSession("Treino", emptyList())),
                        skippedSessionCount = 1,
                        skippedSolveCount = 3,
                    )),
                    actions = TimerTransferActions(confirmImport = { confirmations++ }),
                    onBack = {},
                )
            }
        }
        composeRule.onNodeWithText("Prévia da importação").assertIsDisplayed()
        composeRule.onNodeWithText("Ficam de fora: 1 sessões e 3 tempos de outros tipos.").assertIsDisplayed()
        assertEquals(0, confirmations)
        composeRule.onNodeWithText("Importar em novas sessões").performClick()
        assertEquals(1, confirmations)
    }
}
