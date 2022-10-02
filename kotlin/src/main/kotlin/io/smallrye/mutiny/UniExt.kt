package io.smallrye.mutiny

/**
 * Ignore the item emitted by this [Uni] and replace it with [Unit].
 * In contrast to [Uni.replaceWithVoid] the resulting [Uni] does not hold
 * a `null` item, but an actual [Unit].
 */
fun Uni<*>.replaceWithUnit(): Uni<Unit> = onItem().transform { }

/**
 * Produce a [Uni] from given [supplier] in a non-suspending context.
 * This is a shortcut for `Uni.createFrom().item<T>(() -> T)`.
 */
fun <T> uni(supplier: () -> T): Uni<T> = Uni.createFrom().item(supplier)
