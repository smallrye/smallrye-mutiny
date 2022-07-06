---
tags:
- tutorial
- beginner
---

# Creating `Multi` pipelines

A `Multi` represents a _stream_ of data.
A stream can emit 0, 1, n, or an infinite number of items.

You will rarely create instances of `Multi` yourself but instead use a reactive client that exposes a Mutiny API.
Still, just like `Uni` there exists a rich API for creating `Multi` objects.

## The Multi type

A `Multi<T>` is a data stream that:

- emits `0..n` item events
- emits a failure event
- emits a completion event for bounded streams

!!! warning

    Failures are terminal events: after having received a failure no further item will be emitted.

`Multi<T>` provides many operators that create, transform, and orchestrate `Multi` sequences.
The operators can be used to define a processing pipeline.
The events flow in this pipeline, and each operator can process or transform the events.

`Multis` are lazy by nature.
To trigger the computation, you must subscribe.

The following snippet provides a simple example of pipeline using `Multi`:

```java linenums="1"
{{ insert('java/tutorials/CreatingMultiTest.java', 'pipeline') }}
```

## Subscribing to a Multi

Remember, if you don't subscribe, nothing is going to happen.
Also, the pipeline is materialized for each _subscription_.

When subscribing to a `Multi,` you can pass an item callback (invoked when the item is emitted), or pass two callbacks, one receiving the item and one receiving the failure, or three callbacks to handle respectively the item, failure and completion events.

```java linenums="1"
{{ insert('java/tutorials/CreatingMultiTest.java', 'subscription') }}
```

Note the returned `Cancellable`: this object allows canceling the stream if need be.

## Creating Multi from items

There are many ways to create `Multi` instances.
See `Multi.createFrom()` to see all the possibilities.

For instance, you can create a `Multi` from known items or from an `Iterable`:

```java linenums="1"
{{ insert('java/tutorials/CreatingMultiTest.java', 'simple') }}
```

Every subscriber receives the same set of items (`1`, `2`... `5`) just after the subscription.

You can also use `Suppliers`:

```java linenums="1"
{{ insert('java/tutorials/CreatingMultiTest.java', 'supplier') }}
```

The `Supplier` is called for every subscriber, so each of them will get different values.

!!! tip

    You can create ranges using `Multi.createFrom().range(start, end)`.

## Creating failing Multis

Streams can also fail.

Failures are used to indicate to the downstream subscribers that the source encountered a terrible error and cannot continue emitting items.
Create failed `Multi` instances with:

```java linenums="1"
{{ insert('java/tutorials/CreatingMultiTest.java', 'failed') }}
```

## Creating empty Multis

Unlike `Uni,` `Multi` streams don't send `null` items (this is forbidden in _reactive streams_).

Instead `Multi` streams send completion events indicating that there are no more items to consume.
Of course, the completion event can happen even if there are no items, creating an empty stream.

You can create such a stream using:

```java linenums="1"
{{ insert('java/tutorials/CreatingMultiTest.java', 'empty') }}
```

## Creating Multis using an emitter (_advanced_)

You can create a `Multi` using an emitter.
This approach is useful when integrating callback-based APIs:

```java linenums="1"
{{ insert('java/tutorials/CreatingMultiTest.java', 'emitter') }}
```

The emitter can also send a failure.
It can also get notified of cancellation to, for example, stop the work in progress.

## Creating Multis from _ticks_ (_advanced_)

You can create a stream that emit a _ticks_ periodically:

```java linenums="1"
{{ insert('java/tutorials/CreatingMultiTest.java', 'ticks') }}
```

The downstream receives a `long,` which is a counter.
For the first tick, it's 0, then 1, then 2, and so on.

## Creating Multis from a generator (_advanced_)

You can create a stream from some _initial state_, and a _generator function_:

```java linenums="1"
{{ insert('java/tutorials/CreatingMultiTest.java', 'generator') }}
```

The initial state is given through a supplier (here `() -> 1`).
The generator function accepts 2 arguments:

- the current state,
- an emitter that can emit a new item, emit a failure, or emit a completion.

The generator function return value is the next _current state_.
Running the previous example gives the following number suite: `{2, 4, 7, 11, 17, 26, 40, 61}`.


