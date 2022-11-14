# Mutiny Math Operators

This library provides a set of _operators_ for [Mutiny](https://smallrye.io/smallrye-mutiny).

You can use these operators using the `plug` method offered by Mutiny:

```java
Multi<Long> counts = Multi.createFrom().items("a", "b", "c", "d", "e")
                            .plug(Math.count())
```

These operators provide streams (`Multi`) emitting new values when the computed value changes. To get the last value, use `collect().last()`:

```java
String max = Multi.createFrom().items("e", "b", "c", "f", "g", "e")
                .plug(Math.max())
                .collect().last()
                .await().indefinitely();
```

The `io.smallrye.math.Math` class provides the entry point to the offered operators:

* count - emits the number of items emitted by the upstream
* index - emits `Tuple2<Long, T>` for each item from the upstream. The first element of the tuple is the index (0-based), and the second is the item 
* min / max - emits the min/max item from the upstream, using Java comparator
* top(x) - emits the top x items from the upstream, a new ranking is emitted every time it changes.
* sum - emits the sum of all the items emitted by the upstream
* average - emits the average of all the items emitted by the upstream
* median - emits the median of all the items emitted by the upstream
* statistics - emits statistics about the emitted items



