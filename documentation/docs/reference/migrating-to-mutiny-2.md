---
tags:
- reference
- beginner
---

# Migrating to Mutiny 2

The upgrade is transparent for most code bases that _use_ Mutiny in applications (e.g., Quarkus applications).

## Highlights

- Mutiny 2 is a major release with source and binary incompatible changes to the Mutiny `0.x` and `1.x` series.
- The main highlight of Mutiny 2 is that it is now based on top of the `java.util.concurrent.Flow` APIs instead of the legacy _Reactive Streams APIs_.
- The `Flow` APIs have been part of the JDK since Java 9, and they are the modern _Reactive Streams APIs_.
- Mutiny remains a faithful implementation of the _Reactive Streams_ specification and passes the `Flow` variant of the _Reactive Streams TCK_.
- Deprecated APIs in Mutiny `1.x` have been removed, and experimental APIs have been promoted. 

## Impact of the switch from legacy Reactive Streams APIs to JDK Flow

- The `Flow` types are isomorphic to the legacy _Reactive Streams API_ types.
- We recommend that you migrate to `Flow` in your own code bases.
- You should encourage third-party libraries to migrate to `Flow`.
- You can always use _adapters_ to go back and forth between `Flow` and legacy _Reactive Streams_ types. 

### General guidelines

- If your code _only uses_ `Uni` and `Multi` (i.e., not `org.reactivestreams.Publisher`), then you will be source-compatible with Mutiny 2. You should still recompile and check that your test suites pass.
- If you expose `Multi` as a `org.reactivestreams.Publisher` then you will either need an _adapter_ (see below) or migrate to `java.util.concurrent.Flow.Publisher`.
- If you interact with `org.reactivestreams.Publisher` publishers and you can't migrate them to `java.util.concurrent.Flow.Publisher` (e.g., because it is a third-party library), then you will need an _adapter_. Please encourage third-party libraries to migrate to `Flow`.

### Adapters between Flow and legacy Reactive Streams APIs

- We recommend using the adapters from the [Mutiny Zero project](https://smallrye.io/smallrye-mutiny-zero).
    - The Maven coordinates are `groupId: io.smallrye.reactive`, `artifactId: mutiny-zero-flow-adapters`
    - Use `AdaptersToFlow` to convert from _Reactive Streams_ types to `Flow` types, and
    - Use `AdaptersToReactiveStreams` to convert `Flow` types to _Reactive Streams_ types.
- The Mutiny Zero adapters have virtually zero overhead.

## Other API changes

### Deprecated API removals

- `Uni` and `Multi` `onSubscribe()` group is now `onSubscription()`.
- `AssertSubscriber.await()` has been replaced by event-specific methods (items, failure, completion, etc).
- The _RxJava 2_ integration module has been discarded (only RxJava 3 is now supported). 

### Experimental API promotions

- `Uni` and `Multi` subscription-bound contexts.
- `Uni.join()` publisher.
- `.ifNoItem()` timeout operators.
- `Uni` and `Multi` spies.
- `capDemandsUsing()` and `paceDemand()` request management operators.
- `Multi` `replay()` operator.
