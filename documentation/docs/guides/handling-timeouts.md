---
tags:
- guide
- intermediate
---

# How to handle timeouts?

Unis are often used to represent asynchronous operations, like making an HTTP call.
So, it's not rare to need to add a timeout or a deadline on this kind of operation.
If we don't get a response (receive an item in the Mutiny lingo) before that deadline, we consider that the operation failed.

We can then recover from this failure by using a fallback value, retrying, or any other failure handling strategy.

To configure a timeout use `Uni.ifNoItem().after(Duration)`:

```java linenums="1"
{{ insert('java/guides/UniTimeoutTest.java', 'code') }}
```

When the deadline is reached, you can do various actions.
First you can simply fail:

```java linenums="1"
{{ insert('java/guides/UniTimeoutTest.java', 'fail') }}
```

A `TimeoutException` is propagated in this case.
So you can handle it specifically in the downstream:

```java linenums="1"
{{ insert('java/guides/UniTimeoutTest.java', 'fail-recover') }}
```

You can also pass a custom exception:

```java linenums="1"
{{ insert('java/guides/UniTimeoutTest.java', 'fail-with') }}
```

Failing and recovering might be inconvenient.
So, you can pass a fallback item or `Uni` directly:

```java linenums="1"
{{ insert('java/guides/UniTimeoutTest.java', 'fallback') }}
```

```java linenums="1"
{{ insert('java/guides/UniTimeoutTest.java', 'fallback-uni') }}
```