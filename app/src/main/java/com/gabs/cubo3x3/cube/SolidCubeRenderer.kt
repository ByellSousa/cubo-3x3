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

/**
 * Variante solida do cubo usada durante giros. Cada cubinho leva consigo as
 * suas faces e adesivos, evitando que os adesivos parecam placas soltas.
 */
@Composable
fun SolidCubeRenderer(
    state: CubeState,
    animatedMove: Move,
    progress: Float,
    modifier: Modifier = Modifier,
    colorScheme: CubeColorScheme = CubeColorScheme.STANDARD,
    viewpoint: CubeViewpoint = CubeViewpoint.FRONT,
) {
    val stickers = remember(state) {
        state.stickers.associateBy { sticker -> sticker.position to sticker.normal }
    }
    Canvas(
        modifier = modifier.semantics {
            contentDescription = "Cubo 3x3 sólido animado"
        },
    ) {
        val scale = min(size.width, size.height) / 5.4f
        val origin = Offset(size.width / 2f, size.height * 0.52f)
        val moveAngle = animatedMove.quarterTurns * animatedMove.repetitions *
            (PI / 2.0) * progress
        val viewAngle = viewpoint.yQuarterTurns * (PI / 2.0)
        val halfCubie = 0.47

        val faces = buildList {
            for (x in -1..1) {
                for (y in -1..1) {
                    for (z in -1..1) {
                        if (x == 0 && y == 0 && z == 0) continue
                        val position = Vec3i(x, y, z)
                        val rotates = animatedMove.affects(position)
                        val center = SolidVec3(x.toDouble(), y.toDouble(), z.toDouble())

                        SOLID_FACE_DEFINITIONS.forEach { definition ->
                            val normal = definition.normal.toSolidVector()
                            val visibleNormal = normal
                                .rotateIf(rotates, animatedMove.axis, moveAngle)
                                .rotate(Axis.Y, viewAngle)
                            if (visibleNormal.x + visibleNormal.y + visibleNormal.z <= 0.05) {
                                return@forEach
                            }

                            val faceCenter = center + (normal * halfCubie)
                            val corners = definition.corners(faceCenter, halfCubie).map { point ->
                                point.rotateIf(rotates, animatedMove.axis, moveAngle)
                                    .rotate(Axis.Y, viewAngle)
                            }
                            val sticker = stickers[position to definition.normal]
                            add(
                                SolidProjectedFace(
                                    depth = corners.sumOf { point ->
                                        point.x + point.y + point.z
                                    } / corners.size,
                                    points = corners.map { point ->
                                        Offset(
                                            x = origin.x +
                                                ((point.x - point.z) * 0.866 * scale).toFloat(),
                                            y = origin.y +
                                                ((-point.y + (point.x + point.z) * 0.5) * scale)
                                                    .toFloat(),
                                        )
                                    },
                                    stickerColor = sticker?.color?.toComposeColor(colorScheme),
                                ),
                            )
                        }
                    }
                }
            }
        }.sortedBy(SolidProjectedFace::depth)

        faces.forEach { face ->
            val basePath = face.points.toPath()
            drawPath(path = basePath, color = CUBIE_BODY)
            drawPath(
                path = basePath,
                color = CUBIE_EDGE,
                style = Stroke(width = 1.8f),
            )
            face.stickerColor?.let { stickerColor ->
                val center = face.points.averageOffset()
                val stickerPoints = face.points.map { point ->
                    Offset(
                        x = center.x + (point.x - center.x) * STICKER_INSET,
                        y = center.y + (point.y - center.y) * STICKER_INSET,
                    )
                }
                val stickerPath = stickerPoints.toPath()
                drawPath(path = stickerPath, color = stickerColor)
                drawPath(
                    path = stickerPath,
                    color = CUBIE_EDGE,
                    style = Stroke(width = 1.6f),
                )
            }
        }
    }
}

private data class SolidFaceDefinition(
    val normal: Vec3i,
    val tangentA: SolidVec3,
    val tangentB: SolidVec3,
) {
    fun corners(center: SolidVec3, halfCubie: Double): List<SolidVec3> = listOf(
        center - (tangentA * halfCubie) - (tangentB * halfCubie),
        center + (tangentA * halfCubie) - (tangentB * halfCubie),
        center + (tangentA * halfCubie) + (tangentB * halfCubie),
        center - (tangentA * halfCubie) + (tangentB * halfCubie),
    )
}

private data class SolidProjectedFace(
    val depth: Double,
    val points: List<Offset>,
    val stickerColor: Color?,
)

private data class SolidVec3(
    val x: Double,
    val y: Double,
    val z: Double,
) {
    operator fun plus(other: SolidVec3) = SolidVec3(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: SolidVec3) = SolidVec3(x - other.x, y - other.y, z - other.z)
    operator fun times(multiplier: Double) = SolidVec3(
        x * multiplier,
        y * multiplier,
        z * multiplier,
    )

    fun rotateIf(rotates: Boolean, axis: Axis, angle: Double): SolidVec3 =
        if (rotates) rotate(axis, angle) else this

    fun rotate(axis: Axis, angle: Double): SolidVec3 {
        val cosine = cos(angle)
        val sine = sin(angle)
        return when (axis) {
            Axis.X -> SolidVec3(x, y * cosine - z * sine, y * sine + z * cosine)
            Axis.Y -> SolidVec3(x * cosine + z * sine, y, -x * sine + z * cosine)
            Axis.Z -> SolidVec3(x * cosine - y * sine, x * sine + y * cosine, z)
        }
    }
}

private fun Vec3i.toSolidVector() = SolidVec3(x.toDouble(), y.toDouble(), z.toDouble())

private fun List<Offset>.averageOffset() = Offset(
    x = sumOf { it.x.toDouble() }.toFloat() / size,
    y = sumOf { it.y.toDouble() }.toFloat() / size,
)

private fun List<Offset>.toPath() = Path().apply {
    moveTo(first().x, first().y)
    drop(1).forEach { point -> lineTo(point.x, point.y) }
    close()
}

private val SOLID_FACE_DEFINITIONS = listOf(
    SolidFaceDefinition(Vec3i(1, 0, 0), SolidVec3(0.0, 1.0, 0.0), SolidVec3(0.0, 0.0, 1.0)),
    SolidFaceDefinition(Vec3i(-1, 0, 0), SolidVec3(0.0, 1.0, 0.0), SolidVec3(0.0, 0.0, 1.0)),
    SolidFaceDefinition(Vec3i(0, 1, 0), SolidVec3(1.0, 0.0, 0.0), SolidVec3(0.0, 0.0, 1.0)),
    SolidFaceDefinition(Vec3i(0, -1, 0), SolidVec3(1.0, 0.0, 0.0), SolidVec3(0.0, 0.0, 1.0)),
    SolidFaceDefinition(Vec3i(0, 0, 1), SolidVec3(1.0, 0.0, 0.0), SolidVec3(0.0, 1.0, 0.0)),
    SolidFaceDefinition(Vec3i(0, 0, -1), SolidVec3(1.0, 0.0, 0.0), SolidVec3(0.0, 1.0, 0.0)),
)

private const val STICKER_INSET = 0.86f
private val CUBIE_BODY = Color(0xFF242832)
private val CUBIE_EDGE = Color(0xFF0B0D12)
