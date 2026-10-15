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

import kotlinx.coroutines.ThreadContextElement
import kotlin.coroutines.CoroutineContext

/**
 * A [PropagatedContextElement] that allows synchronous access to context data from non-suspend
 * code.
 *
 * Provides an opt-in escape hatch for rare non-suspend SPI boundaries that cannot read coroutine
 * contexts directly via `coroutineContext[Key]` or [currentPropagatedContext].
 *
 * **Warning about Thread Local Safety:** Implements [ThreadContextElement] rather than raw
 * `ThreadLocal` calls. `kotlinx.coroutines` automatically invokes [updateThreadContext] and
 * [restoreThreadContext] on every dispatch, ensuring the underlying [threadLocal] remains accurate
 * across thread hops (e.g., `Dispatchers.Default` or reactive callbacks).
 *
 * **Note about Design Choice (`ThreadLocal` vs `ScopedValue`):** Uses `ThreadLocal` intentionally
 * instead of Java's `ScopedValue`. `ScopedValue` requires lexical scoping, which does not fit
 * [ThreadContextElement]'s split lifecycle callbacks. Cleanups are guaranteed on every dispatch
 * without `InheritableThreadLocal` performance overhead.
 */
abstract class SynchronousContextElement<T>(
    private val threadLocal: ThreadLocal<T>,
    private val value: T,
) : PropagatedContextElement,
    ThreadContextElement<T> {

    override fun updateThreadContext(context: CoroutineContext): T {
        val previous = threadLocal.get()
        threadLocal.set(value)
        return previous
    }

    override fun restoreThreadContext(
        context: CoroutineContext,
        oldState: T,
    ) {
        threadLocal.set(oldState)
    }
}
