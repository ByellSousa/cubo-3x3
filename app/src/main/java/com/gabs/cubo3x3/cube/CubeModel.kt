package com.gabs.cubo3x3.cube

enum class Axis {
    X,
    Y,
    Z,
}

data class Vec3i(
    val x: Int,
    val y: Int,
    val z: Int,
) {
    fun coordinate(axis: Axis): Int = when (axis) {
        Axis.X -> x
        Axis.Y -> y
        Axis.Z -> z
    }

    fun rotateQuarter(axis: Axis, quarterTurns: Int): Vec3i {
        val normalized = if (quarterTurns >= 0) 1 else -1
        return when (axis) {
            Axis.X -> if (normalized > 0) {
                Vec3i(x, -z, y)
            } else {
                Vec3i(x, z, -y)
            }

            Axis.Y -> if (normalized > 0) {
                Vec3i(z, y, -x)
            } else {
                Vec3i(-z, y, x)
            }

            Axis.Z -> if (normalized > 0) {
                Vec3i(-y, x, z)
            } else {
                Vec3i(y, -x, z)
            }
        }
    }
}

enum class StickerColor {
    WHITE,
    YELLOW,
    RED,
    ORANGE,
    GREEN,
    BLUE,
}

data class Sticker(
    val position: Vec3i,
    val normal: Vec3i,
    val color: StickerColor,
)

data class Move(
    val symbol: String,
    val axis: Axis,
    val layers: Set<Int>,
    val quarterTurns: Int,
    val repetitions: Int,
) {
    init {
        require(quarterTurns == -1 || quarterTurns == 1)
        require(repetitions == 1 || repetitions == 2)
        require(layers.isNotEmpty())
        require(layers.all { it in -1..1 })
    }

    fun affects(position: Vec3i): Boolean = position.coordinate(axis) in layers

    fun inverse(): Move = if (repetitions == 2) {
        this
    } else {
        copy(quarterTurns = -quarterTurns)
    }
}

data class CubeState(
    val stickers: List<Sticker>,
) {
    init {
        require(stickers.size == STICKER_COUNT)
    }

    fun apply(move: Move): CubeState {
        var next = this
        repeat(move.repetitions) {
            next = next.rotate(move.axis, move.layers, move.quarterTurns)
        }
        return next
    }

    fun apply(moves: Iterable<Move>): CubeState = moves.fold(this) { state, move ->
        state.apply(move)
    }

    fun isSolved(): Boolean = stickers
        .groupBy(Sticker::normal)
        .values
        .all { face -> face.map(Sticker::color).distinct().size == 1 }

    private fun rotate(
        axis: Axis,
        layers: Set<Int>,
        quarterTurns: Int,
    ): CubeState = copy(
        stickers = stickers.map { sticker ->
            if (sticker.position.coordinate(axis) !in layers) {
                sticker
            } else {
                sticker.copy(
                    position = sticker.position.rotateQuarter(axis, quarterTurns),
                    normal = sticker.normal.rotateQuarter(axis, quarterTurns),
                )
            }
        },
    )

    companion object {
        const val STICKER_COUNT = 54

        fun solved(): CubeState {
            return solvedWithColors(
                up = StickerColor.YELLOW,
                down = StickerColor.WHITE,
                right = StickerColor.GREEN,
                left = StickerColor.BLUE,
                front = StickerColor.RED,
                back = StickerColor.ORANGE,
            )
        }

        /** Standard WCA scrambling orientation: white on U and green on F. */
        fun solvedWcaScrambleOrientation(): CubeState = solvedWithColors(
            up = StickerColor.WHITE,
            down = StickerColor.YELLOW,
            right = StickerColor.RED,
            left = StickerColor.ORANGE,
            front = StickerColor.GREEN,
            back = StickerColor.BLUE,
        )

        private fun solvedWithColors(
            up: StickerColor,
            down: StickerColor,
            right: StickerColor,
            left: StickerColor,
            front: StickerColor,
            back: StickerColor,
        ): CubeState {
            val stickers = buildList {
                for (x in -1..1) {
                    for (z in -1..1) {
                        add(Sticker(Vec3i(x, 1, z), Vec3i(0, 1, 0), up))
                        add(Sticker(Vec3i(x, -1, z), Vec3i(0, -1, 0), down))
                    }
                }

                for (y in -1..1) {
                    for (z in -1..1) {
                        add(Sticker(Vec3i(1, y, z), Vec3i(1, 0, 0), right))
                        add(Sticker(Vec3i(-1, y, z), Vec3i(-1, 0, 0), left))
                    }
                }

                for (x in -1..1) {
                    for (y in -1..1) {
                        add(Sticker(Vec3i(x, y, 1), Vec3i(0, 0, 1), front))
                        add(Sticker(Vec3i(x, y, -1), Vec3i(0, 0, -1), back))
                    }
                }
            }
            return CubeState(stickers)
        }
    }
}

