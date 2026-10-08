package com.gabs.cubo3x3.cube

import com.gabs.cubo3x3.ui.AlgorithmCatalog
import cs.min2phase.Tools
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import java.util.Random
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

class CaseCalculationConcurrencyTest {
    private val target = CubeState.solved()

    @Test fun cancelledQueuedRequestNeverEntersEngineAndNextRequestCompletes() = runBlocking {
        val entered = CompletableDeferred<Unit>()
        val release = CountDownLatch(1)
        val calls = ConcurrentLinkedQueue<String>()
        val calculator = SerialCaseCalculator { source, _, checkCancelled ->
            calls += source
            if (source == "held") {
                entered.complete(Unit)
                check(release.await(5, TimeUnit.SECONDS)) { "Prazo do teste excedido" }
            }
            checkCancelled()
            CaseSolution(source, 1)
        }
        val first = async { calculator.calculate("held", target) }
        try {
            withTimeout(5_000) { entered.await() }
            val queued = async(start = CoroutineStart.UNDISPATCHED) { calculator.calculate("cancelled", target) }
            queued.cancelAndJoin()
            val next = async { calculator.calculate("next", target) }
            release.countDown()
            assertEquals("held", first.await().notation)
            assertEquals("next", withTimeout(5_000) { next.await() }.notation)
            assertEquals(listOf("held", "next"), calls.toList())
        } finally {
            release.countDown()
            first.cancelAndJoin()
        }
    }

    @Test fun cancelledBlockingSearchCannotPublishOrOverlapTheNextRequest() = runBlocking {
        val entered = CompletableDeferred<Unit>()
        val release = CountDownLatch(1)
        val active = AtomicInteger()
        val maximum = AtomicInteger()
        val published = ConcurrentLinkedQueue<String>()
        val calculator = SerialCaseCalculator { source, _, _ ->
            maximum.accumulateAndGet(active.incrementAndGet(), ::maxOf)
            try {
                if (source == "old") {
                    entered.complete(Unit)
                    check(release.await(5, TimeUnit.SECONDS)) { "Prazo do teste excedido" }
                }
                // Imita uma chamada Java bloqueante que ignora o cancelamento.
                CaseSolution(source, 1)
            } finally {
                active.decrementAndGet()
            }
        }
        val old = async { calculator.calculate("old", target).also { published += it.notation } }
        try {
            withTimeout(5_000) { entered.await() }
            old.cancel()
            val next = async { calculator.calculate("latest", target).also { published += it.notation } }
            release.countDown()
            old.join()
            assertTrue(old.isCancelled)
            assertEquals("latest", withTimeout(5_000) { next.await() }.notation)
            assertEquals(listOf("latest"), published.toList())
            assertEquals(1, maximum.get())
            assertEquals(0, active.get())
        } finally {
            release.countDown()
            old.cancelAndJoin()
        }
    }

    @Test fun engineFailureReleasesTheQueueForRetry() = runBlocking {
        val calculator = SerialCaseCalculator { source, _, _ ->
            check(source != "fail") { "Falha simulada" }
            CaseSolution(source, 1)
        }
        val failure = runCatching { calculator.calculate("fail", target) }.exceptionOrNull()
        assertTrue(failure is IllegalStateException)
        assertEquals("retry", calculator.calculate("retry", target).notation)
    }

    @Test fun engineRunsOutsideTheCallingThread() = runBlocking {
        val caller = Thread.currentThread()
        val engineThread = AtomicReference<Thread>()
        val calculator = SerialCaseCalculator { source, _, _ ->
            engineThread.set(Thread.currentThread())
            CaseSolution(source, 1)
        }
        calculator.calculate("source", target)
        assertNotSame(caller, engineThread.get())
    }

    @Test fun realComputeChecksCancellationBeforeAndAfterTableInitialization() {
        val case = AlgorithmCatalog.initialState(AlgorithmCatalog.allEntries().first())
        for (stopAt in 1..3) {
            val checks = AtomicInteger()
            assertThrows(CancellationException::class.java) {
                CaseSolver.compute(CubeFacelets.solved, case) {
                    if (checks.incrementAndGet() == stopAt) throw CancellationException("Teste")
                }
            }
            assertEquals(stopAt, checks.get())
        }
    }

    @Test fun concurrentRealRequestsReachTheirOwnFull54StickerTargets() = runBlocking {
        val entries = AlgorithmCatalog.allEntries()
        val random = Random(20261018)
        val inputs = List(6) { index ->
            Tools.randomCube(random) to AlgorithmCatalog.initialState(entries[index * 19])
        }
        withTimeout(30_000) {
            inputs.map { (source, wanted) ->
                async {
                    val result = CaseSolver.calculate(source, wanted)
                    val moves = MoveNotation.parseAlgorithm(result.notation)
                    assertTrue(moves.size <= 21)
                    val actual = CubeFacelets.decode(source).apply(moves)
                    assertEquals(CubeFacelets.encode(wanted), CubeFacelets.encode(actual))
                }
            }.awaitAll()
        }
        Unit
    }

    @Test fun impossiblePaintingCannotBypassValidationThroughAsyncEntryPoint() = runBlocking {
        val impossible = CubeFacelets.solved.toCharArray().apply {
            val previous = this[0]; this[0] = this[9]; this[9] = previous
        }.concatToString()
        val failure = runCatching { CaseSolver.calculate(impossible, target) }.exceptionOrNull()
        assertTrue(failure is IllegalArgumentException)
        assertEquals(CaseSolver.validationError(impossible), failure?.message)
        assertEquals("", CaseSolver.calculate(CubeFacelets.solved, target).notation)
    }
}
