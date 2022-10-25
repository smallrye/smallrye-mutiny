---
tags:
- guide
- advanced
---

# Spying on events

Spies are useful when you need to track which _events_ flow into a `Uni` or a `Multi`.
Spies can track events from groups such as `onItem()`, `onFailure()`, `onSubscribe()`, etc.

The `io.smallrye.mutiny.helpers.spies.Spy` interface offers factory methods to spy on selected groups, or even on all groups.

## Spying selected groups

The following example spies on requests and completion group events:

```java linenums="1"
{{ insert('java/guides/SpiesTest.java', 'selected') }}
```

The standard output stream shall display the following text:

```
1
2
3
Number of requests: 9223372036854775807
Completed? true
```

The number of requests corresponds to `Long.MAX_VALUE`, and a completion event was sent.

!!! important
    
    It is important to note that spies observe and report events for all subscribers, not just one in particular.
    
    You should call the `.reset()` method on a given spy to resets its statistics such as the invocation count.

## Spying all groups

You can take advantage of a _global spy_ if you are interested in all event groups:

```java linenums="1"
{{ insert('java/guides/SpiesTest.java', 'global') }}
```

Running the snippet above gives the following output:

```
1
2
3
Number of requests: 9223372036854775807
Cancelled? false
Failure? null
Items: [1, 2, 3]
```

!!! warning

    Tracking `onItem()` events on a `Multi` requires storing all items into a list, which can yield an out-of-memory
    exception with large streams.

    In such cases consider using `Spy.onItem(multi, false)` to obtain a spy that does not store items, but that can
    still report data such as the number of received events (see `spy.invocationCount()`).