---
hide:
- navigation
- toc
title: Home
---

# Mutiny -- Intuitive Event-Driven Reactive Programming Library for Java

```java
Uni<String> request = makeSomeNetworkRequest(params);

request.ifNoItem().after(ofMillis(100))
    .failWith(() -> new TooSlowException("💥"))
    .onFailure(IOException.class).recoverWithItem(fail -> "📦")
    .subscribe().with(
        item -> log("👍 " + item),
         err -> log(err.getMessage())
    );
```

[:fontawesome-solid-download: Get Mutiny {{ attributes.versions.mutiny }}](tutorials/getting-mutiny.md){ .md-button .md-button--primary }

<div class="frontpage-grid" markdown>

<div markdown>
## :material-lightning-bolt: Event-Driven

Mutiny places _events_ at the core of its design.

With Mutiny, you observe _events_, _react_ to them, and create elegant and readable processing pipelines.

A PhD in functional programming is not required.
</div>

<div markdown>
## :material-routes: Navigable

Even with smart code completion, classes with hundreds of methods are confusing.

Mutiny provides a navigable and explicit API driving you towards the operator you need.
</div>

<div markdown>
## :material-cogs: Non-blocking I/O

Mutiny is the perfect companion to tame the asynchronous nature of applications with non-blocking I/O.

Compose operations in a declarative fashion, transform data, enforce progress, recover from failures and more.
</div>

<div markdown>
## :material-home: Quarkus and Vert.x native

Mutiny is integrated in [Quarkus](https://quarkus.io) where every reactive API uses Mutiny, and [Eclipse Vert.x](https://vertx.io) clients are made available using Mutiny bindings.

Mutiny is however an independent library that can ultimately be used in any Java application.
</div>

<div markdown>
## :material-message-fast: Made for an asynchronous world

Mutiny can be used in any asynchronous application such as event-driven microservices, message-based applications, network utilities, data stream processing, and of course... reactive applications!
</div>

<div markdown>
## :material-bridge: Reactive Converters Built-In

Mutiny is based on the [Reactive Streams protocol](https://www.reactive-streams.org/) and [Java Flow](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/concurrent/Flow.html), and so it can be integrated with any other reactive programming library.

In addition, it proposes _converters_ to interact with other popular libraries.
</div>

</div>
