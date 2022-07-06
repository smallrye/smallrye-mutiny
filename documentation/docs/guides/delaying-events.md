---
tags:
- guide
- intermediate
---

# How to delay events?

## Delaying Uni's item

When you have a `Uni`, you can delay the item emission using `onItem().delayIt().by(...)`:

```java linenums="1"
{{ insert('java/guides/operators/DelayTest.java', 'delay-by') }}
```

You pass a duration.
When the item is received, it _waits for_  that duration before propagating it to the downstream consumer.

You can also delay the item's emission based on another _companion_ `Uni`:

```java linenums="1"
{{ insert('java/guides/operators/DelayTest.java', 'delay-until') }}
```

The item is propagated downstream when the `Uni` returned by the function emits an item (possibly `null`).
If the function emits a failure (or throws an exception), this failure is propagated downstream.

## Throttling a Multi

Multi does not have a _delayIt_ operator because applying the same delay to all items is rarely what you want to do.
However, there are several ways to apply a delay in a `Multi`.

First, you can use the `onItem().call()`, which delays the emission until the `Uni` produced the `call` emits an item.
For example, the following snippet delays all the items by 10 ms:

```java linenums="1"
{{ insert('java/guides/operators/DelayTest.java', 'delay-multi') }}
```

In general, you don't want to apply the same delay to all the items.
You can combine `call` with a random delay as follows:

```java linenums="1"
{{ insert('java/guides/operators/DelayTest.java', 'delay-multi-random') }}
```

Finally, you may want to throttle the items.
For example, you can introduce a (minimum) one-second delay between each item.
To achieve this, combine `Multi.createFrom().ticks()` and the multi to throttled:

```java linenums="1"
{{ insert('java/guides/operators/DelayTest.java', 'throttling-multi') }}
```

!!! tip
    
    The `onOverflow().drop()` is used to avoid the _ticks_ to fail if the other stream (`multi`) is too slow.

## Delaying other types of events

We have looked at how to delay items, but you may need to delay other events, such as subscription or failure.
For these, use the `call` approach, and return a `Uni` that delay the event's propagation.
