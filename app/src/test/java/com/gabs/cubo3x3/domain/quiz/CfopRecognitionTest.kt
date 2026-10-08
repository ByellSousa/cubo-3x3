package com.gabs.cubo3x3.domain.quiz

import com.gabs.cubo3x3.ui.AlgorithmCatalog
import com.gabs.cubo3x3.ui.AlgorithmCategory
import org.junit.Assert.*
import org.junit.Test

class CfopRecognitionTest {
    @Test fun all119QuestionsHaveOneActuallyCorrectStageAnswer() {
        for (entry in AlgorithmCatalog.allEntries()) {
            val state = AlgorithmCatalog.initialState(entry)
            val question = CfopQuestionFactory.create(entry, 883 + entry.number)
            assertEquals(4, question.options.size)
            assertEquals(4, question.options.map { it.id }.toSet().size)
            assertTrue(question.options.all { it.category == entry.category })
            assertEquals(entry.id, question.options.single {
                CfopQuestionFactory.resolvesStage(state, it)
            }.id)
            assertEquals(entry.notation, question.target.notation)
        }
    }

    @Test fun roundHasTenUniqueCasesAndIsReproducible() {
        for (category in AlgorithmCategory.entries) {
            val round = CfopQuestionFactory.round(category, 47)
            assertEquals(10, round.size)
            assertEquals(10, round.distinct().size)
            assertEquals(round, CfopQuestionFactory.round(category, 47))
            assertNotEquals(round, CfopQuestionFactory.round(category, 48))
        }
    }

    @Test fun optionsAreStableAcrossRestorationAndKeepOriginalNotation() {
        for (entry in AlgorithmCatalog.allEntries()) {
            assertEquals(CfopQuestionFactory.create(entry, 61), CfopQuestionFactory.create(entry, 61))
            CfopQuestionFactory.create(entry, 61).options.forEach {
                assertEquals(AlgorithmCatalog.entry(it.category, it.number).notation, it.notation)
            }
        }
    }

    @Test fun answersBelongToTheirTargetCaseNotTheMistakenOption() {
        val wrong = CfopAttempt("round-0", "OLL", 5, 7, 100)
        wrong.validate()
        assertFalse(wrong.isCorrect)
        assertEquals("OLL-5", wrong.caseId)
        assertTrue(wrong.copy(selectedCaseNumber = 5).isCorrect)
    }

    @Test fun rejectsInvalidCaseCategoryDateAndIdentifier() {
        val good = CfopAttempt("round-0", "F2L", 1, 2, 100)
        listOf(good.copy(category = "CROSS"), good.copy(caseNumber = 42),
            good.copy(selectedCaseNumber = 0), good.copy(recordedAtEpochMillis = -1),
            good.copy(id = ""), good.copy(id = "../bad")).forEach {
            try { it.validate(); fail("Accepted $it") } catch (_: IllegalArgumentException) { }
        }
    }
}
