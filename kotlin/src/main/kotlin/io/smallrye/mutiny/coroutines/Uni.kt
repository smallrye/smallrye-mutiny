package io.smallrye.mutiny.coroutines

import io.smallrye.mutiny.Uni
import io.smallrye.mutiny.subscription.UniEmitter
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
