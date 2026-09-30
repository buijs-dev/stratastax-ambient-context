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

import io.smallrye.mutiny.Uni
import io.smallrye.mutiny.coroutines.asUni
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async

/**
 * Executes a suspend block as a Mutiny [Uni] while retaining ambient context elements.
 *
 * Functions as a safer replacement for `io.smallrye.mutiny.coroutines.uni`, preserving specified
 * [context] elements across thread boundaries by bypassing Mutiny's detached `GlobalScope`.
 *
 * **Warning: Context Isolation:** Each call creates a brand-new coroutine scope and does **NOT**
 * automatically inherit context from an enclosing coroutine. You must explicitly capture context
 * beforehand (e.g., via [currentPropagatedContext]) and pass it into [context] when crossing
 * boundaries (such as reactive callbacks).
 *
 * **Warning: Quarkus & Request Contexts:** [dispatcher] defaults to [Dispatchers.Default], which
 * lacks Quarkus request-scope integration. Using the default dispatcher will break `@RequestScoped`
 * or security annotations (causing `ContextNotActiveException`). Quarkus callers must explicitly
 * pass an appropriate executor dispatcher (e.g., `managedExecutor.asCoroutineDispatcher()`).
 *
 * **Note about Direct Primitives:** Built directly on `CoroutineScope.async` and `Deferred.asUni`
 * rather than delegating to Mutiny's precompiled `uni { }` wrapper to avoid context loss issues
 * with newer `kotlinx-coroutines` versions.
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun <T> inheritedUni(
    context: CoroutineContext = EmptyCoroutineContext,
    dispatcher: CoroutineContext = Dispatchers.Default,
    block: suspend () -> T,
): Uni<T> {
    val scope = CoroutineScope(dispatcher + context)
    return scope.async { block() }.asUni()
}
