package com.gabs.cubo3x3.ui

import com.gabs.cubo3x3.cube.CubeState
import com.gabs.cubo3x3.cube.Sticker
import com.gabs.cubo3x3.cube.StickerColor
import com.gabs.cubo3x3.cube.Vec3i
import com.gabs.cubo3x3.domain.quiz.CfopComparisonKey

internal data class ComparisonCue(val label: String, val first: String, val second: String) {
    val differs: Boolean get() = first != second
}

/** Read-only descriptions of the very same canonical model used by the diagrams. */
internal fun comparisonCues(key: CfopComparisonKey): List<ComparisonCue> {
    val first = AlgorithmCatalog.initialState(key.first)
    val second = AlgorithmCatalog.initialState(key.second)
    return when (key.category) {
        AlgorithmCategory.F2L -> {
            val corner = setOf(StickerColor.WHITE, StickerColor.RED, StickerColor.GREEN)
            val edge = setOf(StickerColor.RED, StickerColor.GREEN)
            val aCorner = trackedPiece(first, corner)
            val bCorner = trackedPiece(second, corner)
            val aEdge = trackedPiece(first, edge)
            val bEdge = trackedPiece(second, edge)
            listOf(
                ComparisonCue("Posição do canto branco/vermelho/verde",
                    piecePosition(aCorner), piecePosition(bCorner)),
                ComparisonCue("Orientação do canto", pieceOrientation(aCorner), pieceOrientation(bCorner)),
                ComparisonCue("Posição da aresta vermelho/verde", piecePosition(aEdge), piecePosition(bEdge)),
                ComparisonCue("Orientação da aresta", pieceOrientation(aEdge), pieceOrientation(bEdge)),
            )
        }
        AlgorithmCategory.OLL -> {
            val a = ollPattern(first)
            val b = ollPattern(second)
            listOf(
                ComparisonCue("Amarelos no topo", topCells(a.top), topCells(b.top)),
                ComparisonCue("Amarelos nas laterais", sideMarkers(a.markers), sideMarkers(b.markers)),
            )
        }
        AlgorithmCategory.PLL -> {
            val a = pllPermutationArrows(first).associate { it.from to it.to }
            val b = pllPermutationArrows(second).associate { it.from to it.to }
            (0..2).flatMap { row -> (0..2).map { column -> DiagramCell(column, row) } }
                .filterNot { it == DiagramCell(1, 1) }.map { source ->
                    val kind = if (source.row != 1 && source.column != 1) "Canto" else "Aresta"
                    fun destination(map: Map<DiagramCell, DiagramCell>): String {
                        val target = map[source] ?: source
                        return if (target == source) "${cellName(target)} (fixo)" else cellName(target)
                    }
                    ComparisonCue("$kind ${cellName(source)} → destino", destination(a), destination(b))
                }
        }
    }
}

private fun trackedPiece(state: CubeState, colors: Set<StickerColor>): List<Sticker> =
    state.stickers.groupBy { it.position }.values.single { piece -> piece.map { it.color }.toSet() == colors }

private fun piecePosition(piece: List<Sticker>): String = piece.first().position.let {
    buildList {
        if (it.y > 0) add("acima") else if (it.y < 0) add("abaixo") else add("camada do meio")
        if (it.z > 0) add("frente") else if (it.z < 0) add("atrás")
        if (it.x > 0) add("direita") else if (it.x < 0) add("esquerda")
    }.joinToString(" · ")
}

private fun pieceOrientation(piece: List<Sticker>): String =
    piece.sortedBy { it.color.ordinal }.joinToString("; ") {
        val color = when (it.color) {
            StickerColor.WHITE -> "branco"
            StickerColor.RED -> "vermelho"
            StickerColor.GREEN -> "verde"
            else -> error("Peça fora do par F2L")
        }
        "$color → ${faceName(it.normal)}"
    }

private fun faceName(normal: Vec3i): String = when (normal) {
    Vec3i(0, 1, 0) -> "acima"
    Vec3i(0, -1, 0) -> "abaixo"
    Vec3i(0, 0, 1) -> "frente"
    Vec3i(0, 0, -1) -> "atrás"
    Vec3i(1, 0, 0) -> "direita"
    Vec3i(-1, 0, 0) -> "esquerda"
    else -> error("Normal de face inválida")
}

private fun cellName(cell: DiagramCell): String = "L${cell.row + 1}C${cell.column + 1}"

private fun topCells(cells: Set<DiagramCell>): String =
    cells.sortedWith(compareBy<DiagramCell> { it.row }.thenBy { it.column })
        .joinToString(", ", transform = ::cellName).ifEmpty { "nenhum" }

private fun sideMarkers(markers: Set<SideMarker>): String =
    markers.sortedWith(compareBy<SideMarker> { it.side.ordinal }.thenBy { it.index }).joinToString("; ") {
        val location = when (it.side) {
            DiagramSide.NORTH -> "atrás C${it.index + 1}"
            DiagramSide.EAST -> "direita L${it.index + 1}"
            DiagramSide.SOUTH -> "frente C${it.index + 1}"
            DiagramSide.WEST -> "esquerda L${it.index + 1}"
        }
        location
    }.ifEmpty { "nenhum" }