object MoveNotation {
    val roots: List<String> = listOf(
        "U", "D", "L", "R", "F", "B",
        "u", "d", "l", "r", "f", "b",
        "M", "E", "S",
        "x", "y", "z",
    )

    val allCanonicalTokens: List<String> = roots.flatMap { root ->
        listOf(root, root + "'", root + "2")
    }

    fun parseAlgorithm(notation: String): List<Move> {
        if (notation.isBlank()) return emptyList()
        return notation.trim().split(Regex("\\s+")).map(::parseToken)
    }

    fun parseToken(token: String): Move {
        require(TOKEN_PATTERN.matches(token)) {
            "Movimento invalido: " + token
        }

        val hasPrime = token.endsWith("'")
        val withoutPrime = if (hasPrime) token.dropLast(1) else token
        val isDouble = withoutPrime.endsWith("2")
        val rawRoot = if (isDouble) withoutPrime.dropLast(1) else withoutPrime
        val root = if (rawRoot.length == 2 && rawRoot[1] == 'w') {
            rawRoot[0].lowercase()
        } else {
            rawRoot
        }

        val definition = definitions[root]
            ?: throw IllegalArgumentException("Movimento nao suportado: " + token)
        val direction = if (hasPrime) -definition.quarterTurns else definition.quarterTurns

        return Move(
            symbol = token,
            axis = definition.axis,
            layers = definition.layers,
            quarterTurns = direction,
            repetitions = if (isDouble) 2 else 1,
        )
    }

    private data class Definition(
        val axis: Axis,
        val layers: Set<Int>,
        val quarterTurns: Int,
    )

    private val definitions: Map<String, Definition> = mapOf(
        "R" to Definition(Axis.X, setOf(1), -1),
        "L" to Definition(Axis.X, setOf(-1), 1),
        "U" to Definition(Axis.Y, setOf(1), -1),
        "D" to Definition(Axis.Y, setOf(-1), 1),
        "F" to Definition(Axis.Z, setOf(1), -1),
        "B" to Definition(Axis.Z, setOf(-1), 1),
        "r" to Definition(Axis.X, setOf(0, 1), -1),
        "l" to Definition(Axis.X, setOf(-1, 0), 1),
        "u" to Definition(Axis.Y, setOf(0, 1), -1),
        "d" to Definition(Axis.Y, setOf(-1, 0), 1),
        "f" to Definition(Axis.Z, setOf(0, 1), -1),
        "b" to Definition(Axis.Z, setOf(-1, 0), 1),
        "M" to Definition(Axis.X, setOf(0), 1),
        "E" to Definition(Axis.Y, setOf(0), 1),
        "S" to Definition(Axis.Z, setOf(0), -1),
        "x" to Definition(Axis.X, setOf(-1, 0, 1), -1),
        "y" to Definition(Axis.Y, setOf(-1, 0, 1), -1),
        "z" to Definition(Axis.Z, setOf(-1, 0, 1), -1),
    )

    private val TOKEN_PATTERN = Regex("^(?:[UDLRFB]w?|[udlrfbMESxyz])(?:2'?|')?$")
}
