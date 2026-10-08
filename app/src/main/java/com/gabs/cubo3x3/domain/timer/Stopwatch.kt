package com.gabs.cubo3x3.domain.timer

enum class StopwatchPhase { IDLE, RUNNING, PAUSED }

data class StopwatchSession(
    val phase: StopwatchPhase = StopwatchPhase.IDLE,
    val accumulatedMillis: Long = 0L,
    val startedAtRealtimeMillis: Long = 0L,
)

data class FinishedStopwatch(
    val durationMillis: Long,
    val resetSession: StopwatchSession = StopwatchSession(),
)

class Stopwatch(
    private val elapsedRealtime: () -> Long,
) {
    fun start(): StopwatchSession = StopwatchSession(
        phase = StopwatchPhase.RUNNING,
        startedAtRealtimeMillis = elapsedRealtime(),
    )

    fun pause(session: StopwatchSession): StopwatchSession {
        if (session.phase != StopwatchPhase.RUNNING) return session
        return session.copy(
            phase = StopwatchPhase.PAUSED,
            accumulatedMillis = elapsed(session),
            startedAtRealtimeMillis = 0L,
        )
    }

    fun resume(session: StopwatchSession): StopwatchSession {
        if (session.phase != StopwatchPhase.PAUSED) return session
        return session.copy(
            phase = StopwatchPhase.RUNNING,
            startedAtRealtimeMillis = elapsedRealtime(),
        )
    }

    fun finish(session: StopwatchSession): FinishedStopwatch = FinishedStopwatch(
        durationMillis = elapsed(session),
    )

    fun elapsed(session: StopwatchSession): Long {
        val runningDelta = if (session.phase == StopwatchPhase.RUNNING) {
            (elapsedRealtime() - session.startedAtRealtimeMillis).coerceAtLeast(0L)
        } else {
            0L
        }
        return (session.accumulatedMillis + runningDelta).coerceAtLeast(0L)
    }
}
