---
tags:
- guide
- advanced
---

# From reactive to imperative

There are use cases where you need the items in an imperative manner instead of asynchronous.
Typically, when you serve an HTTP request from a worker thread, you can block.

Mutiny provides the ability to block until you get the items.

## Awaiting on Uni's item

When dealing with a `Uni,` you can block and await the item using:

```java linenums="1"
{{ insert('java/guides/integration/ReactiveToImperativeTest.java', 'await') }}
```

This method blocks the caller thread until the observed `uni` emits the item.
Note that the returned item can be `null` if the `uni` emits `null.`
If the `uni` fails, it throws the exception, wrapped in the `CompletionException` for _checked_ exception.

Blocking forever may not be a great idea.
You can use `uni.await().atMost(Duration)` to pass a deadline.
When the deadline is reached, a `TimeoutException` is thrown:

```java linenums="1"
{{ insert('java/guides/integration/ReactiveToImperativeTest.java', 'atMost') }}
```

## Iterating over Multi's items

When dealing with a `Multi,` you may want to iterate over the items using a simple "foreach."
You can achieve this using `multi.subscribe().asIterable()`:

```java linenums="1"
{{ insert('java/guides/integration/ReactiveToImperativeTest.java', 'iterable') }}
```

The returned `iterable` is blocking.
It waits for the next items, and during that time, blocks the caller thread.

The iteration ends once the last item is consumed.
If the `multi` emits a failure, an exception is thrown.

Similar to `asIterable()`, the `asStream` method lets you retrieve a `java.util.stream.Stream`:

```java linenums="1"
{{ insert('java/guides/integration/ReactiveToImperativeTest.java', 'stream') }}
```