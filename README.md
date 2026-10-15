# Stratastax Ambient Context

[![Stratastax](https://img.shields.io/badge/Product-stratastax.dev-4F46E5?style=flat)](https://stratastax.dev/)
[![](https://img.shields.io/badge/Buijs-Software-blue)](https://buijs.dev/)
[![GitHub](https://img.shields.io/github/license/buijs-dev/klutter?color=black)](https://github.com/buijs-dev/stratastax-ambient-context/blob/main/LICENSE)
[![codecov](https://codecov.io/gh/buijs-dev/stratastax-ambient-context/branch/develop/graph/badge.svg?token=p6nMfXwkgR)](https://codecov.io/gh/buijs-dev/stratastax-ambient-context)
[![CodeScene Average Code Health](https://codescene.io/projects/86126/status-badges/average-code-health)](https://codescene.io/projects/86126)
[![CodeScene System Mastery](https://codescene.io/projects/86126/status-badges/system-mastery)](https://codescene.io/projects/86126)

This module provides a small set of building blocks for propagating "ambient" data
(request IDs, security context, tenant info, etc.) across coroutine, thread, and
reactive (Mutiny) boundaries.

## PropagatedContextElement
Marker interface for `CoroutineContext.Element` that should survive a boundary
crossing (e.g. via [`inheritedUni`](#inheriteduni) or a reactive callback). Any
context element you want propagated must implement this interface.

```kotlin
class RequestIdElement(val id: String) : PropagatedContextElement {
    companion object Key : CoroutineContext.Key<RequestIdElement>
    override val key: CoroutineContext.Key<*> = Key
}
```

## SynchronousContextElement<T>
Abstract base for a `PropagatedContextElement` that also needs to be readable from
non-suspend code. It wraps a `ThreadLocal` and implements `ThreadContextElement`, so
`kotlinx.coroutines` keeps the thread-local in sync on every dispatch.

```kotlin
object RequestIdThreadLocal : ThreadLocal<String?>()

class RequestIdContextElement(id: String) :
    SynchronousContextElement<String?>(RequestIdThreadLocal, id) {
    companion object Key : CoroutineContext.Key<RequestIdContextElement>
    override val key: CoroutineContext.Key<*> = Key
}

// Non-suspend SPI code that cannot read the CoroutineContext directly:
fun currentRequestId(): String? = RequestIdThreadLocal.get()
```

## currentPropagatedContext()
Safely snapshots `PropagatedContextElement` from the current coroutine context,
filtering out coroutine machinery (jobs, dispatchers). Always use this instead of
reading `coroutineContext[Key]` directly when you need to carry context across a
boundary.

```kotlin
suspend fun callDownstream() {
    val ctx = currentPropagatedContext()
    inheritedUni(context = ctx, dispatcher = managedExecutor.asCoroutineDispatcher()) {
        // runs with `ctx` available
        someSuspendCall()
    }
}
```

## AmbientContextContributor & AmbientContextProvider

Two-interface split used for CDI aggregation: each `AmbientContextContributor` bean
supplies one piece of ambient data, and a single `AmbientContextProvider` combines
all registered contributors into one `CoroutineContext` for easy injection. The
split exists to avoid infinite self-recursion when the provider aggregates the
contributors.

```kotlin
@ApplicationScoped
class RequestIdContributor : AmbientContextContributor {
    override fun current(): CoroutineContext =
        RequestIdContextElement(RequestContext.currentId())
}

@ApplicationScoped
class DefaultAmbientContextProvider(
    private val contributors: Instance<AmbientContextContributor>,
) : AmbientContextProvider {
    override fun current(): CoroutineContext =
        contributors.fold(EmptyCoroutineContext as CoroutineContext) { acc, c -> acc + c.current() }
}
```

## inheritedUni()

Runs a suspend block as a Mutiny `Uni` while retaining the given `context`, as a
safer replacement for `io.smallrye.mutiny.coroutines.uni` (which uses a detached
`GlobalScope` and loses context). It does **not** automatically inherit the
enclosing coroutine's context. You need to capture it explicitly with
[`currentPropagatedContext()`](#currentpropagatedcontext) first.

> **Quarkus warning:** the default `dispatcher` (`Dispatchers.Default`) has no
> Quarkus request-scope integration and will break `@RequestScoped`/security
> annotations. Always pass a request-aware dispatcher, e.g.
> `managedExecutor.asCoroutineDispatcher()`.

```kotlin
fun findUser(id: String): Uni<User> =
    inheritedUni(dispatcher = managedExecutor.asCoroutineDispatcher()) {
        userRepository.findById(id)
    }
```

## Related Stratastax projects

- [stratastax-quarkus › quarkus-starter](https://github.com/buijs-dev/stratastax-quarkus/tree/main/quarkus-starter) - propagates the ambient context through every generated resource and service call
- [stratastax-security](https://github.com/buijs-dev/stratastax-security) - carries the security context as an ambient context element
- [stratastax-http](https://github.com/buijs-dev/stratastax-http) - uses it in the reactive idempotency filter
- [stratastax-persistence](https://github.com/buijs-dev/stratastax-persistence) - carries the transaction context as an ambient context element
- [stratastax-style](https://github.com/buijs-dev/stratastax-style) - the Kotlin code style this library is formatted with
