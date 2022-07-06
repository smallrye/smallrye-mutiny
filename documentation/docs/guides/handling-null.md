---
tags:
- guide
- beginner
---

# How to handle null?

The `Uni` type can emit `null` as item.

While there are mixed feelings about `null`, it's part of the Java language and so handled in the `Uni` type.

!!! important
    
    `Multi` does not support `null` items as it would break the compatibility with the _Reactive Streams_ protocol.

Emitting `null` is convenient when returning `Uni<Void>`.
However, the downstream must expect `null` as item.

Thus, `Uni` provides specific methods to handle `null` item.
`uni.onItem().ifNull()` lets you decide what you want to do when the received item is `null`:

```java linenums="1"
{{ insert('java/guides/UniNullTest.java', 'code') }}
```

A symmetric group of methods is also available with `ifNotNull` which let you handle the case where the item is _not null_:

```java linenums="1"
{{ insert('java/guides/UniNullTest.java', 'code-not-null') }}
```

!!! important
    
    While supported, emitting `null` should be avoided except for `Uni<Void>`.