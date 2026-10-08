package com.gabs.cubo3x3.domain.timer

import cs.min2phase.Search
import cs.min2phase.SearchWCA
import cs.min2phase.Tools
import java.security.SecureRandom
import java.util.Random

fun interface ScrambleGenerator {
    fun nextScramble(): String
}

class TnoodleScrambleGenerator(
    private val random: Random = SecureRandom(),
    private val search: SearchWCA = SearchWCA(),
) : ScrambleGenerator {
    @Synchronized
    override fun nextScramble(): String {
        repeat(MAX_ATTEMPTS) {
            val randomState = Tools.randomCube(random)
            val result = search.solution(
                randomState,
                MAX_SCRAMBLE_LENGTH,
                TIMEOUT_MILLIS,
                MIN_SEARCH_MILLIS,
                Search.INVERSE_SOLUTION,
            ).trim()
            if (!result.startsWith("Error") && result.split(Regex("\\s+")).size >= MIN_MOVES) {
                return result
            }
        }
        error("TNoodle could not generate a valid 3x3 scramble")
    }

    private companion object {
        const val MAX_SCRAMBLE_LENGTH = 21
        const val TIMEOUT_MILLIS = 60_000L
        const val MIN_SEARCH_MILLIS = 200L
        const val MIN_MOVES = 2
        const val MAX_ATTEMPTS = 5
    }
}
