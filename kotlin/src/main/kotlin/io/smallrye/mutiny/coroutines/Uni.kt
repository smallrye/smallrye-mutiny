package io.smallrye.mutiny.coroutines

import io.smallrye.mutiny.Uni
import io.smallrye.mutiny.subscription.UniEmitter
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Awaits the result of this [Uni] while suspending the surrounding coroutine.
 *
 * As soon as this [Uni] succeeds with an item, that item will be returned.
 * Be aware of, that since the type parameter `T` is determined from the underlying Java type system,
 * there is no first-class type safety and the resulting item may be null.
 *
 * If this [Uni] results in a failure that failure is thrown.
 *
 * A cancellation of the coroutine context will cancel the subscription to this [Uni] and no event will be emitted anymore.
 */
suspend fun <T> Uni<T>.awaitSuspending() = suspendCancellableCoroutine<T> { continuation ->
    subscribe().with(
        /* onItemCallback = */ { item ->
            suppressCancellationException {
                continuation.resume(item)
            }
        },
        /* onFailureCallback = */ { failure ->
            continuation.resumeWithException(failure)
        }
    ).apply {
        continuation.invokeOnCancellation { cancel() }
    }
}

/**
 * Provide this [Deferred]s value or failure as [Uni].
 *
 * If the surrounding coroutine fails or gets cancelled that failure is propagated as well.
 */
@ExperimentalCoroutinesApi
fun <T> Deferred<T>.asUni(): Uni<T> = Uni.createFrom().emitter { em: UniEmitter<in T> ->
    invokeOnCompletion {
        try {
            em.complete(getCompleted())
        } catch (th: Throwable) {
            // Fail the Uni if the Deferred fails or is cancelled.
            em.fail(th)
        }
    }
    // Cancel the Deferred if the Uni is cancelled.
    em.onTermination {
        if (this.isActive) {
            this.cancel()
        }
    }
}

/**
 * Produce a [Uni] from given [suspendSupplier] in a non-suspending context.
 *
 * The [suspendSupplier] block isn't attached to the structured concurrency of the current `coroutineContext` by default
 * but executed in the [GlobalScope], that means that failures raised from [suspendSupplier] will not be
 * thrown immediately but propagated to the resulting [Uni], similar to the behavior of `Uni.createFrom().item<T>(() -> T)`.
 * The behaviour can be changed by passing an own [context] that's used for `async` execution of the given [suspendSupplier].
 */
@ExperimentalCoroutinesApi
@OptIn(DelicateCoroutinesApi::class)
fun <T> uni(context: CoroutineScope = GlobalScope, suspendSupplier: suspend () -> T): Uni<T> = context.async {
    suspendSupplier()
}.asUni()
