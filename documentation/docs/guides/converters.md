---
tags:
- guide
- advanced
---

# Using other reactive programming libraries

You may need to integrate libraries exposing an API using other reactive programming libraries such as RX Java or Reactor.
Mutiny has a built-in conversion mechanism to ease that integration.

## Picking the right dependency

You need to add another dependency to access the converters.
Each artifact contains the converters for a specific reactive library.
Pick the right one and add it to your project:

=== "Reactor"

    ```xml
    <!-- Mutiny <-> Reactor -->
    <dependency>
        <groupId>io.smallrye.reactive</groupId>
        <artifactId>mutiny-reactor</artifactId>
        <version>{{ attributes.versions.mutiny }}</version>
    </dependency>
    ```

=== "RxJava 3"

    ```xml
    <!-- Mutiny <-> RX Java 3 -->
    <dependency>
        <groupId>io.smallrye.reactive</groupId>
        <artifactId>mutiny-rxjava3</artifactId>
        <version>{{ attributes.versions.mutiny }}</version>
    </dependency>
    ```

## Integration with Project Reactor

[Project Reactor](https://projectreactor.io/) is a popular reactive programming library.
It offers two types: `Mono` and `Flux,` both implementing Reactive Stream `Publisher`.

To use the Reactor `<->` Mutiny converter, add the following imports to your class:

```java
import io.smallrye.mutiny.converters.multi.MultiReactorConverters;
import io.smallrye.mutiny.converters.uni.UniReactorConverters;
```

### Converting a Flux or a Mono into a Multi

Both `Flux` and `Mono` implement `Publisher`.
As a result, we can use the Reactive Streams interoperability to convert instances from `Flux<T>` and `Mono<T>` to `Multi<T>`:

```java linenums="1"
{{ insert('java/guides/integration/ReactorTest.java', 'reactor-multi-create') }}
```

!!! attention
    
    Reactor still uses the legacy Reactive Streams APIs instead of `java.util.concurrent.Flow`, so you need to perform an adaptation.
    
    We recommend using the [Mutiny Zero Flow Adapters library](https://smallrye.io/smallrye-mutiny-zero/) as in these examples (Maven coordinates `io.smallrye.reactive:mutiny-zero-flow-adapters`).

### Converting a Flux or a Mono into a Uni

As you can create `Uni` from a `Publisher`, the same approach can be used to create `Uni` instances:

```java linenums="1"
{{ insert('java/guides/integration/ReactorTest.java', 'reactor-uni-create') }}
```

When a `Flux` or `Mono` sends the _completion_ event without having emitted any item, the resulting `Uni` emits `null`.

When converting a `Flux` to `Uni`, the resulting `Uni` emits the first item.
After that emission, it cancels the subscription to the `Flux`.

### Converting a Multi into a Flux or Mono

Converting a `Multi` into a `Flux` or a `Mono` uses the Reactive Streams interoperability:

```java linenums="1"
{{ insert('java/guides/integration/ReactorTest.java', 'reactor-create-multi') }}
```

### Converting a Uni into a Flux or Mono

Converting a `Uni` into a `Flux` or a `Mono` requires a converter, as `Uni` does not implement Reactive Streams.

```java linenums="1"
{{ insert('java/guides/integration/ReactorTest.java', 'reactor-create-uni') }}
```

If the `Uni` emits `null`, it sends the _completion_ event.

### Using converter instead of Reactive Streams

While Reactive Streams interoperability is convenient, Mutiny also provides converters to create `Flux` and `Mono` from `Uni` and `Multi`:

```java linenums="1"
{{ insert('java/guides/integration/ReactorTest.java', 'uni-export') }}
{{ insert('java/guides/integration/ReactorTest.java', 'multi-export') }}
```

## Integration with RX Java 3

RxJava is another popular reactive programming library.
It offers 5 types: `Completable` (no item), `Single` (one item), `Maybe` (0 or 1 item), `Observable` (multiple items), `Flowable` (multiple items, implements Reactive Stream `Publisher`).

To use the RxJava `<->` Mutiny converters, add the following imports to your class:

```java
import io.smallrye.mutiny.converters.multi.MultiRx3Converters;
import io.smallrye.mutiny.converters.uni.UniRx3Converters;
```

### Converting an Observable or a Flowable into a Multi

Both `Observable` and `Flowable` are item streams.
However, `Observable` does not implement `Publisher` and so does not have back-pressure support.

To create `Multi` from an `Observable,` you need a specific converter:

```java linenums="1"
{{ insert('java/guides/integration/RxJavaTest.java', 'rx-multi-create-observable') }}
```

Converting a `Flowable` is easier, as it's a `Publisher`:

```java linenums="1"
{{ insert('java/guides/integration/RxJavaTest.java', 'rx-multi-create-flowable') }}
```

!!! attention

    Like Reactor, RxJava still uses the legacy Reactive Streams APIs instead of `java.util.concurrent.Flow`, so you need to perform an adaptation.
    
### Converting a Completable, Single or Maybe into a Multi

To create a `Multi` from a `Completable,` `Single` or `Maybe` you need specific converters, as none of these types implement Reactive Streams.

```java linenums="1"
{{ insert('java/guides/integration/RxJavaTest.java', 'rx-multi-create-single') }}
```

- Creating a `Multi` from a `Completable` always produces a `Multi<Void>` that only emits the _completion_ or _failure_ event.
- Creating a `Multi` from a `Single` produces a `Multi`. That `Multi` emits the item and then completes it.
- Creating a `Multi` from a `Maybe` produces a `Multi`. That `Multi`  emits the item (if any) and then completes it.
  If the `Maybe` is empty, then the created `Multi` emits the _completion_ event.

When a `Completable,` `Single,` or `Maybe` emits a failure, then the resulting `Multi` emits that failure.

### Converting an Observable or a Flowable into a Uni

To create a `Uni` from an `Observable,` you need to use a specific converter:

```java linenums="1"
{{ insert('java/guides/integration/RxJavaTest.java', 'rx-uni-create-observable') }}
```

The creation from a `Flowable` can be done using the Reactive Streams interoperability:

```java linenums="1"
{{ insert('java/guides/integration/RxJavaTest.java', 'rx-uni-create-flowable') }}
```

In both cases, it cancels the subscription to the `Flowable` or `Observable` after receiving the first item.
If the `Flowable` or `Observable` completes without items, the `Uni` emits a `null` item.

### Converting a Completable, Single or Maybe into a Uni

To create a `Uni` from a `Completable,` `Single,` or `Maybe`, you need to use a specific converter:

```java linenums="1"
{{ insert('java/guides/integration/RxJavaTest.java', 'rx-uni-create-single') }}
```

Converting a `Completable` to a `Uni` always produces a `Uni<Void>,` that emits either `null` once the `Completable` completes or the failure if it fails.
The `Maybe` to `Uni` conversion emits a `null` item if the `Maybe` completes without an item.

### Converting a Multi into a RX Java objects

The conversion from a `Multi` to the various RX Java objects is done using converters:

```java linenums="1"
{{ insert('java/guides/integration/RxJavaTest.java', 'create-rx-from-multi') }}
```

The creation of a `Completable` from a `Multi` discards all the items emitted by the `Multi`.
It only forwards the _completion_ or _failure_ event.

Converting a `Multi` into a `Single` returns a `Single<Optional<T>>,` as the `Multi` may complete without items.
You can also produce a `Single<T>` and emit a _failure_ event if the `Multi` completes without items.
You can configure the thrown exception using `onEmptyThrow.`

!!! tip

    You can also create a `Flowable` from a `Multi` using: `Flowable.fromPublisher(multi)`.

### Converting a Uni into a RX Java type

Similarly to the conversion from a `Multi` into an RX Type, converting a `Uni` requires a converter:

```java linenums="1"
{{ insert('java/guides/integration/RxJavaTest.java', 'create-rx-from-uni') }}
```

The creation of a `Completable` from a `Uni` discards the item and sends the _completion_ signal after emission.

Converting a `Uni` into a `Single` returns a `Single<Optional<T>>,` as the `Uni` may emit `null.`
You can also produce a `Single<T>` and emits a _failure_ event if the `Uni` sends `null.`
Configure the failure to forward using `failOnNull.`

The creation of a `Maybe,` `Flowable,` or an `Observable` from a `Uni` produces an empty `Maybe,` `Flowable,` or `Observable` if the `Uni` emits `null.`
For `Flowable` and `Observable,` if the `Uni` emits a _non-null_ item, that item is emitted, followed immediately by the _completion_ signal.
