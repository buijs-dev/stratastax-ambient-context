/**
 * Copyright (c) 2021 - 2026 Buijs Software
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the
 * Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package dev.buijs.stratastax.ambient.context.api

import io.smallrye.mutiny.coroutines.awaitSuspending
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private val testConcurrencyThreadLocal = ThreadLocal<Int?>()

internal class InheritedUniConcurrencyTest {

    @Test
    fun `N concurrent inheritedUni calls never see each other's installed value`() {
        runBlocking {
            val n = 50

            val results =
                (0 until n)
                    .map { i ->
                        async {
                            inheritedUni(TestConcurrencyElement(i)) {
                                val viaContext = readTestConcurrencyElement()
                                val viaSyncGetter = testConcurrencyThreadLocal.get()
                                Triple(i, viaContext, viaSyncGetter)
                            }.awaitSuspending()
                        }
                    }.awaitAll()

            results.forEach { (installed, viaContext, viaSyncGetter) ->
                assertThat(viaContext).isEqualTo(installed)
                assertThat(viaSyncGetter).isEqualTo(installed)
            }
        }
    }

    @Test
    fun `N concurrent inheritedUni delay calls run in parallel, not serialized`() {
        runBlocking {
            val n = 20
            val delayMs = 200L

            val start = System.currentTimeMillis()
            (0 until n)
                .map { async { inheritedUni { delay(delayMs.milliseconds) }.awaitSuspending() } }
                .awaitAll()
            val elapsed = System.currentTimeMillis() - start

            // Fully serialized would take n * delayMs (4000ms here) - a
            // generous fraction of that confirms genuine parallelism without
            // being a flaky, tight timing assertion.
            assertThat(elapsed)
                .isLessThan((n * delayMs) / 2)
                .withFailMessage(
                    "expected well under ${(n * delayMs) / 2}ms if parallel, took ${elapsed}ms",
                )
        }
    }

    @Test
    fun `a custom dispatcher passed to inheritedUni is actually used`() {
        runBlocking {
            val executor =
                Executors.newSingleThreadExecutor { runnable ->
                    Thread(runnable, "custom-test-dispatcher-thread")
                }

            val dispatcher = executor.asCoroutineDispatcher()
            try {
                val threadName =
                    inheritedUni(dispatcher = dispatcher) { Thread.currentThread().name }
                        .awaitSuspending()

                assertThat(threadName)
                    .startsWith("custom-test-dispatcher-thread")
                    .withFailMessage(
                        "expected thread name to start with 'custom-test-dispatcher-thread', was '$threadName'",
                    )
            } finally {
                executor.shutdown()
            }
        }
    }

    @Test
    fun `concurrent inheritedUni calls overlap`() {
        runBlocking {
            val n = 20
            val started = AtomicInteger()
            val allStarted = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()

            val unis =
                (0 until n).map { i ->
                    inheritedUni {
                        if (started.incrementAndGet() == n) {
                            allStarted.complete(Unit)
                        }

                        release.await()
                        i
                    }
                }

            val results = unis.map { uni -> async { uni.awaitSuspending() } }

            try {
                withTimeout(5.seconds) { allStarted.await() }
            } finally {
                release.complete(Unit)
            }

            assertThat(results.awaitAll()).isEqualTo((0 until n).toList())
        }
    }
}

private class TestConcurrencyElement(
    val value: Int,
) : SynchronousContextElement<Int?>(testConcurrencyThreadLocal, value) {

    companion object Key : CoroutineContext.Key<TestConcurrencyElement>

    override val key: CoroutineContext.Key<TestConcurrencyElement>
        get() = Key
}

private suspend fun readTestConcurrencyElement(): Int? =
    currentCoroutineContext()[TestConcurrencyElement]?.value
