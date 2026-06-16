---
tags:
- guide
- intermediate
---

# Kotlin integration

The module `mutiny-kotlin` provides an integration with Kotlin for use with coroutines and convenient language features.

There are extension methods available for converting between Mutiny and Kotlin (coroutine) types.
For implementation details please have also a look to these methods' documentation.

## Dependency coordinates

The coroutine extension functions are shipped in the package `io.smallrye.mutiny.coroutines`.

```kotlin linenums="1"
{{ insert('kotlin/guides/Coroutines.kt', 'importStatements') }}
```

You need to add the following dependency to your project:

=== "Maven"

    ```xml
    <dependency>
        <groupId>io.smallrye.reactive</groupId>
        <artifactId>mutiny-kotlin</artifactId>
        <version>{{ attributes.versions.mutiny }}</version>
    </dependency>
    ```

=== "Gradle (Kotlin)"

    ```kotlin
    implementation("io.smallrye.reactive:mutiny-kotlin:{{ attributes.versions.mutiny }}")
    ```

=== "Gradle (Groovy)"

    ```groovy
    implementation "io.smallrye.reactive:mutiny-kotlin:{{ attributes.versions.mutiny }}"
    ```

## Awaiting a Uni in coroutines

Within a coroutine or suspend function you can easily await Uni events in a suspended way:

```kotlin linenums="1"
{{ insert('kotlin/guides/Coroutines.kt', 'uniAwaitSuspending') }}
```

## Processing a Multi as Flow

The coroutine `Flow` type matches `Multi` semantically, even though it isn't a feature complete reactive streams implementation.
You can process a `Multi` as `Flow` as follows:

```kotlin linenums="1"
{{ insert('kotlin/guides/Coroutines.kt', 'multiAsFlow') }}
```

!!! note

    There's no flow control availabe for Kotlin's `Flow`. Published items are buffered for consumption using a coroutine `Channel`.
    The buffer size and overflow strategy of that `Channel` can be configured using optional arguments:
    `Multi.asFlow(bufferCapacity = Channel.UNLIMITED, bufferOverflowStrategy = BufferOverflow.SUSPEND)`,
    for more details please consult the method documentation.

## Providing a Deferred value as Uni

The other way around is also possible, let a Deferred become a Uni:

```kotlin linenums="1"
{{ insert('kotlin/guides/Coroutines.kt', 'deferredAsUni') }}
```

## Creating a Multi from a Flow

Finally, creating a Multi from a Flow is also possible:

```kotlin linenums="1"
{{ insert('kotlin/guides/Coroutines.kt', 'flowAsMulti') }}
```

## Awaiting Multi results in coroutines

You can consume `Multi` results directly from suspend functions without converting to `Flow` first:

```kotlin linenums="1"
{{ insert('kotlin/guides/MultiAwait.kt', 'multiAwaitList') }}
```

```kotlin linenums="1"
{{ insert('kotlin/guides/MultiAwait.kt', 'multiAwaitFirst') }}
```

```kotlin linenums="1"
{{ insert('kotlin/guides/MultiAwait.kt', 'multiAwaitLast') }}
```

```kotlin linenums="1"
{{ insert('kotlin/guides/MultiAwait.kt', 'multiAwaitEach') }}
```

## Language convenience

### Unit instead of Void (null) value

Kotlin has a special value type `Unit` similar to Java's `Void`.
While regular `Uni<Void>` holds a `null` item, you can get a `Unit` by using the extension function `replaceWithUnit()`:

```kotlin linenums="1"
{{ insert('kotlin/guides/UniExt.kt', 'uniReplaceWithUnit') }}
```

### Uni builder

Building a `Uni` from Kotlin code can easily be achieved using the following builders available as regular or coroutine variant:

```kotlin linenums="1"
{{ insert('kotlin/guides/UniExt.kt', 'uniBuilder') }}
```

```kotlin linenums="1"
{{ insert('kotlin/guides/Coroutines.kt', 'uniBuilder') }}
```

### Multi builder

Building a `Multi` from Kotlin code can be achieved using the `multi` builder, available as regular or coroutine variant:

```kotlin linenums="1"
{{ insert('kotlin/guides/MultiExt.kt', 'multiBuilder') }}
```

The builder accepts an optional back-pressure strategy:

```kotlin linenums="1"
{{ insert('kotlin/guides/MultiExt.kt', 'multiBuilderBackPressure') }}
```

A coroutine variant allows suspend calls within the builder:

```kotlin linenums="1"
{{ insert('kotlin/guides/MultiExt.kt', 'multiBuilderSuspend') }}
```

### Tuple destructuring

Mutiny's `Tuple2` through `Tuple9` support Kotlin destructuring declarations:

```kotlin linenums="1"
{{ insert('kotlin/guides/TupleExt.kt', 'tupleDestructuring') }}
```

### Typed failure handling

Use reified generics for concise failure type matching on both `Uni` and `Multi`:

```kotlin linenums="1"
{{ insert('kotlin/guides/FailureExt.kt', 'uniReifiedOnFailure') }}
```

```kotlin linenums="1"
{{ insert('kotlin/guides/FailureExt.kt', 'multiReifiedOnFailure') }}
```
