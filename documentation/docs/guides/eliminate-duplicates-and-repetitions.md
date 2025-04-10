---
tags:
- guide
- beginner
---

# Eliminate duplicates and repetitions

When observing a `Multi`, you may see duplicated items or repetitions.
The `multi.select()` and `multi.skip()` groups provide methods to only select distinct items or drop repetitions.

## Selecting distinct

The `.select().distinct()` operator removes all the duplicates.
As a result, the downstream only contains distinct items:

```java linenums="1"
{{ insert('java/guides/operators/RepetitionsTest.java', 'distinct') }}
```

If you have a stream emitting the `{1, 1, 2, 3, 4, 5, 5, 6, 1, 4, 4}` items, then applying `.select().distinct()` on such a stream produces: `{1, 2, 3, 4, 5, 6}`.

!!! important
    
    The operator keeps a reference on all the emitted items, and so, it could lead to memory issues if the stream contains too many distinct items.

!!! tip
    
    By default, `select().distinct()` uses the `hashCode` method from the item's class.
    You can pass a custom comparator for more advanced checks.

If you have a stream emitting items of type `T`, where duplicates can be identified through an attribute of `T` of type `K`,
then an `extractor` of type `Function<T, K>` can be defined. Applying `.select().distinct(extractor)` on such a stream will
eliminate duplicates but have a lesser memory overhead as only the references to the extracted keys need to be kept, not the whole object.
A typical usage of this might be for a stream of records where uniqueness is determined by a UUID assigned to every record.

## Skipping repetitions

The `.skip().repetitions()` operator removes subsequent repetitions of an item:

```java linenums="1"
{{ insert('java/guides/operators/RepetitionsTest.java', 'repetition') }}
```

If you have a stream emitting the `{1, 1, 2, 3, 4, 5, 5, 6, 1, 4, 4}` items, then applying `.skip().repetitions()` on such a stream produces: `{1, 2, 3, 4, 5, 6, 1, 4}`.

Unlike `.select().distinct()`, you can use this operator on large or infinite streams.

!!! tip
    
    By default, `skip().repetitions()` uses the `equals` method from the item's class.
    You can pass a custom comparator for more advanced checks.