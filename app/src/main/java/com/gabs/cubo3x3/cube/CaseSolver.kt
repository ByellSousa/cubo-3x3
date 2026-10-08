package com.gabs.cubo3x3.cube

import cs.min2phase.Search
import cs.min2phase.SearchWCA

data class CaseSolution(val notation: String, val elapsedMillis: Long)

object CaseSolver {
    private val calculator = SerialCaseCalculator(::compute)

    fun validationError(facelets: String): String? = CubeConfigurationValidation.inspect(facelets).message

    /** A fila e cancelavel; uma busca Java ja iniciada termina dentro do seu limite interno.
     * Nenhum resultado cancelado retorna à interface. Inicializacao ocorre fora da UI. */
    suspend fun calculate(source: String, target: CubeState): CaseSolution =
        calculator.calculate(source, target)

    internal fun compute(
        source: String,
        target: CubeState,
        checkCancelled: () -> Unit = {},
    ): CaseSolution {
        val start = System.nanoTime()
        checkCancelled()
        val sourceError = validationError(source)
        require(sourceError == null) { sourceError.orEmpty() }
        val targetText = CubeFacelets.encode(target)
        require(validationError(targetText) == null) { "O caso alvo não tem orientação válida." }
        if (source == targetText) {
            checkCancelled()
            return CaseSolution("", 0)
        }
        val initial = CubeFacelets.decode(source)
        val relative = relativeState(initial, target)
        check(validationError(relative) == null) { "Falha ao calcular a transformação do caso." }
        checkCancelled()
        Search.init()
        checkCancelled()
        // SearchWCA 0.20.0 interpreta os limites como milissegundos, nao sondagens.
        val notation = SearchWCA().solution(relative, 21, 3_000L, 0L, 0).trim()
        checkCancelled()
        check(!notation.startsWith("Error")) {
            "Não foi possível calcular dentro do limite. Tente novamente."
        }
        val actual = initial.apply(MoveNotation.parseAlgorithm(notation))
        checkCancelled()
        check(CubeFacelets.encode(actual) == targetText) {
            "A sequência não passou na conferência. Nenhum resultado foi exibido."
        }
        return CaseSolution(notation, (System.nanoTime() - start) / 1_000_000L)
    }

    /** Compoe atual * inverso(alvo), identificando cada adesivo pela peca + cor.
     * Resolver essa permutacao produz movimentos diretamente do atual ao alvo. */
    internal fun relativeState(source: CubeState, target: CubeState): String {
        fun identities(state: CubeState): Map<Pair<Set<StickerColor>, StickerColor>, Sticker> =
            state.stickers.groupBy(Sticker::position).values.flatMap { piece ->
                val colors = piece.map(Sticker::color).toSet()
                piece.map { (colors to it.color) to it }
            }.toMap()
        val targetIdentities = identities(target)
        val normalColors = CubeState.solved().stickers.associate { it.normal to it.color }
        val relative = identities(source).map { (identity, sticker) ->
            val targetSticker = requireNotNull(targetIdentities[identity])
            sticker.copy(color = normalColors.getValue(targetSticker.normal))
        }
        return CubeFacelets.encode(CubeState(relative))
    }
}
