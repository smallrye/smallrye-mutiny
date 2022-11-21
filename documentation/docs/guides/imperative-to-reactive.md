---
tags:
- guide
- advanced
---

# From imperative to reactive

If you use Mutiny, there is a good chance you may want to avoid blocking the caller thread.

In a _pure_ reactive application, the application logic is executed on one of the few I/O threads, and blocking one of these would have dramatic consequences.
So, here is the big question: _how do you deal with blocking code?_

Let's imagine you have blocking code (e.g., connecting to a database using JDBC, reading a file from the file system...), and you want to integrate that into your reactive pipelines while avoiding blocking.
You would need to isolate such blocking parts of your code and run these parts on worker threads.

Mutiny provides two operators to customize the threads used to handle events:

* `runSubscriptionOn` - to configure the thread used to execute the code happening at subscription-time
* `emitOn` - to configure the thread used to dispatch events downstream

## Running blocking code on subscription

It is very usual to deal with the blocking call during the subscription.
In this case, the `runSubscription` operator is what you need:

```java linenums="1"
{{ insert('java/guides/integration/ImperativeToReactiveTest.java', 'uni-runSubscriptionOn') }}
```

The code above creates a Uni that will supply the item using a blocking call, here the `invokeRemoteServiceUsingBlockingIO` method.
To avoid blocking the subscriber thread, it uses `runSubscriptionOn` which switches the thread and call `invokeRemoteServiceUsingBlockingIO` on another thread.
Here we pass the default worker thread pool, but you can use your own executor.

!!! tip
    
    What's that default worker pool?
    
    In the previous snippet, you may wonder about `Infrastructure.getDefaultWorkerPool()`.
    Mutiny allows the underlying platform to provide a default worker pool.
    `Infrastructure.getDefaultWorkerPool()` provides access to this pool.

If the underlying platform does not provide a pool, a default one is used.

Note that `runSubscriptionOn` does not subscribe to the Uni.
It specifies the executor to use when a subscription happens.

While the snippet above uses `Uni`, you can also use `runSubscriptionOn` on a `Multi`.

## Executing blocking calls on event

Using `runSubscriptionOn` works when the blocking operation happens at subscription time.
But, when dealing with `Multi` and need to execute blocking operations for each item, you need to use `emitOn`.

While `runSubscriptionOn` runs the subscription on the given executor, `emitOn` configures the executor used to propagate downstream the items, failure and completion events:

```java linenums="1"
{{ insert('java/guides/integration/ImperativeToReactiveTest.java', 'multi-emitOn') }}
```

`emitOn` is also available on `Uni`.

!!! warning

    Be careful as this operator can lead to concurrency problems with non thread-safe objects such as CDI request-scoped beans.
    It might also break reactive-streams semantics with items being emitted concurrently.


