package com.gabs.cubo3x3.cube

/** A face-by-face projection of [CubeState] using the conventional unfolded cube net. */
data class CubeNet(
    val up: List<StickerColor>,
    val left: List<StickerColor>,
    val front: List<StickerColor>,
    val right: List<StickerColor>,
    val back: List<StickerColor>,
    val down: List<StickerColor>,
) {
    init {
        listOf(up, left, front, right, back, down).forEach { face ->
            require(face.size == FACE_STICKER_COUNT)
        }
    }

    companion object {
        private const val FACE_STICKER_COUNT = 9

        fun from(state: CubeState): CubeNet = CubeNet(
            up = state.face(
                normal = Vec3i(0, 1, 0),
                positions = grid(
                    rows = listOf(-1, 0, 1),
                    columns = listOf(-1, 0, 1),
                ) { z, x -> Vec3i(x, 1, z) },
            ),
            left = state.face(
                normal = Vec3i(-1, 0, 0),
                positions = grid(
                    rows = listOf(1, 0, -1),
                    columns = listOf(-1, 0, 1),
                ) { y, z -> Vec3i(-1, y, z) },
            ),
            front = state.face(
                normal = Vec3i(0, 0, 1),
                positions = grid(
                    rows = listOf(1, 0, -1),
                    columns = listOf(-1, 0, 1),
                ) { y, x -> Vec3i(x, y, 1) },
            ),
            right = state.face(
                normal = Vec3i(1, 0, 0),
                positions = grid(
                    rows = listOf(1, 0, -1),
                    columns = listOf(1, 0, -1),
                ) { y, z -> Vec3i(1, y, z) },
            ),
            back = state.face(
                normal = Vec3i(0, 0, -1),
                positions = grid(
                    rows = listOf(1, 0, -1),
                    columns = listOf(1, 0, -1),
                ) { y, x -> Vec3i(x, y, -1) },
            ),
            down = state.face(
                normal = Vec3i(0, -1, 0),
                positions = grid(
                    rows = listOf(1, 0, -1),
                    columns = listOf(-1, 0, 1),
                ) { z, x -> Vec3i(x, -1, z) },
            ),
        )

        private fun grid(
            rows: List<Int>,
            columns: List<Int>,
            position: (row: Int, column: Int) -> Vec3i,
        ): List<Vec3i> = rows.flatMap { row ->
            columns.map { column -> position(row, column) }
        }

        private fun CubeState.face(
            normal: Vec3i,
            positions: List<Vec3i>,
        ): List<StickerColor> = positions.map { position ->
            stickers.single { sticker ->
                sticker.normal == normal && sticker.position == position
            }.color
        }
    }
}
