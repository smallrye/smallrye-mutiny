---
tags:
- guide
- beginner
---

# Filtering items from Multi

When observing a `Multi`, you may not want to forward all the received items to the downstream.

Use the `multi.select()` group to select items.

```java linenums="1"
{{ insert('java/guides/operators/FilterTest.java', 'filter') }}
```

To _select_ items passing a given predicate, use `multi.select().where(predicate)`:

`where` accepts a predicate called for each item.
If the predicate returns `true`, the item propagated downstream.
Otherwise, it drops the item.

The predicate passed to `where` is synchronous.
The `when` method provides an asynchronous version:

```java linenums="1"
{{ insert('java/guides/operators/FilterTest.java', 'test') }}
```

`when` accepts a function called for each item.

Unlike `where` where the predicate returns a boolean synchronously, the function returns a `Uni<Boolean>`.
It forwards the item downstream if the `uni` produced by the function emits `true`.
Otherwise, it drops the item.
