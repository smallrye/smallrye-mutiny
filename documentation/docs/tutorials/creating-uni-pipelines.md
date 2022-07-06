---
tags:
- tutorial
- beginner
---

# Creating `Uni` pipelines

A `Uni` represents a _stream_ that can only emit either an item or a failure event.

You rarely create instances of `Uni` yourself, but, instead, use a reactive client exposing a Mutiny API that provides `Uni` objects. 
That being said, it can be handy at times.

## The Uni type

A `Uni<T>` is a specialized stream that emits only an item or a failure. 
Typically, `Uni<T>` are great to represent asynchronous actions such as a remote procedure call, an HTTP request, or an operation producing a single result.

`Uni<T>` provides many operators that create, transform, and orchestrate `Uni` sequences.

As said, `Uni<T>` emits either an item or a failure. 
Note that the item can be `null,` and the `Uni` API has specific methods for this case. 

Typically, a `Uni<Void>` always emits `null` as item event or a failure if the represented operation fails. 
You can consider the item event as a completion signal indicating the success of the operation.

The offered operators can be used to define a processing pipeline.
The event, either the item or failure, flows in this pipeline, and each operator can process or transform the event.
`Unis` are lazy by nature. 

To trigger the computation, you must have a final subscriber indicating your interest.
The following snippet provides a simple example of pipeline using `Uni`:

```java linenums="1"
{{ insert('java/tutorials/CreatingUniTest.java', 'pipeline') }}
```

## Subscribing to a Uni

!!! important

    Remember: if you don't subscribe, nothing is going to happen.
    What's more, the pipeline is materialized for each _subscription_.

When subscribing to a `Uni`, you can pass an item callback (invoked when the item is emitted), or two callbacks (one receiving the item and one receiving the failure):


```java linenums="1"
{{ insert('java/tutorials/CreatingUniTest.java', 'subscription') }}
```

Note the returned `Cancellable`: this object allows canceling the operation if need be.

## Creating Unis from items

There are many ways to create `Uni` instances. 
Use `Uni.createFrom()` to see all the possibilities.

You can, for instance, create a `Uni` from a known value:

```java linenums="1"
{{ insert('java/tutorials/CreatingUniTest.java', 'simple') }}
```

Every subscriber receives the item `1` just after the subscription.

You can also pass a `Supplier`:

```java linenums="1"
{{ insert('java/tutorials/CreatingUniTest.java', 'supplier') }}
```

The `Supplier` is called for every subscriber. 
So, each of them will get a different value.

## Creating failing Unis

Operations represented by `Unis` can also emit a failure event, indicating that the operation failed.

You can create failed `Uni` instances with:

```java linenums="1"
{{ insert('java/tutorials/CreatingUniTest.java', 'failed') }}
```

## Creating `Uni<Void>`

When the represented operation to not produce a result, you still need a way to indicate the operation's completion.
For this, you need to emit a `null` item:

```java linenums="1"
{{ insert('java/tutorials/CreatingUniTest.java', 'null') }}
```

## Creating Unis using an emitter (_advanced_)

You can create a `Uni` using an emitter.
This approach is useful when integrating callback-based APIs:

```java linenums="1"
{{ insert('java/tutorials/CreatingUniTest.java', 'emitter') }}
```

The emitter can also send a failure.
It can also get notified of cancellation to, for example, stop the work in progress.

## Creating Unis from a CompletionStage (_advanced_)

You can also `Uni` objects from `CompletionStage` / `CompletableFuture`. 
This is useful when integrating with APIs that are based on these types:

```java linenums="1"
{{ insert('java/tutorials/CreatingUniTest.java', 'cs') }}
```

!!! tip

    You can also create a `CompletionStage` from a `Uni` using `uni.subscribe().asCompletionStage()`

