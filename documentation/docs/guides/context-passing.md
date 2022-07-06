---
tags:
- guide
- advanced
---

# Context passing

Mutiny reactive pipelines let data flow from publishers to subscribers.

In the vast majority of cases a publisher shall have _all_ required data, and operators shall perform processing based on item values.
For instance a network request shall be made with all request data known in advance, and response processing shall only depend on the response payload.

That being said there are cases were this is not sufficient, and some data has to be carried along with items.
For instance one intermediary operator in a pipeline may have to make another networked request from which we need to extract some correlation identifier which will be used by another operator down the pipeline.
In such cases one will be tempted to forward tuples consisting of some item value plus some "extra" data.

For such cases Mutiny offers a _subscriber-provided context_, so all operators involved in a subscription can share some form of _implicit data_.

## What's in a context?

A context is a simple key / value, in-memory storage.
Data can be queried, added and deleted from a context, as shown in the following snippet:

```java linenums="1"
{{ insert('java/guides/ContextPassingTest.java', 'contextManipulation') }}
```

`Context` objects are thread-safe, and can be created from sequences of key / value pairs (as shown above), from a Java `Map`, or they can be created empty.

Note that an empty-created context defers its internal storage allocation until the first call to `put`.
You can see `Context` as a glorified `ConcurrentHashMap` delegate, although this is an implementation detail and Mutiny might explore various internal storage strategies in the future.

!!! tip
    
    Contexts shall be primarily used to share transient data used for networked I/O processing such as correlation identifiers, tokens, etc.

    They should not be used as general-purpose data structures that are frequently updated and that hold large amounts of data.

## How to access a context?

Given a `Uni` or a `Multi`, a context can be accessed using the `withContext` operator, as in:

```java linenums="1"
{{ insert('java/guides/ContextPassingTest.java', 'contextSampleUsage') }}
```

This operator builds a sub-pipeline using 2 parameters: the current `Uni` or `Multi` and the context.

!!! important
    
    The function passed to `withContext` is called at subscription time.
    
    This means that the context has not had a chance to be updated by upstream operators yet, so be careful with what you do in the body of that function.

There is another way to access the context by using the `attachContext` method:

```java linenums="1"
{{ insert('java/guides/ContextPassingTest.java', 'contextAttachedSampleUsage') }}
```

This method materializes the context in the regular pipeline items using the wrapper `ItemWithContext` class.
The `get` method provides the item while the `context` method provides the context.

## How to access a context at the pipeline source?

The `Uni` and `Multi` _builder_ methods like `Multi.createFrom()` provide publishers, not operators, so they don't have the `withContext` method.

The first option is to use the `Uni.createFrom().context(...)` or `Multi.createFrom().context(...)` general purpose method to materialize the context:

```java linenums="1"
{{ insert('java/guides/ContextPassingTest.java', 'builderUsage') }}
```

The `context` method takes a function that accepts a `Context` and returns a pipeline.
This is very similar to the `deferred` builder.

If you use an `emitter` builder then for both `Uni` and `Multi` cases the emitter object offers a `context` method to access the context:

```java linenums="1"
{{ insert('java/guides/ContextPassingTest.java', 'emitterUsage') }}
```
