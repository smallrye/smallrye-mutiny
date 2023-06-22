---
tags:
- guide
- intermediate
---

# Splitting a Multi into several Multi

It is possible to split a `Multi` into several `Multi` streams.

## Using the split operator

Suppose that we have a stream of strings that represent _signals_, and that we want a `Multi` for each kind of signal:

- `?foo`, `?bar` are _input_ signals,
- `!foo`, `!bar` are _output_ signals,
- `foo`, `bar` are _other_ signals.

To do that, we need a function that maps each item of the stream to its target stream.
The splitter API needs a Java enumeration to define keys, as in:

```java linenums="1"
{{ insert('java/guides/operators/SplitTest.java', 'enum') }}
```

Now we can use the `split` operator that provides a splitter object, and fetch individual `Multi` for each split stream using the `get` method:

```java linenums="1"
{{ insert('java/guides/operators/SplitTest.java', 'splits') }}
```

This prints the following console output:

```
output - a
input - b
output - c
output - d
other - 123
input - e
```

## Notes on using splits

- Items flow when all splits have a subscriber.
- The flow stops when either of the subscribers cancels, or when any subscriber has a no outstanding demand.
- The flow resumes when all splits have a subscriber again, and when all subscribers have outstanding demand.
- Only one subscriber can be active for a given split. Other subscription attempts will receive an error.
- When a subscriber cancels, then a new subscription attempt on its corresponding split can succeed.
- Subscribing to an already completed or errored split results in receiving the terminal signal (`onComplete()` or `onFailure(err)`).
- The upstream `Multi` gets subscribed to when the first split subscription happens, no matter which split it is.
- The first split subscription passes its context, if any, to the upstream `Multi`. It is expected that all split subscribers share the same context object, or the behavior of your code will most likely be incorrect.
