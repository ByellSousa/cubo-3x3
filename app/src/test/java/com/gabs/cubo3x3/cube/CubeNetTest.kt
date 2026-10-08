package com.gabs.cubo3x3.cube

import org.junit.Assert.assertEquals
import org.junit.Test

class CubeNetTest {
    @Test
    fun solvedNetUsesCanonicalCfopOrientation() {
        val net = CubeNet.from(CubeState.solved())

        assertEquals(List(9) { StickerColor.YELLOW }, net.up)
        assertEquals(List(9) { StickerColor.BLUE }, net.left)
        assertEquals(List(9) { StickerColor.RED }, net.front)
        assertEquals(List(9) { StickerColor.GREEN }, net.right)
        assertEquals(List(9) { StickerColor.ORANGE }, net.back)
        assertEquals(List(9) { StickerColor.WHITE }, net.down)
    }

    @Test
    fun scrambledNetContainsEveryStickerExactlyOnce() {
        val state = CubeState.solved().apply(
            MoveNotation.parseAlgorithm("R U R' U' F2 L D2 B'"),
        )
        val net = CubeNet.from(state)
        val colors = net.up + net.left + net.front + net.right + net.back + net.down

        assertEquals(CubeState.STICKER_COUNT, colors.size)
        StickerColor.entries.forEach { color ->
            assertEquals(9, colors.count { it == color })
        }
    }

    @Test
    fun faceCentersStayAttachedToTheirNormalsAfterFaceTurns() {
        val state = CubeState.solved().apply(
            MoveNotation.parseAlgorithm("R U2 F' L2 D B"),
        )
        val net = CubeNet.from(state)

        assertEquals(StickerColor.YELLOW, net.up[4])
        assertEquals(StickerColor.BLUE, net.left[4])
        assertEquals(StickerColor.RED, net.front[4])
        assertEquals(StickerColor.GREEN, net.right[4])
        assertEquals(StickerColor.ORANGE, net.back[4])
        assertEquals(StickerColor.WHITE, net.down[4])
    }
}
