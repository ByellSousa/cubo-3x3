package com.gabs.cubo3x3.cube

/** Historico local de pintura; aceita rascunhos incompletos, mas nunca centros alterados. */
class CubePaintHistory private constructor(
    private val snapshots: List<String>,
    private val position: Int,
) {
    val facelets: String get() = snapshots[position]
    val canUndo: Boolean get() = position > 0
    val canRedo: Boolean get() = position < snapshots.lastIndex

    fun paint(index: Int, color: Char): CubePaintHistory {
        require(index in 0 until 54 && index % 9 != 4) { "O centro não pode ser pintado." }
        require(color in CubeFacelets.faces) { "Cor desconhecida." }
        return record(facelets.replaceRange(index, index + 1, color.toString()))
    }

    fun reset(): CubePaintHistory = record(CubeFacelets.solved)
    fun undo(): CubePaintHistory = if (canUndo) CubePaintHistory(snapshots, position - 1) else this
    fun redo(): CubePaintHistory = if (canRedo) CubePaintHistory(snapshots, position + 1) else this

    /** Quando a alteracao afeta uma so face, a UI pode mostra-la sem adivinhar adesivos. */
    fun changedFaceFrom(previous: CubePaintHistory): Int? =
        facelets.indices.filter { facelets[it] != previous.facelets[it] }
            .map { it / 9 }.distinct().singleOrNull()

    fun checkpoint(): List<String> = listOf("1", position.toString()) + snapshots

    private fun record(next: String): CubePaintHistory {
        if (next == facelets) return this
        val retained = (snapshots.take(position + 1) + next).takeLast(MAX_UNDO_STEPS + 1)
        return CubePaintHistory(retained, retained.lastIndex)
    }

    companion object {
        const val MAX_UNDO_STEPS = 100

        fun solved(): CubePaintHistory = CubePaintHistory(listOf(CubeFacelets.solved), 0)

        /** Checkpoint invalido nao e parcialmente recuperado nem enviado ao solver. */
        fun restore(checkpoint: List<String>): CubePaintHistory? {
            if (checkpoint.size !in 3..MAX_UNDO_STEPS + 3 || checkpoint[0] != "1") return null
            val position = checkpoint[1].toIntOrNull() ?: return null
            val snapshots = checkpoint.drop(2)
            if (position !in snapshots.indices || snapshots.any { !isPainting(it) }) return null
            return CubePaintHistory(snapshots, position)
        }

        private fun isPainting(text: String): Boolean =
            text.length == 54 && text.all { it in CubeFacelets.faces } &&
                CubeFacelets.faces.indices.all { text[it * 9 + 4] == CubeFacelets.faces[it] }
    }
}
