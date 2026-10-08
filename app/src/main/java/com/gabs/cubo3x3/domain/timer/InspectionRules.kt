package com.gabs.cubo3x3.domain.timer

enum class InspectionPenalty {
    NONE,
    PLUS_TWO,
    DNF,
}

object InspectionRules {
    const val EIGHT_SECOND_CALLOUT_MILLIS = 8_000L
    const val TWELVE_SECOND_CALLOUT_MILLIS = 12_000L
    const val PLUS_TWO_THRESHOLD_MILLIS = 15_000L
    const val DNF_THRESHOLD_MILLIS = 17_000L

    fun penaltyAt(elapsedMillis: Long): InspectionPenalty = when {
        elapsedMillis >= DNF_THRESHOLD_MILLIS -> InspectionPenalty.DNF
        elapsedMillis >= PLUS_TWO_THRESHOLD_MILLIS -> InspectionPenalty.PLUS_TWO
        else -> InspectionPenalty.NONE
    }

    fun remainingMillis(elapsedMillis: Long): Long =
        (PLUS_TWO_THRESHOLD_MILLIS - elapsedMillis).coerceAtLeast(0L)
}
