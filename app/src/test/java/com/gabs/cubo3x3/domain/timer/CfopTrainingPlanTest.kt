package com.gabs.cubo3x3.domain.timer

import com.gabs.cubo3x3.cube.CubeState
import com.gabs.cubo3x3.cube.MoveNotation
import com.gabs.cubo3x3.data.progress.AlgorithmProgress
import com.gabs.cubo3x3.ui.AlgorithmCatalog
import com.gabs.cubo3x3.ui.AlgorithmCategory
import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class CfopTrainingPlanTest {
    private val completed = CfopTrainingPlan(mode = CfopTrainingMode.COMPLETED)

    @Test fun selectsOnlyCompletedIgnoringFavoritesAndInvalidIds() {
        val progress = mapOf(
            "F2L-1" to AlgorithmProgress(isCompleted = true),
            "OLL-3" to AlgorithmProgress(isCompleted = true),
            "PLL-2" to AlgorithmProgress(isFavorite = true),
            "F2L-999" to AlgorithmProgress(isCompleted = true),
        )
        assertEquals(listOf("F2L-1", "OLL-3"), completed.entries(progress).map { it.id })
    }

    @Test fun filtersAndExtrasAreUnionWithoutDuplicatesOrProgressMutation() {
        val progress = mutableMapOf(
            "F2L-1" to AlgorithmProgress(isCompleted = true),
            "OLL-3" to AlgorithmProgress(isCompleted = true),
        )
        val original = progress.toMap()
        val plan = completed.copy(
            completedCategories = setOf(AlgorithmCategory.F2L),
            extraCaseIds = setOf("F2L-1", "F2L-2"),
        )
        assertEquals(listOf("F2L-1", "F2L-2"), plan.entries(progress).map { it.id })
        assertEquals(original, progress)
    }

    @Test fun noMarksIsEmptyNotSilentlyFreeOrAllCases() {
        val round = CfopTrainingRound.start(completed, emptyMap())
        assertTrue(round.caseIds.isEmpty())
        assertFalse(round.isComplete)
        assertNull(round.nextCase())
    }

    @Test fun all119AreCoveredExactlyOnce() {
        val allIds = AlgorithmCatalog.allEntries().map { it.id }.toSet()
        val progress = allIds.associateWith { AlgorithmProgress(isCompleted = true) }
        repeat(20) { seed ->
            var round = CfopTrainingRound.start(completed, progress, Random(seed))
            val visited = mutableListOf<String>()
            while (!round.isComplete) {
                val id = requireNotNull(round.nextCase())
                visited += id
                round = round.recordAttempt(id)
            }
            assertEquals(119, visited.size)
            assertEquals(allIds, visited.toSet())
            assertNull(round.nextCase())
        }
    }

    @Test fun singleCaseNeedsAnAttemptAndRestartIsNotComplete() {
        val plan = completed.copy(extraCaseIds = setOf("PLL-1"))
        val round = CfopTrainingRound.start(plan, emptyMap())
        repeat(5) { assertEquals("PLL-1", round.nextCase("PLL-1")) }
        assertTrue(round.practicedIds.isEmpty())
        assertTrue(round.recordAttempt("PLL-1").isComplete)
        assertFalse(CfopTrainingRound.start(plan, emptyMap()).isComplete)
    }

    @Test fun skippedPreviewsDoNotCountOrLosePendingCases() {
        val round = CfopTrainingRound(listOf("F2L-1", "OLL-2", "PLL-3"))
        assertEquals("OLL-2", round.nextCase("F2L-1"))
        assertEquals("PLL-3", round.nextCase("OLL-2"))
        assertEquals("F2L-1", round.nextCase("PLL-3"))
        val practiced = round.recordAttempt("OLL-2")
        assertEquals("PLL-3", practiced.nextCase("F2L-1"))
        assertEquals(setOf("OLL-2"), practiced.practicedIds)
    }

    @Test fun repeatedOrUnrelatedAttemptsDoNotInflateCoverage() {
        val round = CfopTrainingRound(listOf("F2L-1", "OLL-2")).recordAttempt("F2L-1")
        assertEquals(round, round.recordAttempt("F2L-1"))
        assertEquals(round, round.recordAttempt("PLL-1"))
        assertFalse(round.isComplete)
    }

    @Test fun liveChangesAffectOnlyNewRounds() {
        val progress = mutableMapOf("F2L-1" to AlgorithmProgress(isCompleted = true))
        val round = CfopTrainingRound.start(completed, progress)
        progress.clear()
        progress["OLL-2"] = AlgorithmProgress(isCompleted = true)
        assertEquals(listOf("F2L-1"), round.caseIds)
        assertEquals(listOf("OLL-2"), CfopTrainingRound.start(completed, progress).caseIds)
    }

    @Test fun modesAndAllNewCasesRoundTripExactly() {
        val plans = listOf(CfopTrainingPlan(), completed,
            CfopTrainingPlan(CfopTrainingMode.CATEGORY, AlgorithmCategory.OLL),
            CfopTrainingPlan(CfopTrainingMode.CASE, AlgorithmCategory.PLL, 21),
            completed.copy(extraCaseIds = AlgorithmCatalog.allEntries().map { it.id }.toSet()))
        plans.forEach { assertEquals(it, CfopTrainingPlanCodec.decode(CfopTrainingPlanCodec.encode(it))) }
        assertEquals(57, plans[2].entries(emptyMap()).size)
        assertEquals("PLL-21", plans[3].entries(emptyMap()).single().id)
    }

    @Test fun rejectsMalformedConfigurationsInsteadOfExpandingSelection() {
        listOf("2|FREE||0|F2L,OLL,PLL|", "1|FREE||-1|F2L,OLL,PLL|",
            "1|CASE|PLL|22|F2L,OLL,PLL|", "1|COMPLETED||0||",
            "1|COMPLETED||0|F2L,F2L|", "1|COMPLETED||0|F2L|OLL-1",
            "1|COMPLETED||0|F2L|F2L-1,F2L-1", "1|COMPLETED||0|F2L|F2L-42",
            "1|CATEGORY||0|F2L,OLL,PLL|", "1|FREE||0|F2L,OLL,PLL|F2L-1",
            "x".repeat(2_001)).forEach {
            assertThrows(IllegalArgumentException::class.java) { CfopTrainingPlanCodec.decode(it) }
        }
    }

    @Test fun rejectsInvalidRoundMembershipOrCoverage() {
        listOf(listOf("F2L-1", "F2L-1"), listOf("F2L-42")).forEach {
            assertThrows(IllegalArgumentException::class.java) { CfopTrainingRound(it) }
        }
        assertThrows(IllegalArgumentException::class.java) {
            CfopTrainingRound(listOf("F2L-1"), setOf("OLL-2"))
        }
    }

    @Test fun setupMatchesAllCanonicalCasesAndPreservesOriginals() {
        val original = AlgorithmCatalog.allEntries().associate { it.id to it.notation }
        val round = CfopTrainingRound.start(
            completed.copy(extraCaseIds = original.keys), emptyMap(), Random(100),
        )
        round.caseIds.forEach { id ->
            val entry = AlgorithmCatalog.allEntries().single { it.id == id }
            val generated = CubeState.solved().apply(MoveNotation.parseAlgorithm(AlgorithmCatalog.setupNotation(entry)))
            assertEquals(id, AlgorithmCatalog.initialState(entry), generated)
            assertEquals(original[id], entry.notation)
        }
    }
}
