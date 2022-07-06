---
tags:
- guide
- intermediate
---

# Joining several unis

A `Uni` represents an operation that either emits a value or a failure.
Examples of operations that fit into a `Uni` include: HTTP client requests, database `insert` queries, sending messages to a broker, etc.

It is common to trigger several _concurrent_ operations, then _join_ on the results.
For instance you can make HTTP requests to 3 different HTTP APIs, then collect all HTTP responses.
Or you can just take the response from the one who was the fastest.

`Uni` offers the `join` group to assemble all results from a list of `Uni`, pick the first one that terminates, or pick the first one that terminates with a value.

## Joining multiple unis

Given multiple `Uni`, you can join them all and obtain a `Uni` that emits a list of values:

```java linenums="1"
{{ insert('java/guides/operators/UniJoinTest.java', 'join-all') }}
```

The assembled values are in the same order as the list of unis.
The last call to `.andCollectFailures()` specifies that if one or several `Uni` fail, then the failures are assembled in a `CompositeException`.

Sometimes you just want to _fail fast_ if any of the `Uni` fails, and not wait for all unis to terminate:

```java linenums="1"
{{ insert('java/guides/operators/UniJoinTest.java', 'join-all-ff') }}
```

When any `Uni` fails, then the failure is directly forwarded as a failure of `res`.

## Joining on the first Uni

In some cases you do not want to have all the results but just that of the first `Uni` to respond.
There are actually 2 different cases, depending on whether you want the result of the first `Uni` that emits a value, or just the result of the first `Uni` to terminate.

If you want to get the first `Uni` that terminates:

```java linenums="1"
{{ insert('java/guides/operators/UniJoinTest.java', 'join-first') }}
```

If you want to have the first `Uni` that emits a value (and forget the first failures), then:

```java linenums="1"
{{ insert('java/guides/operators/UniJoinTest.java', 'join-first-withitem') }}
```

When all unis fail then `res` fails with a `CompositeException` that reports all failures.

## Using a builder object

There are situations where it can be more convenient to gather the unis to join in an iterative fashion.
For this purpose you can use a builder object, as in:

```java linenums="1"
{{ insert('java/guides/operators/UniJoinTest.java', 'builder') }}
```

The builder offers `joinAll()` and `joinFirst()` methods.
