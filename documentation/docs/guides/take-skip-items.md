---
tags:
- guide
- beginner
---

# Take/Skip the first or last items

Multi provides the ability to:

- only forward items from the beginning of the observed multi,
- only forward the last items (and discard all the other ones),
- skip items from the beginning of the multi,
- skip the last items.

These actions are available from the `multi.select()` and `multi.skip()` groups, allowing to, respectively, select and skip
items from upstream.

## Selecting items

The `multi.select().first` method forwards on the _n_ **first** items from the multi.
It forwards that amount of items and then sends the completion signal.
It also cancels the upstream subscription.

```java linenums="1"
{{ insert('java/guides/operators/SelectAndSkipTest.java', 'take-first') }}
```

!!! note
    
    The `select().first()` method selects only the first item.

If the observed multi emits fewer items, it sends the completion event when the upstream completes.

Similarly, The `multi.select().last` operator forwards on the _n_  **last** items from the multi.
It discards all the items emitted beforehand.


```java linenums="1"
{{ insert('java/guides/operators/SelectAndSkipTest.java', 'take-last') }}
```

!!! note
    
    The `select().last()` method selects only the last item.

The `multi.select().first(Predicate)` operator forwards the items while the passed predicate returns `true`:


```java linenums="1"
{{ insert('java/guides/operators/SelectAndSkipTest.java', 'take-while') }}
```

It calls the predicate for each item.
Once the predicate returns `false`, it stops forwarding the items downstream.
It also sends the completion event and cancels the upstream subscription.

Finally,  `multi.select().first(Duration)` operator picks the first items emitted during a given period.
Once the passed duration expires, it sends the completion event and cancels the upstream subscription.
If the observed multi completes before the passed duration, it sends the completion event.


```java linenums="1"
{{ insert('java/guides/operators/SelectAndSkipTest.java', 'take-for') }}
```

## Skipping items

You can also skip items using `multi.skip()`.

The `multi.skip().first(n)` method skips the _n_ **first** items from the multi.
It forwards all the remaining items and sends the completion event when the upstream multi completes.


```java linenums="1"
{{ insert('java/guides/operators/SelectAndSkipTest.java', 'skip-first') }}
```

If the observed multi emits fewer items, it sends the completion event without emitting any items.

!!! note
    
    `skip().last()` drops only the very last item.

Similarly, The `multi.skip().last(n)` operator skips on the _n_  **last** items from the multi:


```java linenums="1"
{{ insert('java/guides/operators/SelectAndSkipTest.java', 'skip-last') }}
```

The `multi.skip().first(Predicate)` operator skips the items while the passed predicate returns `true`:


```java linenums="1"
{{ insert('java/guides/operators/SelectAndSkipTest.java', 'skip-while') }}
```

It calls the predicate for each item.
Once the predicate returns `false`, it stops discarding the items and starts forwarding downstream.

Finally,  `multi.skip().first(Duration)` operator skips the first items for a given period.
Once the passed duration expires, it sends the items emitted after the deadline downstream.
If the observed multi completes before the passed duration, it sends the completion event.


```java linenums="1"
{{ insert('java/guides/operators/SelectAndSkipTest.java', 'skip-for') }}
```
