---
tags:
- tutorial
- beginner
---

# Retrying on failures

It is common to want to retry if something terrible happened.

You can retry upon failure.
The [How does retry... retries](https://quarkus.io/blog/uni-retry/) blog post provides a more detailed overview of the retry mechanism.

!!! note
    
    If despite multiple attempts, it still fails, the failure is propagated downstream.

## Retry multiple times

To retry on failure, use `onFailure().retry()`:


```java linenums="1"
{{ insert('java/tutorials/RetryTest.java', 'retry-at-most') }}
```

You pass the number of retries as a parameter.

!!! important

    While `.onFailure().retry().indefinitely()` is available, it may never terminate, so use it with caution.

## Introducing delays

By default, `retry` retries immediately.
When using remote services, it is often better to delay a bit the attempts.

Mutiny provides a method to configure an exponential backoff: a growing delay between retries.
Configure the exponential backoff as follows:

```java linenums="1"
{{ insert('java/tutorials/RetryTest.java', 'retry-backoff') }}
```

The backoff is configured with the initial and max delay.
Optionally, you can also configure a jitter to add a pinch of randomness to the delay.

When using exponential backoff, you may not want to configure the max number of attempts (`atMost`), but a deadline.
To do so, use either `expireIn` or `expireAt`.

## Deciding to retry

As an alternative to `atMost`, you can also use `until`.
This method accepts a predicate called after every failure.

If the predicate returned `true,` it retries.
Otherwise, it stops retrying and propagates the last failure downstream:

```java linenums="1"
{{ insert('java/tutorials/RetryTest.java', 'retry-until') }}
```
