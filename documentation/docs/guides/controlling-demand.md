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
