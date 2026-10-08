package com.gabs.cubo3x3.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.gabs.cubo3x3.cube.CubeRenderer
import com.gabs.cubo3x3.cube.CubeState
import com.gabs.cubo3x3.cube.CubeVisualMode
import com.gabs.cubo3x3.cube.Move
import com.gabs.cubo3x3.cube.StickerColor
import com.gabs.cubo3x3.cube.Vec3i
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
internal fun AlgorithmCaseDiagram(
    entry: AlgorithmEntry,
    state: CubeState,
    animatedMove: Move?,
    progress: Float,
    isDetail: Boolean,
    modifier: Modifier = Modifier,
) {
    when (entry.category) {
        AlgorithmCategory.F2L -> CubeRenderer(
            state = state,
            animatedMove = animatedMove,
            progress = progress,
            visualMode = CubeVisualMode.F2L_LAYERS,
            modifier = modifier,
        )
        AlgorithmCategory.OLL -> if (isDetail) {
            CubeRenderer(
                state = state,
                animatedMove = animatedMove,
                progress = progress,
                visualMode = CubeVisualMode.OLL_YELLOW,
                modifier = modifier,
            )
        } else {
            OllTopDiagram(state = state, modifier = modifier)
        }
        AlgorithmCategory.PLL -> if (isDetail) {
            CubeRenderer(
                state = state,
                animatedMove = animatedMove,
                progress = progress,
                visualMode = CubeVisualMode.PLL_LAST_LAYER,
                modifier = modifier,
            )
        } else {
            PllTopDiagram(state = state, modifier = modifier)
        }
    }
}

@Composable
private fun OllTopDiagram(
    state: CubeState,
    modifier: Modifier = Modifier,
) {
    val pattern = remember(state) { ollPattern(state) }
    Canvas(
        modifier = modifier.semantics {
            contentDescription = "Vista superior do padrão OLL amarelo"
        },
    ) {
        val boardSize = min(size.width, size.height) * 0.68f
        val cell = boardSize / 3f
        val left = (size.width - boardSize) / 2f
        val top = (size.height - boardSize) / 2f
        val outline = maxOf(1.5f, cell * 0.045f)

        for (row in 0..2) {
            for (column in 0..2) {
                drawRect(
                    color = if (DiagramCell(column, row) in pattern.top) {
                        DIAGRAM_YELLOW
                    } else {
                        DIAGRAM_NEUTRAL
                    },
                    topLeft = Offset(left + column * cell, top + row * cell),
                    size = Size(cell, cell),
                )
            }
        }
        drawGrid(left = left, top = top, boardSize = boardSize, strokeWidth = outline)

        pattern.markers.forEach { marker ->
            drawSideMarker(
                side = marker.side,
                index = marker.index,
                left = left,
                top = top,
                boardSize = boardSize,
                cell = cell,
            )
        }
    }
}

@Composable
private fun PllTopDiagram(
    state: CubeState,
    modifier: Modifier = Modifier,
) {
    val arrows = remember(state) { pllPermutationArrows(state) }
    Canvas(
        modifier = modifier.semantics {
            contentDescription = "Vista superior PLL com setas de permutação"
        },
    ) {
        val boardSize = min(size.width, size.height) * 0.72f
        val cell = boardSize / 3f
        val left = (size.width - boardSize) / 2f
        val top = (size.height - boardSize) / 2f
        val outline = maxOf(1.5f, cell * 0.045f)

        drawRect(
            color = DIAGRAM_YELLOW,
            topLeft = Offset(left, top),
            size = Size(boardSize, boardSize),
        )
        drawGrid(left = left, top = top, boardSize = boardSize, strokeWidth = outline)

        val reciprocalPairs = arrows.groupBy { arrow ->
            setOf(arrow.from, arrow.to)
        }
        reciprocalPairs.values.forEach { group ->
            val arrow = group.first()
            val start = arrow.from.toOffset(left, top, cell)
            val end = arrow.to.toOffset(left, top, cell)
            val color = if (arrow.kind == PllPieceKind.CORNER) {
                PLL_CORNER
            } else {
                PLL_EDGE
            }
            drawPermutationArrow(
                start = start,
                end = end,
                color = color,
                drawStartHead = group.any { it.from == arrow.to && it.to == arrow.from },
            )
        }
    }
}

