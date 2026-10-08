package com.gabs.cubo3x3.ui

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.gabs.cubo3x3.data.progress.AlgorithmProgress
import com.gabs.cubo3x3.ui.theme.Cubo3x3Theme
import org.junit.Rule
import org.junit.Test

class AlgorithmSearchScreenTest {
    @get:Rule val rule = createComposeRule()
    private val progress = mapOf(
        "F2L-1" to AlgorithmProgress(isFavorite = true),
        "F2L-2" to AlgorithmProgress(isCompleted = true),
        "F2L-3" to AlgorithmProgress(isFavorite = true, isCompleted = true),
        "OLL-1" to AlgorithmProgress(isFavorite = true),
        "PLL-1" to AlgorithmProgress(isFavorite = true),
    )

    private fun openCategory() {
        rule.setContent {
            Cubo3x3Theme(darkTheme = true) {
                val list = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
                AlgorithmListScreen(AlgorithmCategory.F2L, progress, list, {}, {})
            }
        }
    }

    @Test fun exactCaseNumberAndClearRestoreFullCatalog() {
        openCategory()
        rule.onNodeWithContentDescription("Buscar casos").performTextInput("caso 30")
        rule.onNodeWithContentDescription("Buscar casos").performImeAction()
        rule.onNodeWithText("1 de 41 casos").assertIsDisplayed()
        rule.onNodeWithContentDescription("Abrir F2L caso 30").assertIsDisplayed()
        rule.onNodeWithContentDescription("Abrir F2L caso 3").assertDoesNotExist()
        rule.onNodeWithText("Limpar").performClick()
        rule.onNodeWithText("41 casos únicos").assertIsDisplayed()
    }

    @Test fun statusFiltersAndNoResultsCanBeCleared() {
        openCategory()
        rule.onNodeWithContentDescription("Filtrar status: Favoritos").performClick()
        rule.onNodeWithText("2 de 41 casos").assertIsDisplayed()
        rule.onNodeWithContentDescription("Buscar casos").performTextInput("2")
        rule.onNodeWithContentDescription("Buscar casos").performImeAction()
        rule.onNodeWithText("0 de 41 casos").assertIsDisplayed()
        rule.onNodeWithText("Limpar busca e filtros").performScrollTo().performClick()
        rule.onNodeWithContentDescription("Filtrar status: Todos").assertIsSelected()
        rule.onNodeWithText("41 casos únicos").assertIsDisplayed()
    }

    @Test fun favoritesCanBeFilteredByCategoryAndCompletion() {
        rule.setContent {
            Cubo3x3Theme(darkTheme = false) {
                val list = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
                SavedAlgorithmListScreen(SavedAlgorithmFilter.FAVORITES, progress, list, {}, {})
            }
        }
        rule.onNodeWithContentDescription("Filtrar etapa: F2L").performClick()
        rule.onNodeWithContentDescription("Filtrar status: Concluídos").performClick()
        rule.onNodeWithText("1 de 4 casos").assertIsDisplayed()
        rule.onNodeWithContentDescription("Abrir F2L caso 3").assertIsDisplayed()
        rule.onNodeWithContentDescription("Abrir OLL caso 1").assertDoesNotExist()
        rule.onNodeWithContentDescription("Filtrar etapa: OLL").performClick()
        rule.onNodeWithText("Limpar busca e filtros").performScrollTo().performClick()
        rule.onNodeWithText("4 de 4 casos").assertIsDisplayed()
    }

    @Test fun queryFiltersAndScrollSurviveOpeningAndReturningFromCase() {
        rule.setContent {
            Cubo3x3Theme(darkTheme = true) {
                var selected by rememberSaveable { mutableIntStateOf(0) }
                val holder = rememberSaveableStateHolder()
                val list = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
                if (selected == 0) {
                    holder.SaveableStateProvider("category-F2L") {
                        AlgorithmListScreen(AlgorithmCategory.F2L, emptyMap(), list, {}, {
                            selected = it.number
                        })
                    }
                } else {
                    TextButton(onClick = { selected = 0 }) { Text("Voltar à lista") }
                }
            }
        }
        rule.onNodeWithContentDescription("Buscar casos").performTextInput("F2L")
        rule.onNodeWithContentDescription("Buscar casos").performImeAction()
        rule.onNodeWithContentDescription("Filtrar status: Pendentes").performScrollTo().performClick()
        rule.onNodeWithTag("algorithm-case-list")
            .performScrollToNode(hasContentDescription("Abrir F2L caso 30"))
        rule.onNodeWithContentDescription("Abrir F2L caso 30").performClick()
        rule.onNodeWithText("Voltar à lista").performClick()
        rule.onNodeWithContentDescription("Buscar casos").assertTextContains("F2L")
        rule.onNodeWithContentDescription("Filtrar status: Pendentes").assertIsSelected()
        rule.onNodeWithContentDescription("Abrir F2L caso 30").assertIsDisplayed()
    }

    @Test fun restorationPreservesCriteriaAndSelectedCategory() {
        val restoration = StateRestorationTester(rule)
        restoration.setContent {
            Cubo3x3Theme(darkTheme = true) {
                val list = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
                SavedAlgorithmListScreen(SavedAlgorithmFilter.FAVORITES, progress, list, {}, {})
            }
        }
        rule.onNodeWithContentDescription("Buscar casos").performTextInput("caso 1")
        rule.onNodeWithContentDescription("Buscar casos").performImeAction()
        rule.onNodeWithContentDescription("Filtrar etapa: PLL").performClick()
        restoration.emulateSavedInstanceStateRestore()
        rule.onNodeWithContentDescription("Buscar casos").assertTextContains("caso 1")
        rule.onNodeWithContentDescription("Filtrar etapa: PLL").assertIsSelected()
        rule.onNodeWithText("1 de 4 casos").assertIsDisplayed()
        rule.onNodeWithContentDescription("Abrir PLL caso 1").assertIsDisplayed()
    }

    @Test fun liveFavoriteChangesUpdateResultsWithoutClearingQuery() {
        val state = mutableStateOf(progress)
        rule.setContent {
            Cubo3x3Theme(darkTheme = true) {
                val list = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
                AlgorithmListScreen(AlgorithmCategory.F2L, state.value, list, {}, {})
            }
        }
        rule.onNodeWithContentDescription("Buscar casos").performTextInput("1")
        rule.onNodeWithContentDescription("Buscar casos").performImeAction()
        rule.onNodeWithContentDescription("Filtrar status: Favoritos").performClick()
        rule.onNodeWithText("1 de 41 casos").assertIsDisplayed()
        rule.runOnIdle { state.value = emptyMap() }
        rule.onNodeWithText("0 de 41 casos").assertIsDisplayed()
        rule.onNodeWithContentDescription("Buscar casos").assertTextContains("1")
    }
}
