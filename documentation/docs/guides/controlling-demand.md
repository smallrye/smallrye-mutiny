---
tags:
- guide
- advanced
---


# Controlling the demand

## Pacing the demand

A subscription is used for 2 purposes: cancelling a request and demanding batches of items.

The `Multi.paceDemand()` operator can be used to automatically issue requests at certain points in time.

The following example issues requests of 25 items every 100ms:

```java linenums="1"
{{ insert('java/guides/operators/ControlDemandTest.java', 'pacing') }}
```

`FixedDemandPacer` is a simple _pacer_ with a fixed demand and a fixed delay.

You can create more elaborated pacers by implementing the `DemandPacer` interface.
To do so you provide an initial request and a function to evaluate the next request which is evaluated based on the previous request and the number of items emitted since the last request:

```java linenums="1"
{{ insert('java/guides/operators/ControlDemandTest.java', 'custom-pacer') }}
```

The previous example is a custom pacer that doubles the demand and increases the delay for each new request.

## Capping the demand requests

The `capDemandsTo` and `capDemandUsing` operators can be used to cap the demand from downstream subscribers.

The `capDemandTo` operator defines a maximum demand that can flow:

```java linenums="1"
{{ insert('java/guides/operators/ControlDemandTest.java', 'capConstant') }}
```

Here we cap requests to 50 items, so it takes 2 requests to get all 100 items of the upstream range.
The first request of 75 items is capped to a request of 50 items, leaving an outstanding demand of 25 items.
The second request of 25 items is added to the outstanding demand, resulting in a request of 50 items and completing the stream.

You can also define a custom function that provides a capping value based on a custom formula, or based on earlier demand observations:

```java linenums="1"
{{ insert('java/guides/operators/ControlDemandTest.java', 'capFunction') }}
```

Here we have a function that requests 75% of the downstream requests.

Note that the function must return a value `n` that satisfies `(0 < n <= requested)` where `requested` is the downstream demand.

## Pausing the demand

The `Multi.pauseDemand()` operator provides fine-grained control over demand propagation in reactive streams.
Unlike cancellation, which terminates the subscription, pausing allows to suspend demand without unsubscribing from the upstream.
This is useful for implementing flow control patterns where item flow needs to be paused based on external conditions.

### Basic pausing and resuming

The `pauseDemand()` operator works with a `DemandPauser` handle that allows to control the stream:

```java linenums="1"
{{ insert('java/guides/operators/PausingDemandTest.java', 'basic') }}
```

The `DemandPauser` provides methods to:

- `pause()`: Stop propagating demand to upstream
- `resume()`: Resume demand propagation and deliver buffered items
- `isPaused()`: Check the current pause state

Note that a few items may still arrive after pausing due to in-flight requests that were already issued to upstream.

### Starting in a paused state

You can create a stream that starts paused and only begins flowing when explicitly resumed:

```java linenums="1"
{{ insert('java/guides/operators/PausingDemandTest.java', 'initially-paused') }}
```

This is useful when you want to prepare a stream but delay its execution until certain conditions are met.

### Late subscription

By default, the upstream subscription happens immediately even when starting paused.
The `lateSubscription()` option delays the upstream subscription until the stream is resumed:

```java linenums="1"
{{ insert('java/guides/operators/PausingDemandTest.java', 'late-subscription') }}
```

### Buffer strategies

When a stream is paused, the operator stops requesting new items from upstream.
However, items that were already requested (due to downstream demand) may still arrive.
Buffer strategies control what happens to these in-flight items.

The `pauseDemand()` operator supports three buffer strategies: `BUFFER` (default), `DROP`, and `IGNORE`.
Configuring any other strategy will throw an `IllegalArgumentException`.

#### BUFFER strategy (default)

Already-requested items are buffered while paused and delivered when resumed:

```java linenums="1"
{{ insert('java/guides/operators/PausingDemandTest.java', 'buffer-strategy') }}
```

You can configure the buffer size:

- `bufferUnconditionally()`: Unbounded buffer
- `bufferSize(n)`: Buffer up to `n` items, then fail with buffer overflow

When the buffer overflows, the stream fails with an `IllegalStateException`.

**Important**: The buffer only holds items that were already requested from upstream before pausing.
When paused, no new requests are issued to upstream, so the buffer size is bounded by the outstanding demand at the time of pausing.

#### DROP strategy

Already-requested items are dropped while paused:

```java linenums="1"
{{ insert('java/guides/operators/PausingDemandTest.java', 'drop-strategy') }}
```

Items that arrive while paused are discarded, and when resumed, the stream continues requesting fresh items.

#### IGNORE strategy

Already-requested items continue to flow downstream while paused.
This strategy doesn't use any buffers.
It only pauses demand from being issued to upstream, but does not pause the flow of already requested items.

### Buffer management

When using the BUFFER strategy, you can inspect and manage the buffer:

```java linenums="1"
{{ insert('java/guides/operators/PausingDemandTest.java', 'buffer-management') }}
```

The `DemandPauser` provides:

- `bufferSize()`: Returns the current number of buffered items
- `clearBuffer()`: Clears the buffer (only works while paused), returns `true` if successful

