package com.gabs.cubo3x3.domain.quiz

import com.gabs.cubo3x3.cube.CubeState
import com.gabs.cubo3x3.cube.MoveNotation
import com.gabs.cubo3x3.cube.StickerColor
import com.gabs.cubo3x3.ui.AlgorithmCatalog
import com.gabs.cubo3x3.ui.AlgorithmCategory
import com.gabs.cubo3x3.ui.AlgorithmEntry
import kotlin.random.Random

data class CfopQuestion(val target: AlgorithmEntry, val options: List<AlgorithmEntry>)

object CfopQuestionFactory {
    fun create(target: AlgorithmEntry, seed: Int): CfopQuestion {
        val state = AlgorithmCatalog.initialState(target)
        val wrong = AlgorithmCatalog.entries(target.category).shuffled(Random(seed))
            .asSequence()
            .filter { it.id != target.id }
            // Do not offer another formula that actually solves the displayed stage.
            .filterNot { resolvesStage(state, it) }
            .take(3).toList()
        check(wrong.size == 3) { "Alternativas insuficientes para ${target.id}" }
        return CfopQuestion(target, (wrong + target).shuffled(Random(seed xor 0x3C91)))
    }

    fun resolvesStage(initial: CubeState, answer: AlgorithmEntry): Boolean {
        val result = initial.apply(MoveNotation.parseAlgorithm(answer.notation))
        if (answer.category == AlgorithmCategory.PLL) return result.isSolved()
        val centers = result.stickers.filter { it.position == it.normal }
            .associate { it.normal to it.color }
        val whiteNormal = centers.entries.single { it.value == StickerColor.WHITE }.key
        fun inFirstLayers(state: com.gabs.cubo3x3.cube.Sticker): Boolean =
            state.position.x * whiteNormal.x + state.position.y * whiteNormal.y +
                state.position.z * whiteNormal.z >= 0
        val firstLayersSolved = result.stickers.filter(::inFirstLayers)
            .all { it.color == centers[it.normal] }
        if (answer.category == AlgorithmCategory.F2L) return firstLayersSolved
        val yellowNormal = centers.entries.single { it.value == StickerColor.YELLOW }.key
        return firstLayersSolved && result.stickers.filter { it.normal == yellowNormal }
            .all { it.color == StickerColor.YELLOW }
    }

    fun round(category: AlgorithmCategory, seed: Int, size: Int = 10): List<AlgorithmEntry> {
        require(size in 1..AlgorithmCatalog.entries(category).size)
        return AlgorithmCatalog.entries(category).shuffled(Random(seed)).take(size)
    }
}

data class CfopAttempt(
    val id: String,
    val category: String,
    val caseNumber: Int,
    val selectedCaseNumber: Int,
    val recordedAtEpochMillis: Long,
) {
    val isCorrect: Boolean get() = caseNumber == selectedCaseNumber
    val caseId: String get() = "$category-$caseNumber"

    fun validate() {
        require(id.matches(Regex("[A-Za-z0-9-]{1,100}"))) { "Identificador inválido" }
        val maximum = when (category) {
            "F2L" -> 41
            "OLL" -> 57
            "PLL" -> 21
            else -> throw IllegalArgumentException("Categoria inválida")
        }
        require(caseNumber in 1..maximum && selectedCaseNumber in 1..maximum)
        require(recordedAtEpochMillis >= 0L)
    }
}
