---
tags:
- guide
- intermediate
---

# Kotlin coroutines integration

The module `mutiny-kotlin` provides an integration with Kotlin coroutines.

There are currently four extension methods available for converting between Mutiny and Kotlin coroutine types offered in a separate package.
For implementation details please have also a look to these methods documentation.

## Dependency coordinates

The extension functions are shipped in the package `io.smallrye.mutiny.coroutines`:

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
