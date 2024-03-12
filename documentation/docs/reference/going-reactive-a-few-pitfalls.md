---
tags:
- reference
- beginner
---

# Going reactive: a few pitfalls

Don't get us wrong, reactive programming is a fantastic way to write resource-efficient code!
 
That being said, reactive programming has a learning curve that should not be taken lightly, and in some cases it is safer to write imperative code that you fully comprehend over reactive code that you don't fully grok.

We have assembled a few considerations that we think new users should know before they embark into writing complex reactive business logic.

## Mutiny doesn't auto-magically make your code asynchronous

This is a common source of confusion for new reactive programmers.
Mutiny itself **does not perform any scheduling work**, except for the [`emitOn` and `runSubscriptionOn` operators](../guides/emit-on-vs-run-subscription-on.md).

Consider the following code where we _join_ results from multiple asynchronous operations, materialised by the `Uni<String>`-returning `fetch` method: 

```java linenums="1"
{{ insert('java/guides/reference/PitfallsTest.java', 'noMagicJoin') }}
```

You might think that the `join` operator schedules the calls to `fetch` to be run concurrently, and then collects the results into a list.
This is not how it works!

The `join` operator does subscribe to each `Uni<String>` returned by each call to `fetch`.
When it receives a value, it puts it into a list, and when all values have been received, that list is emitted.
The threads involved here are the ones that emit values in `fetch`.
If `fetch` uses async I/O underneath then you should observe true concurrency, but if `fetch` just emits a value right when the subscription happens then you will merely observe a sequential execution of each call to `fetch`, in order.

## When to prefer `Uni<List<T>>` over `Multi<T>`

The reason why `Multi` exists is to model streams over back-pressured sources.
By conforming to the [Reactive Streams protocol](https://www.reactive-streams.org/), a `Multi` respects the control flow requests from its subscribers, avoiding classic problems such as a fast producer and a slow consumer that can yield to memory exhaustion problems.

That being said, not everything is a stream.
Take the example of relational databases: **databases don't stream!** (for the most parts)

When you do a query such as `SELECT * FROM ABC WHERE INDEX < 123`, you get result rows.
While you might wrap the results in a `Multi<Row>` as a convenience, the network protocol of the database still sends you all `Row` values and is very unlikely to support any notion of back-pressure on a SQL query result.

This is why `Uni<List<Row>>` is in this case a better representation of an asynchronous operation than `Multi<Row>`, because the underlying networked service protocol does not provide you with any back-pressured stream.

## Creating `Uni` and `Multi` from in-memory data might be suspicious

You will find lots of occurrences of creating `Uni`  and `Multi` from in-memory data in this documentation, as in:

```java linenums="1"
{{ insert('java/guides/reference/PitfallsTest.java', 'inMemoryData') }}
```

This is convenient and expected when creating tests and examples, but this should be a strong warning in production.
Indeed, if we have a method such as the following:

```java linenums="1"
{{ insert('java/guides/reference/PitfallsTest.java', 'suspiciousPublisher') }}
```

then it is clear that there is nothing _"reactive"_ in this code _(sadly, you can find such idioms in some well-known "reactive" client libraries, but we digress)_.

As a rule of thumb, if your **initial** publisher does not make any I/O operation and it already has the data available in memory, then it is suspicious:

- if it is a `Uni`, then it does not really model an asynchronous I/O operation because the data is already here, and
- if it is a `Multi` then not only there is no asynchronous I/O operation involved, but there is no need for a back-pressure protocol either (see the previous section).

What is not suspicious however is to create, say, a `Multi` to perform a transformation operation:

```java linenums="1"
{{ insert('java/guides/reference/PitfallsTest.java', 'flatmap-ism') }}
```

