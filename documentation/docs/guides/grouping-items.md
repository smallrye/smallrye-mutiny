---
tags:
- guide
- intermediate
---

# Grouping items from Multi

Mutiny provides several operators to group items from a `Multi` stream. 
You can group items by a key function (similar to SQL's `GROUP BY`), split items into fixed-size chunks, or create time-based windows.

The grouping operators are available from the `group()` method on `Multi`.

## Grouping into Lists

The `group().intoLists()` operator allows you to collect items into lists based on size or time.

### Fixed-size lists

Use `group().intoLists().of(size)` to create fixed-size lists from the stream:

```java linenums="1"
{{ insert('java/guides/operators/GroupingItemsTest.java', 'groupIntoLists') }}
```

The last list may contain fewer items if the stream doesn't divide evenly.

### Time-based lists

You can create time-based lists using `group().intoLists().every(Duration)`:

```java linenums="1"
{{ insert('java/guides/operators/GroupingItemsTest.java', 'timeBasedListGrouping') }}
```

### Size and time-based lists

You can combine both size and time constraints using `group().intoLists().of(size, Duration)`:

```java linenums="1"
{{ insert('java/guides/operators/GroupingItemsTest.java', 'sizeAndTimeBasedListGrouping') }}
```

This will emit a list when either the size limit is reached or the duration expires, whichever comes first.

## Grouping into Multi Streams

The `group().intoMultis()` operator allows you to create separate `Multi` streams from your data. Unlike `intoLists()` which materializes all items into memory, `intoMultis()` keeps items as streams, which is better for:

- Applying stream transformations to each group
- Processing large groups without loading everything into memory
- Composing with other reactive operators

### Fixed-size Multi streams

Use `group().intoMultis().of(size)` to create `Multi` streams of a fixed size:

```java linenums="1"
{{ insert('java/guides/operators/GroupingItemsTest.java', 'groupIntoMultis') }}
```

### Time-based Multi streams

You can create time-based windows using `group().intoMultis().every(Duration)`:

```java linenums="1"
{{ insert('java/guides/operators/GroupingItemsTest.java', 'timeBasedGrouping') }}
```

## Grouping by a key function

The `group().by()` operator groups items based on a key function, emitting a `Multi<GroupedMulti<K, V>>` where each `GroupedMulti` represents a group of items sharing the same key.

```java linenums="1"
{{ insert('java/guides/operators/GroupingItemsTest.java', 'groupByKey') }}
```

Each `GroupedMulti` has a `key()` method that returns the key for that group.
Items are distributed to groups based on the key function result.

### Using both key and value mappers

You can transform items while grouping them by providing both a key mapper and a value mapper:

```java linenums="1"
{{ insert('java/guides/operators/GroupingItemsTest.java', 'groupByKeyAndValue') }}
```


## Processing groups with merge vs concatenate

When processing groups, you need to decide how to combine the results back into a single stream. Similar to [transforming items asynchronously](../tutorials/transforming-items-asynchronously.md), you can use either **merge** or **concatenate**:

### Using merge

With merge, groups are processed concurrently - items from different groups can interleave in the output stream:

```java linenums="1"
{{ insert('java/guides/operators/GroupingItemsTest.java', 'groupByWithMerge') }}
```

!!! warning "Upstream request starvation with merge"

    When using `.merge(concurrency)` or similar merge operations after `group().by()`,
    **the concurrency parameter must be greater than or equal to the number of groups that are created but not terminated**.

    If you create more groups than the concurrency limit allows, some groups cannot make progress while waiting for others to complete.
    This leads to a **request starvation** where the upstream won't receive requests for emitting new items.

#### Example causing request starvation

```java linenums="1"
{{ insert('java/guides/operators/GroupingItemsTest.java', 'groupByDeadlock') }}
```

In this example, 10 groups are created but only 2 can be processed concurrently.
Groups 3-10 cannot make progress because the downstream subscriber is busy with groups 1-2.
Meanwhile, groups 1-2 may not complete because they're waiting for backpressure signals from the full pipeline.
The problem is even more exacerbated with infinite streams and infinite groups.

#### How to avoid request starvation

1. **Set concurrency >= number of groups**: If you know the maximum number of groups in advance, set the concurrency parameter to at least that number using `.merge(n)`
2. **Use unbounded concurrency**: Call `.merge(Integer.MAX_VALUE)` to allow unlimited number of concurrent groups
3. **Use concatenate instead**: Process groups sequentially (see below)

### Using concatenate

With concatenate, groups are processed sequentially - each group must fully terminate before the next group can start processing:

```java linenums="1"
{{ insert('java/guides/operators/GroupingItemsTest.java', 'groupByWithConcatenate') }}
```

## Choosing between group().by() and split()

Mutiny provides both `group().by()` and `split()` operators. Here's when to use each:

- **Use `group().by()`** when you don't know the keys in advance and the number of groups is dynamic.
- **Use `split()`** when you know all possible keys upfront (defined by an enum) and you want individual `Multi` instances for each split.

See the [splitting guide](multi-split.md) for more details on `split()`.
