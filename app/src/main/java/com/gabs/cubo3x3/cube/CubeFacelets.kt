package com.gabs.cubo3x3.cube

/** Codec proprio: faces vistas de frente, na ordem URFDLB do solver. */
object CubeFacelets {
    const val faces = "URFDLB"
    val colors = listOf(StickerColor.YELLOW, StickerColor.GREEN, StickerColor.RED,
        StickerColor.WHITE, StickerColor.BLUE, StickerColor.ORANGE)
    val solved: String = faces.flatMap { face -> List(9) { face } }.joinToString("")

    private val slots: List<Pair<Vec3i, Vec3i>> = buildList {
        for (face in faces) for (row in 0..2) for (column in 0..2) {
            val position = when (face) {
                'U' -> Vec3i(column - 1, 1, row - 1)
                'R' -> Vec3i(1, 1 - row, 1 - column)
                'F' -> Vec3i(column - 1, 1 - row, 1)
                'D' -> Vec3i(column - 1, -1, 1 - row)
                'L' -> Vec3i(-1, 1 - row, column - 1)
                else -> Vec3i(1 - column, 1 - row, -1)
            }
            val normal = when (face) {
                'U' -> Vec3i(0, 1, 0)
                'R' -> Vec3i(1, 0, 0)
                'F' -> Vec3i(0, 0, 1)
                'D' -> Vec3i(0, -1, 0)
                'L' -> Vec3i(-1, 0, 0)
                else -> Vec3i(0, 0, -1)
            }
            add(position to normal)
        }
    }

    fun encode(state: CubeState): String {
        val bySlot = state.stickers.associateBy { it.position to it.normal }
        require(bySlot.size == 54) { "Adesivos duplicados no modelo." }
        return slots.joinToString("") { slot ->
            faces[colors.indexOf(requireNotNull(bySlot[slot]).color)].toString()
        }
    }

    fun decode(facelets: String): CubeState {
        require(facelets.length == 54 && facelets.all { it in faces }) {
            "Informe os 54 adesivos usando as seis cores."
        }
        return CubeState(slots.mapIndexed { index, (position, normal) ->
            Sticker(position, normal, colors[faces.indexOf(facelets[index])])
        })
    }
}

object CubeOrientations {
    /** Busca apenas as 24 orientacoes rigidas; nao e um solver de pecas. */
    fun setupTo(target: CubeState): String {
        val wanted = CubeFacelets.encode(target)
        val queue = ArrayDeque<Pair<CubeState, List<String>>>()
        queue.add(CubeState.solved() to emptyList())
        val visited = mutableSetOf<String>()
        while (queue.isNotEmpty()) {
            val (state, tokens) = queue.removeFirst()
            val key = CubeFacelets.encode(state)
            if (!visited.add(key)) continue
            if (key == wanted) return tokens.joinToString(" ")
            listOf("x", "x'", "x2", "y", "y'", "y2", "z", "z'", "z2").forEach {
                queue.add(state.apply(MoveNotation.parseToken(it)) to (tokens + it))
            }
        }
        error("O alvo nao e uma orientacao do cubo resolvido.")
    }
}
