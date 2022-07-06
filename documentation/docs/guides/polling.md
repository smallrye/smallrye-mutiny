---
tags:
- guide
- advanced
---

# How to use polling?

There are many poll-based API around us.
Sometimes you need to use these APIs to generate a stream from the polled values.

To do this, use the `repeat()` feature:

```java linenums="1"
{{ insert('java/guides/operators/PollableSourceTest.java', 'code') }}
```

You can also stop the repetition using the `repeat().until()` method which will continue the repetition until the given predicate returns `true`, and/or directly create a `Multi` using `Multi.createBy().repeating()`:

```java linenums="1"
{{ insert('java/guides/operators/PollableSourceTest.java', 'code2') }}
```