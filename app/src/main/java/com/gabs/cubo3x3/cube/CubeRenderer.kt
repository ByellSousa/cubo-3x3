package com.gabs.cubo3x3.cube

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

enum class CubeColorScheme(val label: String) {
    STANDARD("Padrão"),
    HIGH_CONTRAST("Alto contraste"),
}

enum class CubeVisualMode {
    FULL,
    F2L_LAYERS,
    OLL_YELLOW,
    PLL_LAST_LAYER,
}

enum class CubeViewpoint(val label: String, val yQuarterTurns: Int) {
    FRONT("Frente", 0),
    RIGHT("Direita", 1),
    BACK("Trás", 2),
    LEFT("Esquerda", 3),
}

@Composable
fun CubeRenderer(
    state: CubeState,
    animatedMove: Move?,
    progress: Float,
    modifier: Modifier = Modifier,
    colorScheme: CubeColorScheme = CubeColorScheme.STANDARD,
    viewpoint: CubeViewpoint = CubeViewpoint.FRONT,
    visualMode: CubeVisualMode = CubeVisualMode.FULL,
) {
    val colorsByPosition = remember(state) {
        state.stickers
            .groupBy(Sticker::position)
            .mapValues { (_, stickers) -> stickers.map(Sticker::color).toSet() }
    }
    Canvas(
        modifier = modifier.semantics {
            contentDescription = "Cubo 3x3 animado"
        },
    ) {
        val scale = min(size.width, size.height) / 5.4f
        val origin = Offset(size.width / 2f, size.height * 0.52f)
        val angle = animatedMove?.let {
            it.quarterTurns * it.repetitions * (PI / 2.0) * progress
        } ?: 0.0

        val viewAngle = viewpoint.yQuarterTurns * (PI / 2.0)
        val projected = state.stickers.mapNotNull { sticker ->
            val isAnimated = animatedMove?.affects(sticker.position) == true
            val normal = sticker.normal.toDoubleVector().let { vector ->
                if (isAnimated) vector.rotate(animatedMove.axis, angle) else vector
            }.rotate(Axis.Y, viewAngle)

            if (normal.x + normal.y + normal.z <= 0.05) {
                return@mapNotNull null
            }

            val center = sticker.surfaceCenter()
            val (tangentA, tangentB) = sticker.tangents()
            val corners = listOf(
                center - tangentA - tangentB,
                center + tangentA - tangentB,
                center + tangentA + tangentB,
                center - tangentA + tangentB,
            ).map { point ->
                val animated = if (isAnimated) point.rotate(animatedMove.axis, angle) else point
                animated.rotate(Axis.Y, viewAngle)
            }

            ProjectedSticker(
                depth = corners.sumOf { it.x + it.y + it.z } / corners.size,
                points = corners.map { point ->
                    Offset(
                        x = origin.x + ((point.x - point.z) * 0.866 * scale).toFloat(),
                        y = origin.y + ((-point.y + (point.x + point.z) * 0.5) * scale).toFloat(),
                    )
                },
                color = sticker.displayColor(
                    mode = visualMode,
                    colorScheme = colorScheme,
                    cubieColors = colorsByPosition.getValue(sticker.position),
                ),
            )
        }.sortedBy(ProjectedSticker::depth)

        projected.forEach { sticker ->
            val path = Path().apply {
                moveTo(sticker.points.first().x, sticker.points.first().y)
                sticker.points.drop(1).forEach { lineTo(it.x, it.y) }
                close()
            }
            drawPath(path = path, color = sticker.color)
            drawPath(
                path = path,
                color = Color(0xFF111318),
                style = Stroke(width = 2.4f),
            )
        }
    }
}

private fun Sticker.displayColor(
    mode: CubeVisualMode,
    colorScheme: CubeColorScheme,
    cubieColors: Set<StickerColor>,
): Color = if (mode.isColoredSticker(color, cubieColors)) {
    if (mode == CubeVisualMode.OLL_YELLOW) OLL_YELLOW else color.toComposeColor(colorScheme)
} else {
    NEUTRAL_STICKER
}

