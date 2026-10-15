# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

First release of Stratastax Ambient Context: building blocks for propagating ambient data
(request IDs, security context, tenant info, etc.) across coroutine, thread and reactive (Mutiny) boundaries.

- `PropagatedContextElement` to mark coroutine context elements that should survive a boundary crossing.
- `SynchronousContextElement` to make a propagated element readable from non-suspend code through a `ThreadLocal`.
- `currentPropagatedContext()` to snapshot the propagated elements of the current coroutine context.
- `AmbientContextContributor` and `AmbientContextProvider` for CDI aggregation of ambient data.
- `inheritedUni()` to run a suspend block as a Mutiny `Uni` while retaining the given context.

See [README.md](https://github.com/buijs-dev/stratastax-ambient-context/blob/main/README.md) for examples.
