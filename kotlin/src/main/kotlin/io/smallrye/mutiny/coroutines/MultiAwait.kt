package io.smallrye.mutiny.coroutines

import io.smallrye.mutiny.Multi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.lastOrNull
import kotlinx.coroutines.flow.toList

/**
 * Collect all items from this [Multi] into a [List], suspending until completion.
 */
suspend fun <T> Multi<T>.awaitList(): List<T> =
    asFlow().toList()

/**
 * Await the first item from this [Multi], or `null` if the [Multi] completes empty.
 */
suspend fun <T> Multi<T>.awaitFirst(): T? =
    asFlow().firstOrNull()

/**
 * Await the first item from this [Multi], throwing [NoSuchElementException] if empty.
 */
suspend fun <T> Multi<T>.awaitFirstOrThrow(): T =
    asFlow().first()

/**
 * Await the last item from this [Multi], or `null` if the [Multi] completes empty.
 */
suspend fun <T> Multi<T>.awaitLast(): T? =
    asFlow().lastOrNull()

/**
 * Await the last item from this [Multi], throwing [NoSuchElementException] if empty.
 */
suspend fun <T> Multi<T>.awaitLastOrThrow(): T =
    asFlow().last()

/**
 * Process each item from this [Multi] using a suspend [action], suspending until completion.
 */
suspend fun <T> Multi<T>.awaitEach(action: suspend (T) -> Unit) =
    asFlow().collect { action(it) }