internal fun CubeVisualMode.isColoredSticker(
    color: StickerColor,
    cubieColors: Set<StickerColor>,
): Boolean = when (this) {
    CubeVisualMode.FULL -> true
    CubeVisualMode.F2L_LAYERS -> cubieColors in F2L_LAYER_COLOR_SETS
    CubeVisualMode.OLL_YELLOW -> {
        cubieColors in LAST_LAYER_COLOR_SETS && color == StickerColor.YELLOW
    }
    CubeVisualMode.PLL_LAST_LAYER -> cubieColors in LAST_LAYER_COLOR_SETS
}

private data class ProjectedSticker(
    val depth: Double,
    val points: List<Offset>,
    val color: Color,
)

private data class Vec3d(
    val x: Double,
    val y: Double,
    val z: Double,
) {
    operator fun plus(other: Vec3d) = Vec3d(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: Vec3d) = Vec3d(x - other.x, y - other.y, z - other.z)

    fun rotate(axis: Axis, angle: Double): Vec3d {
        val cosine = cos(angle)
        val sine = sin(angle)
        return when (axis) {
            Axis.X -> Vec3d(x, y * cosine - z * sine, y * sine + z * cosine)
            Axis.Y -> Vec3d(x * cosine + z * sine, y, -x * sine + z * cosine)
            Axis.Z -> Vec3d(x * cosine - y * sine, x * sine + y * cosine, z)
        }
    }
}

private fun Vec3i.toDoubleVector() = Vec3d(x.toDouble(), y.toDouble(), z.toDouble())

private fun Sticker.surfaceCenter(): Vec3d {
    val position = position.toDoubleVector()
    val normal = normal.toDoubleVector()
    return Vec3d(
        x = if (normal.x != 0.0) normal.x * 1.5 else position.x,
        y = if (normal.y != 0.0) normal.y * 1.5 else position.y,
        z = if (normal.z != 0.0) normal.z * 1.5 else position.z,
    )
}

private fun Sticker.tangents(): Pair<Vec3d, Vec3d> {
    val halfSticker = 0.43
    return when {
        normal.x != 0 -> Vec3d(0.0, 0.0, halfSticker) to Vec3d(0.0, halfSticker, 0.0)
        normal.y != 0 -> Vec3d(halfSticker, 0.0, 0.0) to Vec3d(0.0, 0.0, halfSticker)
        else -> Vec3d(halfSticker, 0.0, 0.0) to Vec3d(0.0, halfSticker, 0.0)
    }
}

internal fun StickerColor.toComposeColor(scheme: CubeColorScheme): Color = when (scheme) {
    CubeColorScheme.STANDARD -> when (this) {
        StickerColor.WHITE -> Color(0xFFF4F5F7)
        StickerColor.YELLOW -> Color(0xFFFFD43B)
        StickerColor.RED -> Color(0xFFE5484D)
        StickerColor.ORANGE -> Color(0xFFFF8A34)
        StickerColor.GREEN -> Color(0xFF35B66A)
        StickerColor.BLUE -> Color(0xFF3D7EFF)
    }
    CubeColorScheme.HIGH_CONTRAST -> when (this) {
        StickerColor.WHITE -> Color.White
        StickerColor.YELLOW -> Color(0xFFFFFF00)
        StickerColor.RED -> Color(0xFFFF1744)
        StickerColor.ORANGE -> Color(0xFFFF9100)
        StickerColor.GREEN -> Color(0xFF00C853)
        StickerColor.BLUE -> Color(0xFF2962FF)
    }
}

private val NEUTRAL_STICKER = Color(0xFF8D929B)
private val OLL_YELLOW = Color(0xFFFFD43B)

private val SOLVED_COLOR_SETS_BY_POSITION = CubeState.solved().stickers
    .groupBy(Sticker::position)
    .mapValues { (_, stickers) -> stickers.map(Sticker::color).toSet() }

internal val F2L_LAYER_COLOR_SETS = SOLVED_COLOR_SETS_BY_POSITION
    .filterKeys { position -> position.y <= 0 }
    .values
    .toSet()

internal val LAST_LAYER_COLOR_SETS = SOLVED_COLOR_SETS_BY_POSITION
    .filterKeys { position -> position.y == 1 }
    .values
    .toSet()
