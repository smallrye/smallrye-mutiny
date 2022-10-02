package io.smallrye.mutiny.coroutines

import io.smallrye.mutiny.Multi
import io.smallrye.mutiny.subscription.MultiEmitter
import io.smallrye.mutiny.subscription.MultiSubscriber
import java.util.concurrent.Flow.Subscription
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex

/**
 * Subscribe to this [Multi] and provide the items as [Flow].
 *
 * If this [Multi] emits a failure, it is thrown from the Flow when operating on it.
 *
 * The subscription to this [Multi] gets automatically cancelled when the surrounding coroutine fails or gets cancelled.
 */
@ExperimentalCoroutinesApi
suspend fun <T> Multi<T>.asFlow(): Flow<T> = channelFlow<T> {
    val parentCtx = coroutineContext
    val terminationFailure = AtomicReference<Throwable?>(null)
    val terminationLock = Mutex(locked = true)

    val subscriber = object : MultiSubscriber<T> {
        private val subscription = AtomicReference<Subscription?>()

        init {
            invokeOnClose { reason ->
                if (reason != null) {
                    // coroutine was cancelled or is failed
                    subscription.get()?.cancel()
                }
            }
        }

        override fun onSubscribe(s: Subscription) {
            if (subscription.compareAndSet(null, s)) {
                s.request(Long.MAX_VALUE)
            } else {
                s.cancel()
            }
        }

        override fun onItem(item: T) {
            if (parentCtx.isActive) {
                runBlocking {
                    channel.send(item)
                }
            } else if (terminationLock.isLocked) {
                // Coroutine is completed or was cancelled:
                // regular cancellation may happen by Flow abortion like with Flow.first(), no failure must be thrown
                // in case of a failure, 'invokeOnClose' is called with a reason separately - setting a terminatinoFailure is also not necessary here
                subscription.get()?.cancel()
                terminationLock.unlock()
            }
        }

        override fun onFailure(failure: Throwable) {
            terminationFailure.set(failure)
            terminationLock.unlock()
        }

        override fun onCompletion() {
            if (terminationLock.isLocked) {
                terminationLock.unlock()
            }
        }

    }

    subscribe().withSubscriber(subscriber)
    // wait (suspending) for completion or failure of Multi to terminate the Flow
    terminationLock.lock()
    terminationFailure.get()?.also { throw it }
}

/**
 * Provide this [Flow]s items or failure as [Multi].
 *
 * The MultiEmitter is scheduled as child of callings coroutine context.
 *
 * Please note, that the [Flow]s values are emitted immediately,
 * without respecting the requested amount of the subscriber.
 */
suspend fun <T> Flow<T>.asMulti(): Multi<T> {
    val parentCtx = coroutineContext
    return Multi.createFrom().emitter { em: MultiEmitter<in T> ->
        val job = CoroutineScope(parentCtx).launch {
            try {
                collect { item ->
                    if (em.isCancelled) {
                        throw NonPropagatingCancellationException()
                    }
                    em.emit(item)
                }
                em.complete()
            } catch (th: Throwable) {
                when (th) {
                    is NonPropagatingCancellationException -> em.complete()
                    else -> em.fail(th)
                }
            }
        }
        em.onTermination {
            job.cancel(NonPropagatingCancellationException())
        }
    }
}

private class NonPropagatingCancellationException : kotlin.coroutines.cancellation.CancellationException()