internal data class DiagramCell(
    val column: Int,
    val row: Int,
)

internal enum class DiagramSide {
    NORTH,
    EAST,
    SOUTH,
    WEST,
}

internal data class SideMarker(
    val side: DiagramSide,
    val index: Int,
)

internal data class OllPattern(
    val top: Set<DiagramCell>,
    val markers: Set<SideMarker>,
)

internal enum class PllPieceKind {
    EDGE,
    CORNER,
}

internal data class PllArrow(
    val from: DiagramCell,
    val to: DiagramCell,
    val kind: PllPieceKind,
)

internal fun ollPattern(state: CubeState): OllPattern {
    val upColor = state.stickers.single { sticker ->
        sticker.position == UP && sticker.normal == UP
    }.color
    val yellowStickers = state.stickers.filter { sticker ->
        sticker.position.y == 1 && sticker.color == upColor
    }
    val top = yellowStickers
        .filter { it.normal == UP }
        .mapTo(mutableSetOf()) { sticker -> sticker.position.toCell() }
    val markers = yellowStickers.mapNotNullTo(mutableSetOf()) { sticker ->
        when (sticker.normal) {
            BACK -> SideMarker(DiagramSide.NORTH, sticker.position.x + 1)
            RIGHT -> SideMarker(DiagramSide.EAST, sticker.position.z + 1)
            FRONT -> SideMarker(DiagramSide.SOUTH, sticker.position.x + 1)
            LEFT -> SideMarker(DiagramSide.WEST, sticker.position.z + 1)
            else -> null
        }
    }
    return OllPattern(top = top, markers = markers)
}

internal fun pllPermutationArrows(state: CubeState): List<PllArrow> {
    val centersByColor = state.stickers
        .filter { sticker -> sticker.position == sticker.normal }
        .associate { sticker -> sticker.color to sticker.normal }
    val upColor = state.stickers.single { sticker ->
        sticker.position == UP && sticker.normal == UP
    }.color

    return state.stickers
        .filter { sticker -> sticker.position.y == 1 }
        .groupBy { sticker -> sticker.position }
        .mapNotNull { (position, stickers) ->
            if (position.x == 0 && position.z == 0) return@mapNotNull null
            val targetNormals = stickers
                .mapNotNull { sticker -> centersByColor[sticker.color] }
                .filter { normal -> normal.y == 0 }
            val target = Vec3i(
                x = targetNormals.sumOf(Vec3i::x),
                y = 1,
                z = targetNormals.sumOf(Vec3i::z),
            )
            if (target == position || stickers.none { it.color == upColor }) {
                null
            } else {
                PllArrow(
                    from = position.toCell(),
                    to = target.toCell(),
                    kind = if (stickers.size == 3) PllPieceKind.CORNER else PllPieceKind.EDGE,
                )
            }
        }
        .sortedWith(compareBy({ it.kind.ordinal }, { it.from.row }, { it.from.column }))
}

private fun Vec3i.toCell() = DiagramCell(column = x + 1, row = z + 1)

private fun DiagramCell.toOffset(left: Float, top: Float, cell: Float) = Offset(
    x = left + (column + 0.5f) * cell,
    y = top + (row + 0.5f) * cell,
)

private fun DrawScope.drawGrid(
    left: Float,
    top: Float,
    boardSize: Float,
    strokeWidth: Float,
) {
    val cell = boardSize / 3f
    drawRect(
        color = DIAGRAM_OUTLINE,
        topLeft = Offset(left, top),
        size = Size(boardSize, boardSize),
        style = Stroke(strokeWidth),
    )
    for (index in 1..2) {
        drawLine(
            color = DIAGRAM_OUTLINE,
            start = Offset(left + index * cell, top),
            end = Offset(left + index * cell, top + boardSize),
            strokeWidth = strokeWidth,
        )
        drawLine(
            color = DIAGRAM_OUTLINE,
            start = Offset(left, top + index * cell),
            end = Offset(left + boardSize, top + index * cell),
            strokeWidth = strokeWidth,
        )
    }
}

