package com.gabs.cubo3x3.data.transfer

import com.gabs.cubo3x3.data.timer.SolvePenalty

data class TransferredSolve(
    val durationMillis: Long,
    val recordedAtEpochMillis: Long,
    val scramble: String,
    val comment: String,
    val penalty: SolvePenalty,
)

data class TransferredSession(val name: String, val solves: List<TransferredSolve>)

data class TimerTransferPreview(
    val sessions: List<TransferredSession>,
    val skippedSessionCount: Int = 0,
    val skippedSolveCount: Int = 0,
    val warnings: List<String> = emptyList(),
) {
    val solveCount: Int get() = sessions.sumOf { it.solves.size }
}

data class PreparedTimerExport(val json: String, val preview: TimerTransferPreview)

class TimerTransferException(message: String) : IllegalArgumentException(message)
