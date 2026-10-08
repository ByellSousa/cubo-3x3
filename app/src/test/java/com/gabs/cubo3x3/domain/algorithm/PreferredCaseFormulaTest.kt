package com.gabs.cubo3x3.domain.algorithm

import com.gabs.cubo3x3.cube.CubeFacelets
import com.gabs.cubo3x3.cube.CubeState
import com.gabs.cubo3x3.cube.MoveNotation
import com.gabs.cubo3x3.ui.AlgorithmCatalog
import com.gabs.cubo3x3.ui.AlgorithmCategory
import org.junit.Assert.*
import org.junit.Test

class PreferredCaseFormulaTest {
    private val f2l = AlgorithmCatalog.entry(AlgorithmCategory.F2L, 1)
    private val oll = AlgorithmCatalog.entry(AlgorithmCategory.OLL, 1)
    private val pll = AlgorithmCatalog.entry(AlgorithmCategory.PLL, 1)
    private fun fails(block: () -> Unit) {
        try { block(); fail("Deveria recusar a fórmula") }
        catch (_: IllegalArgumentException) { }
    }

    @Test fun all119OriginalFormulasPassAgainstTheirCanonicalState() {
        AlgorithmCatalog.allEntries().forEach {
            val result = CaseFormulaValidation.validate(it, it.notation)
            assertEquals(it.notation, result.notation)
            assertTrue(it.id, result.finalState.isSolved())
        }
    }

    @Test fun f2lAlternativeMayLeaveDifferentLastLayerWithoutBreakingFirstLayers() {
        val result = CaseFormulaValidation.validate(f2l, f2l.notation + " U")
        assertFalse(result.finalState.isSolved())
        assertEquals(f2l.notation + " U", result.notation)
    }

    @Test fun ollAlternativeMayPermuteLastLayerWhileKeepingYellowOriented() {
        val result = CaseFormulaValidation.validate(oll, oll.notation + " U")
        assertFalse(result.finalState.isSolved())
    }

    @Test fun pllMustSolveAllFacesNotOnlyKeepYellowOriented() {
        fails { CaseFormulaValidation.validate(pll, pll.notation + " U") }
    }

    @Test fun breaksInPreviouslySolvedLayersAreRejectedForAllStages() {
        listOf(f2l, oll, pll).forEach {
            fails { CaseFormulaValidation.validate(it, it.notation + " R") }
        }
    }

    @Test fun validSyntaxAloneAndFormulaOfWrongCaseDoNotPass() {
        fails { CaseFormulaValidation.validate(f2l, "U") }
        fails { CaseFormulaValidation.validate(f2l,
            AlgorithmCatalog.entry(AlgorithmCategory.F2L, 2).notation) }
    }

    @Test fun validationNeverBuildsTargetFromSuppliedAlternative() {
        // Inverting this fake entry's formula would incorrectly make U' look correct.
        fails { CaseFormulaValidation.validate(f2l.copy(notation = "U"), "U'") }
        assertTrue(CaseFormulaValidation.validate(f2l.copy(notation = "Q"), f2l.notation)
            .finalState.isSolved())
    }

    @Test fun blankMalformedUnsupportedAndConcatenatedTokensAreRejected() {
        listOf("", "  ", "R Q", "R3", "RU", "R’’", "(R U R')").forEach {
            fails { CaseFormulaValidation.validate(f2l, it) }
        }
    }

    @Test fun spacesNormalizeButDoublePrimesWideAliasesSlicesAndCaseArePreserved() {
        val notation = "  ${f2l.notation}\n R2' R2' Rw Rw' M M' x2' x2'  "
        val normalized = CaseFormulaValidation.validate(f2l, notation).notation
        assertEquals(f2l.notation + " R2' R2' Rw Rw' M M' x2' x2'", normalized)
        assertEquals("x2' x2' M M' Rw Rw' R2' R2' R U' R'",
            CaseFormulaValidation.inverseNotation(normalized))
    }

    @Test fun fullCubeRotationsAreValidatedRelativeToTheirCenters() {
        listOf(f2l, oll, pll).forEach {
            assertTrue(CaseFormulaValidation.validate(it, it.notation + " x y' z2'")
                .finalState.isSolved())
        }
    }

    @Test fun maximumTextAndMovesAreAcceptedButOverLimitIsRejected() {
        assertEquals(f2l.notation,
            CaseFormulaValidation.validate(f2l, f2l.notation.padEnd(2_000)).notation)
        fails { CaseFormulaValidation.validate(f2l, f2l.notation.padEnd(2_001)) }
        val moves = f2l.notation + " " + List(197) { "U" }.joinToString(" ")
        assertEquals(200, MoveNotation.parseAlgorithm(CaseFormulaValidation.validate(f2l, moves).notation).size)
        fails { CaseFormulaValidation.validate(f2l, moves + " U") }
    }

    @Test fun personalInverseReturnsExact54StickersFromItsOwnResult() {
        AlgorithmCatalog.allEntries().forEach {
            // Some original formulas rotate centers; a world-space U is not always the yellow layer.
            val alternative = it.notation + " x2'"
            val result = CaseFormulaValidation.validate(it, alternative)
            val returned = result.finalState.apply(
                MoveNotation.parseAlgorithm(CaseFormulaValidation.inverseNotation(result.notation)))
            assertEquals(it.id, CubeFacelets.encode(AlgorithmCatalog.initialState(it)),
                CubeFacelets.encode(returned))
        }
    }

    @Test fun inverseOfPartialStageSolutionReturnsExactCaseNotAnInventedSolvedSetup() {
        listOf(f2l, oll).forEach {
            val result = CaseFormulaValidation.validate(it, it.notation + " U")
            assertFalse(result.finalState.isSolved())
            val inverse = MoveNotation.parseAlgorithm(CaseFormulaValidation.inverseNotation(result.notation))
            assertEquals(CubeFacelets.encode(AlgorithmCatalog.initialState(it)),
                CubeFacelets.encode(result.finalState.apply(inverse)))
            assertNotEquals(CubeFacelets.encode(AlgorithmCatalog.initialState(it)),
                CubeFacelets.encode(CubeState.solved().apply(inverse)))
        }
    }

    @Test fun originalSetupAndCatalogStayUnchangedWhenValidatingPersonalFormula() {
        val before = AlgorithmCatalog.allEntries()
        val setup = AlgorithmCatalog.setupNotation(f2l)
        CaseFormulaValidation.validate(f2l, f2l.notation + " U2'")
        assertEquals(before, AlgorithmCatalog.allEntries())
        assertEquals(setup, AlgorithmCatalog.setupNotation(f2l))
        assertEquals(CubeFacelets.encode(AlgorithmCatalog.initialState(f2l)),
            CubeFacelets.encode(CubeState.solved().apply(MoveNotation.parseAlgorithm(setup))))
    }

    @Test fun persistedModelValidatesIdentifierTimestampAndSemantics() {
        val valid = PreferredCaseFormula("F2L", 1, " R U R' ", 0).validated()
        assertEquals("F2L-1", valid.caseId)
        assertEquals(f2l.notation, valid.notation)
        listOf(valid.copy(category = "BAD"), valid.copy(caseNumber = 42),
            valid.copy(updatedAtEpochMillis = -1), valid.copy(notation = "U")).forEach {
            fails { it.validated() }
        }
    }
}