private fun DrawScope.drawSideMarker(
    side: DiagramSide,
    index: Int,
    left: Float,
    top: Float,
    boardSize: Float,
    cell: Float,
) {
    val markerLength = cell * 0.7f
    val thickness = maxOf(3f, cell * 0.12f)
    val offset = thickness * 1.65f
    val segmentOffset = index * cell + (cell - markerLength) / 2f
    val markerTopLeft = when (side) {
        DiagramSide.NORTH -> Offset(left + segmentOffset, top - offset)
        DiagramSide.SOUTH -> Offset(left + segmentOffset, top + boardSize + offset - thickness)
        DiagramSide.WEST -> Offset(left - offset, top + segmentOffset)
        DiagramSide.EAST -> Offset(left + boardSize + offset - thickness, top + segmentOffset)
    }
    val markerSize = when (side) {
        DiagramSide.NORTH, DiagramSide.SOUTH -> Size(markerLength, thickness)
        DiagramSide.EAST, DiagramSide.WEST -> Size(thickness, markerLength)
    }
    drawRect(color = DIAGRAM_OUTLINE, topLeft = markerTopLeft, size = markerSize)
    drawRect(
        color = DIAGRAM_YELLOW,
        topLeft = Offset(markerTopLeft.x + 1.5f, markerTopLeft.y + 1.5f),
        size = Size(
            width = (markerSize.width - 3f).coerceAtLeast(1f),
            height = (markerSize.height - 3f).coerceAtLeast(1f),
        ),
    )
}

private fun DrawScope.drawPermutationArrow(
    start: Offset,
    end: Offset,
    color: Color,
    drawStartHead: Boolean,
) {
    val dx = end.x - start.x
    val dy = end.y - start.y
    val length = sqrt(dx * dx + dy * dy)
    if (length < 1f) return
    val inset = min(12f, length * 0.16f)
    val ux = dx / length
    val uy = dy / length
    val lineStart = Offset(start.x + ux * inset, start.y + uy * inset)
    val lineEnd = Offset(end.x - ux * inset, end.y - uy * inset)
    val strokeWidth = maxOf(3f, size.minDimension * 0.024f)

    drawCircle(color = color, radius = strokeWidth * 1.25f, center = start)
    drawCircle(color = color, radius = strokeWidth * 1.25f, center = end)
    drawLine(
        color = color,
        start = lineStart,
        end = lineEnd,
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round,
    )
    drawArrowHead(tip = lineEnd, angle = atan2(dy, dx), color = color, strokeWidth = strokeWidth)
    if (drawStartHead) {
        drawArrowHead(
            tip = lineStart,
            angle = atan2(-dy, -dx),
            color = color,
            strokeWidth = strokeWidth,
        )
    }
}

private fun DrawScope.drawArrowHead(
    tip: Offset,
    angle: Float,
    color: Color,
    strokeWidth: Float,
) {
    val headLength = strokeWidth * 3.3f
    val spread = 0.55f
    listOf(angle + Math.PI.toFloat() - spread, angle + Math.PI.toFloat() + spread)
        .forEach { branchAngle ->
            drawLine(
                color = color,
                start = tip,
                end = Offset(
                    x = tip.x + cos(branchAngle) * headLength,
                    y = tip.y + sin(branchAngle) * headLength,
                ),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
        }
}

private val UP = Vec3i(0, 1, 0)
private val RIGHT = Vec3i(1, 0, 0)
private val LEFT = Vec3i(-1, 0, 0)
private val FRONT = Vec3i(0, 0, 1)
private val BACK = Vec3i(0, 0, -1)

private val DIAGRAM_YELLOW = Color(0xFFFFD43B)
private val DIAGRAM_NEUTRAL = Color(0xFFF1F2F5)
private val DIAGRAM_OUTLINE = Color(0xFF20242B)
private val PLL_EDGE = Color(0xFF1769AA)
private val PLL_CORNER = Color(0xFF9E2A2B)
