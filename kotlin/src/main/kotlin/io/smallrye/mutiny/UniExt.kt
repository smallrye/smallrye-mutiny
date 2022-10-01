package io.smallrye.mutiny

/**
 * Ignore the item emitted by this [Uni] and replace it with [Unit].
 * In contrast to [Uni.replaceWithVoid] the resulting [Uni] does not hold
 * a `null` item, but an actual [Unit].
 */
fun Uni<*>.replaceWithUnit(): Uni<Unit> = onItem().transform { }
