package io.smallrye.mutiny

import io.smallrye.mutiny.groups.MultiOnFailure
import io.smallrye.mutiny.groups.UniOnFailure

/**
 * Configures failure handling for failures of type [E].
 * Kotlin-idiomatic alternative to `onFailure(E::class.java)`.
 */
inline fun <T, reified E : Throwable> Uni<T>.onFailure(): UniOnFailure<T, E> =
    onFailure(E::class.java)

/**
 * Configures failure handling for failures of type [E].
 * Kotlin-idiomatic alternative to `onFailure(E::class.java)`.
 */
inline fun <T, reified E : Throwable> Multi<T>.onFailure(): MultiOnFailure<T> =
    onFailure(E::class.java)
