package com.gabs.cubo3x3.domain.timer

import org.junit.Assert.assertEquals
import org.junit.Test

class StopwatchTest {
    @Test
    fun startPauseResumeAndFinishUseMonotonicElapsedTime() {
        val clock = FakeElapsedClock(1_000L)
        val stopwatch = Stopwatch(clock::now)

        var session = stopwatch.start()
        clock.advance(1_250L)
        assertEquals(1_250L, stopwatch.elapsed(session))

        session = stopwatch.pause(session)
        clock.advance(5_000L)
        assertEquals(1_250L, stopwatch.elapsed(session))

        session = stopwatch.resume(session)
        clock.advance(750L)
        val finished = stopwatch.finish(session)

        assertEquals(2_000L, finished.durationMillis)
        assertEquals(StopwatchSession(), finished.resetSession)
    }

    @Test
    fun elapsedKeepsAdvancingWhenUiIsNotObserving() {
        val clock = FakeElapsedClock(50L)
        val stopwatch = Stopwatch(clock::now)
        val session = stopwatch.start()

        clock.advance(30_000L)

        assertEquals(30_000L, stopwatch.elapsed(session))
    }

    @Test
    fun invalidBackwardClockDeltaCannotProduceNegativeTime() {
        val clock = FakeElapsedClock(500L)
        val stopwatch = Stopwatch(clock::now)
        val session = stopwatch.start()

        clock.set(100L)

        assertEquals(0L, stopwatch.elapsed(session))
    }
}

private class FakeElapsedClock(initialMillis: Long) {
    private var value = initialMillis

    fun now(): Long = value

    fun advance(deltaMillis: Long) {
        value += deltaMillis
    }

    fun set(valueMillis: Long) {
        value = valueMillis
    }
}
