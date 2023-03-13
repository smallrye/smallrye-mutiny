---
tags:
- guide
- beginner
---

# Shortcut methods

The Mutiny API is decomposed around the idea of groups, each group handling a specific event.
However, to avoid verbosity, Mutiny also exposes _shortcuts_ for the most used methods.
Be aware that these shorts, while making the code shorter, may harm the readability and understandability.

To _peek_ at items, you can use the `invoke` method:

```java linenums="1"
{{ insert('java/guides/ShortcutsTest.java', 'invoke') }}
```

`invoke` is a shortcut for `onItem().invoke(...)`.

Mutiny also provides the `call` method for executing an action returning a `Uni`.
This is useful to execute an asynchronous action without modifying incoming item:

```java linenums="1"
{{ insert('java/guides/ShortcutsTest.java', 'call') }}
```
`call` is a shortcut for `onItem().call(...)`.

The following table lists the available shortcuts available by the `Uni` class:

| Shortcut                                                 | Equivalent                                                                                          |
|----------------------------------------------------------|-----------------------------------------------------------------------------------------------------|
| `uni.map(x -> y)`                                        | `uni.onItem().transform(x -> y)`                                                                    |
| `uni.flatMap(x -> uni2)`                                 | `uni.onItem().transformToUni(x -> uni2)`                                                            |
| `uni.chain(x -> uni2)`                                   | `uni.onItem().transformToUni(x -> uni2)`                                                            |
| `uni.invoke(x -> System.out.println(x))`                 | `uni.onItem().invoke(x -> System.out.println(x))`                                                   |
| `uni.call(x -> uni2)`                                    | `uni.onItem().call(x -> uni2)`                                                                      |
| `uni.eventually(() -> System.out.println("eventually"))` | `uni.onItemOrFailure().invoke((ignoredItem, ignoredException) -> System.out.println("eventually"))` |
| `uni.eventually(() -> uni2)`                             | `uni.onItemOrFailure().call((ignoredItem, ignoredException) -> uni2)`                               |
| `uni.replaceWith(x)`                                     | `uni.onItem().transform(ignored -> x)`                                                              |
| `uni.replaceWith(uni2)`                                  | `uni.onItem().transformToUni(ignored -> uni2)`                                                      |
| `uni.replaceIfNullWith(x)`                               | `uni.onItem().ifNull().continueWith(x)`                                                             |

