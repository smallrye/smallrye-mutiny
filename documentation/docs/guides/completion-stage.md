---
tags:
- guide
- intermediate
---

# How to deal with CompletionStage?

`CompletionStage` and `CompletableFuture` are classes provided by Java to represent asynchronous actions.

## Differences between Uni and CompletionStage

While `CompletionStage` and `CompletableFuture` are close to `Uni` in terms of use case, there are some fundamental differences.

`CompletionStage` are _eager_.
When a method returns a `CompletionStage,` the operation has already been triggered.
The outcome is used to complete the returned `CompletionStage`.
On the other side, `Unis` are lazy.
The operation is only triggered once there is a subscription.

`CompletionStage` _caches_ the outcome.
So, once received, you can retrieve the result.
Every retrieval will get the same result.
With `Uni`, every subscription has the opportunity to re-trigger the operation and gets a different result.

!!! tip
    
    You can also _cache_ the outcome with `Uni.memoize().indefinitely()`.

## From Uni to CompletionStage

You can create a `CompletionStage` from `Uni` using `uni.subscribeAsCompletionStage()`.

```java linenums="1"
{{ insert('java/guides/CompletionStageTest.java', 'uni-subscribe-cs') }}
```

It's important to understand that retrieving a `CompletionStage` subscribes to the `Uni`.
If you do this operation twice, it subscribes to the `Uni` twice and re-trigger the operation.

```java linenums="1"
{{ insert('java/guides/CompletionStageTest.java', 'uni-subscribe-cs-twice') }}
```

## Creating a Uni from a CompletionStage

To create a `Uni` from a `CompletionStage`, use `Uni.createFrom().completionStage(...)`.

```java linenums="1"
{{ insert('java/guides/CompletionStageTest.java', 'create-uni') }}
```

As you can see, there are two versions.
The first one receives the `CompletionStage` directly, while the second one gets a supplier.
In the case of multiple subscriptions on the produced `Uni`, the supplier is called multiple times (once per subscription), and so can change the return `CompletionStage`.
It also delays the creation of the `CompletionStage` until there is a subscription, which only triggers the operation at that time.
If you pass the instance directly, it will always use the same one (even for multiple subscriptions) and triggers the operation even if there is no subscription.
For these reasons, it is generally better to use the variant accepting a supplier.

Note that if the completion stage produces a `null` value, the resulting `Uni` emits `null` as item.
If the completion stages complete exceptionally, the failure is emitted by the resulting `Uni`.

## Creating a Multi from a CompletionStage

To create a `Multi` from a `CompletionStage`, use `Multi.createFrom().completionStage(...)`.
It produces:

* a multi emitting an item and completing - if the value produced by the completion stage is not `null`,
* an empty multi if the value produced by the completion stage is `null`,
* a failed multi is completion stage is completed exceptionally.

```java linenums="1"
{{ insert('java/guides/CompletionStageTest.java', 'create-multi') }}
```

For the same reason as for `Uni`, there are two versions:

1. one accepting a `CompletionStage` directly
2. one accepting a `Supplier<CompletionStage>`, called at subscription-time, for every subscription.

It is recommended to use the second version.

