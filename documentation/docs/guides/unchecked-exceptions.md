---
tags:
- guide
- intermediate
---

# Dealing with checked exceptions

When implementing your reactive pipeline, you write lots of functions (`java.util.function.Function`), consumers (`java.util.function.Consumer`), suppliers (`java.util.function.Supplier`) and so on.

By default, you cannot throw checked exceptions.

When integrating libraries throwing checked exceptions (like `IOException`) it's not very convenient to add a `try/catch` block and wrap the thrown exception into a runtime exception:

```java linenums="1"
{{ insert('java/guides/UncheckedTest.java', 'rethrow') }}
```

Mutiny provides utilities to avoid having to do this manually.

If your operation throws a _checked exception_, you can use the [`io.smallrye.mutiny.unchecked.Unchecked`](https://javadoc.io/doc/io.smallrye.reactive/mutiny/latest/io/smallrye/mutiny/unchecked/Unchecked.html) wrappers.

For example, if your synchronous transformation uses a method throwing a checked exception, wrap it using `Unchecked.function`:

```java linenums="1"
{{ insert('java/guides/UncheckedTest.java', 'transform') }}
```
You can also wrap consumers such as in:

```java linenums="1"
{{ insert('java/guides/UncheckedTest.java', 'invoke') }}
```


!!! tip

    You can add the following import statement to simplify the usage of the provided methods:
    
    `import static io.smallrye.mutiny.unchecked.Unchecked.*;`
