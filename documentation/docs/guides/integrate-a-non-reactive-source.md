---
tags:
- guide
- advanced
---

# How can I create a Multi from a non-reactive source?

The `UnicastProcessor` is an implementation of `Multi` that lets you enqueue items in a queue.

The items are then dispatched to the subscriber using the request protocol.
While this pattern is against the idea of back-pressure, it lets you connect sources of data that do not support back-pressure with your subscriber.

In the following example, the `UnicastProcessor` is used by a thread emitting items.
These items are enqueued in the processor and replayed when the subscriber is connected, following the request protocol.

```java linenums="1"
{{ insert('java/guides/operators/UnicastProcessorTest.java', 'code') }}
```

By default, the `UnicastProcessor` uses an unbounded queue.
You can also pass a fixed size queue that would reject the items once full.