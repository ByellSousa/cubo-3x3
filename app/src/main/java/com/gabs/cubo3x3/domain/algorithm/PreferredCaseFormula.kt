package com.gabs.cubo3x3.domain.algorithm

import com.gabs.cubo3x3.cube.CubeState
import com.gabs.cubo3x3.cube.MoveNotation
import com.gabs.cubo3x3.domain.quiz.CfopQuestionFactory
import com.gabs.cubo3x3.ui.AlgorithmCatalog
import com.gabs.cubo3x3.ui.AlgorithmCategory
import com.gabs.cubo3x3.ui.AlgorithmEntry

data class PreferredCaseFormula(
    val category: String,
    val caseNumber: Int,
    val notation: String,
    val updatedAtEpochMillis: Long,
) {
    val caseId: String get() = "$category-$caseNumber"

    fun validated(): PreferredCaseFormula {
        require(updatedAtEpochMillis >= 0L) { "Data da fórmula inválida." }
        val stage = AlgorithmCategory.entries.firstOrNull { it.name == category }
            ?: throw IllegalArgumentException("Categoria da fórmula inválida.")
        val entry = AlgorithmCatalog.entries(stage).firstOrNull { it.number == caseNumber }
            ?: throw IllegalArgumentException("Caso da fórmula inválido.")
        return copy(notation = CaseFormulaValidation.validate(entry, notation).notation)
    }
}

data class ValidatedCaseFormula(val notation: String, val finalState: CubeState)

object CaseFormulaValidation {
    const val MAX_TEXT_LENGTH = 2_000
    const val MAX_MOVES = 200

    fun validate(entry: AlgorithmEntry, notation: String): ValidatedCaseFormula {
        require(notation.length <= MAX_TEXT_LENGTH) { "Use no máximo 2.000 caracteres." }
        val moves = MoveNotation.parseAlgorithm(notation)
        require(moves.isNotEmpty()) { "Adicione pelo menos um movimento." }
        require(moves.size <= MAX_MOVES) { "Use no máximo 200 movimentos." }
        // Never derive the target from the user's formula: that would validate itself.
        val canonical = AlgorithmCatalog.entry(entry.category, entry.number)
        val initial = AlgorithmCatalog.initialState(canonical)
        val normalized = moves.joinToString(" ") { it.symbol }
        require(CfopQuestionFactory.resolvesStage(initial, canonical.copy(notation = normalized))) {
            when (entry.category) {
                AlgorithmCategory.F2L -> "A fórmula não resolve as duas primeiras camadas deste caso."
                AlgorithmCategory.OLL -> "A fórmula não orienta o amarelo preservando as primeiras camadas."
                AlgorithmCategory.PLL -> "A fórmula não resolve completamente este caso PLL."
            }
        }
        return ValidatedCaseFormula(normalized, initial.apply(moves))
    }

    fun inverseNotation(notation: String): String =
        MoveNotation.parseAlgorithm(notation).asReversed().joinToString(" ") { move ->
            when {
                move.repetitions == 2 -> move.symbol
                move.symbol.endsWith("'") -> move.symbol.dropLast(1)
                else -> move.symbol + "'"
            }
        }
}
