---
tags:
- tutorial
- beginner
---

# Handling failures

Mutiny provides several operators to handle failures.

Remember, failures are terminal events sent by the observed stream, indicating that something _bad_ happened.
After a failure, no more items are being received.

When such an event is received, you can:

- propagate the failure downstream (default), or
- transform the failure into another failure, or
- recover from it by switching to another stream, passing a fallback item, or completing, or
- retrying (covered in the next guide)

If you don't handle the failure event, it is propagated downstream until a stage handles the failure or reaches the final subscriber.

!!! important

    on `Multi`, a failure cancels the subscription, meaning you will not receive any more items.
    The `retry` operator lets you re-subscribe and continue the reception.

## Observing failures

It can be useful to execute some custom action when a failure happens.
For example, you can log the failure:

```java linenums="1"
{{ insert('java/tutorials/HandlingFailuresTest.java', 'invoke') }}
```

!!! tip
    
    You can also execute an asynchronous action using `onFailure().call(Function<Throwable, Uni<?>)`.
    The received failure will be propagated downstream when the `Uni` produced by the passed function emits its item.

## Transforming failures

Another useful action on failure is to transform the failure into a _more meaningful_ failure.

Typically, you can wrap a low-level failure (like an `IOException`) into a business failure (`ServiceUnavailableException`):

```java linenums="1"
{{ insert('java/tutorials/HandlingFailuresTest.java', 'transform') }}
```

## Recovering using fallback item(s)

In general, upon failure, you want to recover.
The first approach is to recover by replacing the failure with an item:

```java linenums="1"
{{ insert('java/tutorials/HandlingFailuresTest.java', 'recover-item') }}
```

The second approach receives a `Supplier` to compute the fallback item.
For the downstream, it didn't fail; it gets the fallback item instead.

However, don't forget that failures are terminal!
So for `Multi`, the downstream receives the fallback item followed by the completion signal, as no more items can be produced.

## Completing on failure

When observing a `Multi` you can replace the failure with the completion signal:

```java linenums="1"
{{ insert('java/tutorials/HandlingFailuresTest.java', 'recover-completion') }}
```

The downstream won't see the failure, just the completion event.

## Switching to another stream

On failure, you may want to switch to an alternate stream.
When the failure is received, it subscribes to this other stream and propagates the items from this stream instead:

```java linenums="1"
{{ insert('java/tutorials/HandlingFailuresTest.java', 'recover-switch') }}
```

The `recoverWithUni` and `recoverWithMulti` methods replace the failed upstream with the returned stream.

The fallback streams must produce the same type of event as the original upstream.
