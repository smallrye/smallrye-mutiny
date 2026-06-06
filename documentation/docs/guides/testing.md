---
tags:
- guide
- beginner
---

# How can I write unit / integration tests?

Mutiny provides subscribers for `Uni` and `Multi` offering helpful assertion methods.
You can use them to test pipelines.

## Testing a Uni

Here is an example to test a `Uni`:

```java linenums="1"
{{ insert('java/guides/TestSubscribersTest.java', 'uni') }}
```

You can also use predicate-based assertions and item inspection:

```java linenums="1"
{{ insert('java/guides/TestSubscribersTest.java', 'uni-predicate') }}
```

## Testing a Multi

Testing a `Multi` pipeline is similar:

```java linenums="1"
{{ insert('java/guides/TestSubscribersTest.java', 'multi') }}
```

Predicate-based assertions work on `Multi` too:

```java linenums="1"
{{ insert('java/guides/TestSubscribersTest.java', 'multi-predicate') }}
```

## Declarative verification with AssertMulti

For multi-item streams, `AssertMulti` provides a declarative step-by-step verifier.
Build a sequence of expectations, then call `verify()`:

```java linenums="1"
{{ insert('java/guides/TestSubscribersTest.java', 'assert-multi') }}
```

You can control demand explicitly for backpressure testing:

```java linenums="1"
{{ insert('java/guides/TestSubscribersTest.java', 'assert-multi-demand') }}
```

## Testing failures

The assertions do not just focus on _good_ outcomes, you can also test failures as in:

```java linenums="1"
{{ insert('java/guides/TestSubscribersTest.java', 'failing') }}
```

With `AssertMulti`:

```java linenums="1"
{{ insert('java/guides/TestSubscribersTest.java', 'assert-multi-failure') }}
```
