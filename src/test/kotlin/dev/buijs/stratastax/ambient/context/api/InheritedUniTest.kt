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
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class InheritedUniTest {

    @Test
    fun `the context passed to inheritedUni is readable inside block`() {
        runBlocking {
            val result =
                inheritedUni(TestUniElement("carried")) { readTestUniElement() }.awaitSuspending()
            assertThat(result).isEqualTo("carried")
        }
    }

    @Test
    fun `inheritedUni with no context behaves like a bare uni`() {
        runBlocking {
            val result = inheritedUni { 42 }.awaitSuspending()
            assertThat(result).isEqualTo(42)
        }
    }

    @Test
    fun `a nested inheritedUni call does not inherit the enclosing call's context`() {
        runBlocking {
            val (outer, inner) =
                inheritedUni(TestUniElement("outer")) {
                        val outerSeen = readTestUniElement()
                        val innerSeen = inheritedUni { readTestUniElement() }.awaitSuspending()
                        outerSeen to innerSeen
                    }
                    .awaitSuspending()
            assertThat(outer).isEqualTo("outer")
            assertThat(inner).isNull()
        }
    }

    @Test
    fun `a bare coroutineContext read inside the block parameter is unreliable so route through a suspend function instead`() {
        runBlocking {
            val bareRead =
                inheritedUni(TestUniElement("carried")) {
                        this@runBlocking.coroutineContext[TestUniElement]?.value
                    }
                    .awaitSuspending()

            val safeRead =
                inheritedUni(TestUniElement("carried")) { readTestUniElement() }.awaitSuspending()

            assertThat(bareRead).isNull()
            assertThat(safeRead).isEqualTo("carried")
        }
    }
}

private class TestUniElement(val value: String) : PropagatedContextElement {
    companion object Key : CoroutineContext.Key<TestUniElement>

    override val key: CoroutineContext.Key<TestUniElement>
        get() = Key
}

private suspend fun readTestUniElement(): String? = currentCoroutineContext()[TestUniElement]?.value
