package com.gabs.cubo3x3.data.timer

data class SolveTime(
    val id: Long,
    val durationMillis: Long,
    val recordedAtEpochMillis: Long,
    val scramble: String,
    val comment: String,
    val penalty: SolvePenalty,
    val sessionId: Long = DEFAULT_TIMER_SESSION_ID,
    val trainingCategory: String? = null,
    val trainingCaseNumber: Int? = null,
)

enum class SolvePenalty {
    NONE,
    PLUS_TWO,
    DNF,
}

val SolveTime.effectiveDurationMillis: Long?
    get() = when (penalty) {
        SolvePenalty.NONE -> durationMillis
        SolvePenalty.PLUS_TWO -> durationMillis + 2_000L
        SolvePenalty.DNF -> null
    }
