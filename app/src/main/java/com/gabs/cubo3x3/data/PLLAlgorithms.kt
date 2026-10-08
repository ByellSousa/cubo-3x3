package com.gabs.cubo3x3.data

data class PLLAlgorithmCase(
    val number: Int,
    val notation: String,
)

object PLLAlgorithms {
    val all: List<PLLAlgorithmCase> = listOf(
        PLLAlgorithmCase(1, "x R' U R' D2 R U' R' D2 R2"),
        PLLAlgorithmCase(2, "x' R U' R D2 R' U R D2 R2"),
        PLLAlgorithmCase(3, "R2 U R U R' U' R' U' R' U R'"),
        PLLAlgorithmCase(4, "R U' R U R U R U' R' U' R2"),
        PLLAlgorithmCase(5, "M2 U M2 U2 M2 U M2"),
        PLLAlgorithmCase(6, "R U R' U' R' F R2 U' R' U' R U R' F'"),
        PLLAlgorithmCase(7, "R' U L' U2 R U' R' U2 R L U'"),
        PLLAlgorithmCase(8, "R U R' F' R U R' U' R' F R2 U' R' U'"),
        PLLAlgorithmCase(9, "L U2' L' U2' L F' L' U' L U L F L2' U"),
        PLLAlgorithmCase(10, "R' U2 R U2 R' F R U R' U' R' F' R2 U'"),
        PLLAlgorithmCase(11, "R' U R' d' R' F' R2 U' R' U R' F R F"),
        PLLAlgorithmCase(12, "R2 u R' U R' U' R u' R2 y' R' U R"),
        PLLAlgorithmCase(13, "R' U' R y R2 u R' U R U' R u' R2"),
        PLLAlgorithmCase(14, "R2 u' R U' R U R' u R2 y R U' R'"),
        PLLAlgorithmCase(15, "R U R' y' R2 u' R U' R' U R' u R2"),
        PLLAlgorithmCase(16, "R' U2 R' d' R' F' R2 U' R' U R' F R U' F"),
        PLLAlgorithmCase(17, "M2 U M2 U M' U2 M2 U2 M' U2"),
        PLLAlgorithmCase(18, "F R U' R' U' R U R' F' R U R' U' R' F R F'"),
        PLLAlgorithmCase(19, "L U' R U2 L' U R' L U' R U2 L' U R' U"),
        PLLAlgorithmCase(20, "R' U L' U2 R U' L R' U L' U2 R U' L U'"),
        PLLAlgorithmCase(21, "x' R U' R' D R U R' u2 R' U R D R' U' R"),
    )
}
