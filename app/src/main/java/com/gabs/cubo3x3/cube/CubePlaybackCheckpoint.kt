package com.gabs.cubo3x3.cube

/** Salva apenas passos inteiros. Um giro interrompido nunca vira um passo concluido. */
data class CubePlaybackCheckpoint(
    val sequenceId: String,
    val completedMoves: Int,
) {
    fun indexFor(currentSequenceId: String, moveCount: Int): Int {
        require(moveCount >= 0)
        return if (sequenceId == currentSequenceId) completedMoves.coerceIn(0, moveCount) else 0
    }

    fun stateFor(initial: CubeState, moves: List<Move>, currentSequenceId: String): CubeState =
        initial.apply(moves.take(indexFor(currentSequenceId, moves.size)))
}
