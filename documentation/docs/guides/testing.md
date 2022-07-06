---
tags:
- guide
- beginner
---

# How can I write unit / integration tests?

Mutiny provides subscribers for `Uni` and `Multi` offering helpful assertion methods.
You can use them to test pipelines.

Here is an example to test a `Uni`:

```java linenums="1"
{{ insert('java/guides/TestSubscribersTest.java', 'uni') }}
```

Testing a `Multi` pipeline is similar:

```java linenums="1"
{{ insert('java/guides/TestSubscribersTest.java', 'multi') }}
```

The assertions do not just focus on _good_ outcomes, you can also test failures as in:

```java linenums="1"
{{ insert('java/guides/TestSubscribersTest.java', 'failing') }}
```