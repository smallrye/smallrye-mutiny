---
tags:
- guide
- advanced
---

# Using map, flatMap and concatMap

If you are a seasoned reactive developer, you may miss the `map`, `flatMap`, `concatMap` methods.

The Mutiny API is quite different from the _standard_ reactive eXtensions API.

There are multiple reasons for this choice.
Typically, _flatMap_ is not necessarily well understood by every developer, leading to potentially catastrophic consequences.

That being said, Mutiny provides the _map_, _flatMap_ and _concatMap_ methods, implementing the most common variant for each:

```java linenums="1"
{{ insert('java/guides/RxTest.java', 'rx') }}
```

The Mutiny equivalents are:

* `map -> onItem().transform()`
* `flatMap -> onItem().transformToUniAndMerge` and `onItem().transformToMultiAndMerge`
* `concatMap -> onItem().transformToUniAndConcatenate` and `onItem().transformToMultiAndConcatenate`

The following snippet demonstrates how to uses these methods:

```java linenums="1"
{{ insert('java/guides/RxTest.java', 'mutiny') }}
```
