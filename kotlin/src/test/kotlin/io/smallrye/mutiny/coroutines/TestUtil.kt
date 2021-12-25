package io.smallrye.mutiny.coroutines

import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

/**
 * Utility method for typing gentle coroutine testing.
 *
 * Wrap the given [block] in coroutines [runBlocking] using the [Dispatchers.Default] as default [context].
 */
fun <T> testBlocking(context: CoroutineContext = Dispatchers.Default, block: suspend CoroutineScope.() -> T): T =
    runBlocking(context, block)

/**
 * Produces a new embedded [CoroutineScope] to be used for isolating coroutines inside a coroutine test
 */
fun embeddedScope() = CoroutineScope(Dispatchers.Default)