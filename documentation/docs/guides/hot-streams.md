---
tags:
- guide
- advanced
---

# Hot streams

In a _cold_ stream, the stream is created when one subscriber subscribes to the stream.
So, if no one subscribes, the actual stream is not created, saving resources (that would be wasted because nobody is interested in the items).

In a _hot_ stream, the stream exists before subscribers subscribe.
The stream emits items even if no subscribers observe the stream.
If there are no subscribers, the items are just dropped.
Subscribers only get items emitted after their subscription, meaning that any previous items would not be received.

To create a hot stream, you can use `io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor` that:

- drops items if no subscribers are present,
- forwards items to the set of observing subscribers.

```java linenums="1"
{{ insert('java/guides/operators/BroadcastProcessorTest.java', 'code') }}
```

Note that the `BroadcastProcessor` subscribes to the _hot_ source aggressively and without back-pressure.
However, the `BroadcastProcessor` enforces the back-pressure protocol per subscriber.
If a subscriber is not ready to handle an item emitted by the _hot_ source, an `io.smallrye.mutiny.subscription.BackPressureFailure` is forwarded to this subscriber.
