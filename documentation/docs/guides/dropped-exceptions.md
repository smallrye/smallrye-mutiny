---
tags:
- guide
- advanced
---

# How to deal with dropped exceptions?

There are a few corner cases where Mutiny cannot propagate an exception to a `Uni` or a `Multi` subscriber.

Consider the following example:

```java linenums="1"
{{ insert('java/guides/infrastructure/DroppedExceptionTest.java', 'code') }}
```

The `onCancellation().call(...)` method is called when the `Uni` subscription is cancelled.
The returned `Uni` is failed with a `IOException`, but since the subscription itself has been cancelled then there is no way to catch the exception.

By default Mutiny reports such dropped exceptions to the standard error stream along with the corresponding stack trace.
You can change how these exceptions are handled using `Infrastructure.setDroppedExceptionHandler`.

The following logs dropped exceptions to a logger:

```java linenums="1"
{{ insert('java/guides/infrastructure/DroppedExceptionTest.java', 'override-handler') }}
```
