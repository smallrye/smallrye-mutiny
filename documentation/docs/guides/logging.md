---
tags:
- guide
- beginner
---

# Logging events

Both `Uni` and `Multi` offer a `log` operator that can be used to trace events as they flow through operators.

Mutiny does not make any assumption on _how_ logging is defined, and does not rely on any specific logging API.

## Using a logging operator

The `log` method comes in 2 forms: one that takes an identifier and one that derives the identifier from the upstream class:

```java linenums="1"
{{ insert('java/guides/infrastructure/OperatorLoggingTest.java', 'log') }}
```

Here the `log` operator traces all events between the `onItem().transform(...)` operator and the subscriber, as in the following output:

```
11:01:48.709 [main] INFO Multi.MultiMapOp.0 - onSubscription()
11:01:48.711 [main] INFO Multi.MultiMapOp.0 - request(9223372036854775807)
11:01:48.711 [main] INFO Multi.MultiMapOp.0 - onItem(10)
>>> 10
11:01:48.711 [main] INFO Multi.MultiMapOp.0 - onItem(20)
>>> 20
11:01:48.711 [main] INFO Multi.MultiMapOp.0 - onItem(30)
>>> 30
11:01:48.711 [main] INFO Multi.MultiMapOp.0 - onCompletion()
```

There are a few things to note here:

1. we are logging on a `Multi`, so the logging event is prefixed with `Multi` (and `Uni` in the case of a... `Uni`), and
2. since we did not specify any identifier in the `log` method call, `MultiMapOp` has been derived from the preceding operator (non-qualified) class name, and
3. since there can be multiple subscriptions an integer is appended to the identifier (`0`, `1`, `2`, ...).

## Defining logging

What happens when events are being logged is defined with the `Infrastructure` class.
Events are written by default to the standard console output in a format similar to:

```
[--> Multi.MultiMapOp.0 | onSubscription()
[--> Multi.MultiMapOp.0 | request(9223372036854775807)
[--> Multi.MultiMapOp.0 | onItem(10)
[--> Multi.MultiMapOp.0 | onItem(20)
[--> Multi.MultiMapOp.0 | onItem(30)
[--> Multi.MultiMapOp.0 | onCompletion()
```

The following is an example of configuring logging with http://www.slf4j.org[SLF4J]:

```java linenums="1"
{{ insert('java/guides/infrastructure/OperatorLoggingTest.java', 'set-logger') }}
```

!!! tip
    
    Note that this is only useful to do when embedding Mutiny in your own stack, some frameworks like [Quarkus](https://quarkus.io) will already have defined the correct logging strategy.
