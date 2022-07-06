---
tags:
- guide
- advanced
---

# How can I integrate Mutiny with my framework?

Sometimes, Mutiny needs to execute tasks on other threads, such as monitoring time or delaying actions.
Most operators relying on such capacity let you pass either a `ScheduledExecutorService` or an `ExecutorService`.

By default, Mutiny uses the a _cached_ thread pool as default executor, that creates new threads as needed, but reuse previously constructed threads when they are available.
A `ScheduledExecutorService` is also created but delegates the execution of the delayed/scheduled tasks to the default executor.

In the case you want to integrate Mutiny with a thread pool managed by a platform, you can configure it using `Infrastructure.setDefaultExecutor()` method:

```java linenums="1"
{{ insert('java/guides/infrastructure/InfrastructureTest.java', 'infra') }}
```

You can configure the default executor using the `Infrastructure.setDefaultExecutor` method:

```java linenums="1"
{{ insert('java/guides/infrastructure/InfrastructureTest.java', 'set-infra') }}
```

!!! tip
    
    If you are using Quarkus, the default executor is already configured to use the Quarkus worker thread pool.
    Logging is also configured correctly.
