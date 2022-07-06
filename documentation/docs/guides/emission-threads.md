---
tags:
- guide
- intermediate
---

# How to change the emission thread?

Except indicated otherwise, Mutiny invokes the next _stage_ using the thread emitting the event from upstream.
So, in the following code, the _transform_ stage is invoked from the thread emitting the event.

```java linenums="1"
{{ insert('java/guides/operators/EmitOnTest.java', 'example') }}
```

You can switch to another thread using the `emitOn` operator.
The `emitOn` operator lets you switch the thread used to dispatch (upstream -> downstream) events, so items, failure and completion events.
Just pass the _executor_ you want to use.

```java linenums="1"
{{ insert('java/guides/operators/EmitOnTest.java', 'code') }}
```

!!! note
    
    You cannot pass a specific thread, but you can implement a simple `Executor` dispatching on that specific thread, or use a _single threaded executor_.