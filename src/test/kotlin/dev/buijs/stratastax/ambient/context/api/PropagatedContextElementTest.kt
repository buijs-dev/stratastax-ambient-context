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

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import kotlin.coroutines.CoroutineContext

internal class PropagatedContextElementTest {

    @Test
    fun `a PropagatedContextElement survives a Dispatchers-IO hop`() {
        runBlocking {
            withContext(TestElement("hello")) {
                withContext(Dispatchers.IO) {
                    assertThat(coroutineContext[TestElement]?.value).isEqualTo("hello")
                }
            }
        }
    }

    @Test
    fun `currentPropagatedContext keeps only PropagatedContextElement instances`() {
        runBlocking {
            withContext(TestElement("kept") + CoroutineName("dropped")) {
                val propagated = currentPropagatedContext()
                assertThat(propagated[TestElement]?.value).isEqualTo("kept")
                assertThat(propagated[CoroutineName]).isNull()
            }
        }
    }

    @Test
    fun `currentPropagatedContext is empty when nothing was installed`() {
        runBlocking {
            val propagated = currentPropagatedContext()
            assertThat(propagated[TestElement]).isNull()
        }
    }
}

private class TestElement(
    val value: String,
) : PropagatedContextElement {

    companion object Key : CoroutineContext.Key<TestElement>

    override val key: CoroutineContext.Key<TestElement>
        get() = Key
}
