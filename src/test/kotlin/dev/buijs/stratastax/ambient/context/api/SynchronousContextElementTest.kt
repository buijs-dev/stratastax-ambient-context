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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import kotlin.coroutines.CoroutineContext

private val testThreadLocal = ThreadLocal<String?>()

internal class SynchronousContextElementTest {

    @Test
    fun `is readable synchronously from plain code inside a withContext block`() {
        runBlocking {
            withContext(TestSyncElement("carried")) {
                assertThat(readSynchronously()).isEqualTo("carried")
            }
        }
    }

    @Test
    fun `survives a Dispatchers-IO hop`() {
        runBlocking {
            withContext(TestSyncElement("carried")) {
                withContext(Dispatchers.IO) { assertThat(readSynchronously()).isEqualTo("carried") }
            }
        }
    }

    @Test
    fun `is restored to null after the installing block exits`() {
        runBlocking {
            withContext(TestSyncElement("carried")) {}

            assertThat(readSynchronously()).isNull()
        }
    }

    @Test
    fun `is readable synchronously from plain code inside an inheritedUni block`() {
        runBlocking {
            val uni = inheritedUni(TestSyncElement("carried")) { readSynchronously() }

            val result = uni.awaitSuspending()
            assertThat(result).isEqualTo("carried")
        }
    }
}

private fun readSynchronously(): String? = testThreadLocal.get()

private class TestSyncElement(
    value: String,
) : SynchronousContextElement<String?>(testThreadLocal, value) {

    companion object Key : CoroutineContext.Key<TestSyncElement>

    override val key: CoroutineContext.Key<TestSyncElement>
        get() = Key
}
