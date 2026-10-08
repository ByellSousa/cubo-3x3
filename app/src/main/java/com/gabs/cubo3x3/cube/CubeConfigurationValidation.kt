package com.gabs.cubo3x3.cube

import cs.min2phase.Tools

enum class CubeConfigurationProblem {
    FORMAT, COLOR_COUNT, CENTERS, EDGES, EDGE_FLIP, CORNERS, CORNER_TWIST, PARITY, OTHER,
}

data class CubeConfigurationReport(
    val problem: CubeConfigurationProblem?,
    val message: String?,
    val colorCounts: List<Int>,
    val reviewStickerIndices: Set<Int> = emptySet(),
) {
    val isValid: Boolean get() = problem == null
}

/** Diagnostico da pintura, sem tentar corrigir ou adivinhar adesivos automaticamente. */
object CubeConfigurationValidation {
    private val solvedStickers = CubeFacelets.decode(CubeFacelets.solved).stickers
    private val pieces = solvedStickers
        .withIndex().groupBy { it.value.position }.values
        .map { piece -> piece.map { it.index } }

    fun inspect(facelets: String): CubeConfigurationReport {
        val counts = CubeFacelets.faces.map { color -> facelets.count { it == color } }
        fun report(problem: CubeConfigurationProblem, message: String,
            indices: Set<Int> = emptySet()) = CubeConfigurationReport(problem, message, counts, indices)

        if (facelets.length != 54 || facelets.any { it !in CubeFacelets.faces }) {
            return report(CubeConfigurationProblem.FORMAT,
                "Informe os 54 adesivos usando as seis cores.")
        }
        if (counts.any { it != 9 }) {
            return report(CubeConfigurationProblem.COLOR_COUNT,
                "Cada cor precisa aparecer exatamente 9 vezes. Confira a pintura.")
        }
        val centers = CubeFacelets.faces.indices.map { it * 9 + 4 }
            .filter { facelets[it] != CubeFacelets.faces[it / 9] }.toSet()
        if (centers.isNotEmpty()) {
            return report(CubeConfigurationProblem.CENTERS,
                "Mantenha os centros: amarelo acima, vermelho à frente e verde à direita.", centers)
        }
        // O parser externo pode omitir um canto sem U/D e ainda retornar sucesso.
        // Inventario e quiralidade proprios devem ser conferidos antes de Tools.verify.
        val edgesToReview = reviewPieces(facelets, 2)
        if (edgesToReview.isNotEmpty()) {
            return report(CubeConfigurationProblem.EDGES,
                "Há arestas repetidas ou com cores incompatíveis. Confira as seis faces.", edgesToReview)
        }
        val cornersToReview = reviewPieces(facelets, 3) + mirroredCorners(facelets)
        if (cornersToReview.isNotEmpty()) {
            return report(CubeConfigurationProblem.CORNERS,
                "Há cantos repetidos, espelhados ou com cores incompatíveis. Confira as seis faces.",
                cornersToReview)
        }
        return when (Tools.verify(facelets)) {
            0 -> CubeConfigurationReport(null, null, counts)
            -2 -> report(CubeConfigurationProblem.EDGES,
                "Há arestas repetidas ou com cores incompatíveis. Confira as seis faces.",
                reviewPieces(facelets, 2))
            -3 -> report(CubeConfigurationProblem.EDGE_FLIP,
                "Uma aresta está invertida: esse estado não ocorre apenas com giros.")
            -4 -> report(CubeConfigurationProblem.CORNERS,
                "Há cantos repetidos, espelhados ou com cores incompatíveis. Confira as seis faces.",
                reviewPieces(facelets, 3))
            -5 -> report(CubeConfigurationProblem.CORNER_TWIST,
                "Um canto está torcido: esse estado não ocorre apenas com giros.")
            -6 -> report(CubeConfigurationProblem.PARITY,
                "Há uma troca impossível de peças. Confira a posição de arestas e cantos.")
            else -> report(CubeConfigurationProblem.OTHER,
                "Configuração inválida. Confira as cores e a orientação das faces.")
        }
    }

    /** Marca todos os exemplares repetidos: nao e possivel escolher qual foi pintado errado. */
    private fun reviewPieces(facelets: String, size: Int): Set<Int> {
        val groups = pieces.filter { it.size == size }
        fun signature(indices: List<Int>, text: String) = indices.map { text[it] }.sorted().joinToString("")
        val expected = groups.map { signature(it, CubeFacelets.solved) }.toSet()
        val occurrences = groups.groupBy { signature(it, facelets) }
        return occurrences.filter { (colors, copies) -> colors !in expected || copies.size != 1 }
            .values.flatten().flatten().toSet()
    }

    /** Rotacoes de uma peca preservam o determinante das suas tres normais por cor. */
    private fun mirroredCorners(facelets: String): Set<Int> {
        val colorNormals = CubeFacelets.faces.mapIndexed { index, color ->
            color to solvedStickers[index * 9 + 4].normal
        }.toMap()
        fun determinant(normals: List<Vec3i>): Int {
            val (a, b, c) = normals
            return a.x * (b.y * c.z - b.z * c.y) -
                a.y * (b.x * c.z - b.z * c.x) + a.z * (b.x * c.y - b.y * c.x)
        }
        return pieces.filter { it.size == 3 }.filter { indices ->
            val byColor = indices.associate { facelets[it] to solvedStickers[it].normal }
            if (byColor.size != 3) return@filter false // Ja marcado no inventario.
            val colors = byColor.keys.sorted()
            determinant(colors.map(colorNormals::getValue)) != determinant(colors.map(byColor::getValue))
        }.flatten().toSet()
    }
}
