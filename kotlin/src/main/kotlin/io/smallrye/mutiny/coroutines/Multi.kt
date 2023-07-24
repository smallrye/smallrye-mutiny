package io.smallrye.mutiny.coroutines

import io.smallrye.mutiny.Multi
import io.smallrye.mutiny.subscription.MultiEmitter
import io.smallrye.mutiny.subscription.MultiSubscriber
import java.util.concurrent.Flow.Subscription
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Subscribe to this [Multi] and provide the items as [Flow].
 *
 * If this [Multi] emits a failure, it is thrown from the Flow when operating on it.
 * The subscription to this [Multi] gets automatically cancelled when the surrounding coroutine fails or gets cancelled.
 *
 * The resulting [Flow] is backed by a [Channel] which behavior can be configured using:
 * @property bufferCapacity the type (defined by [Channel] constants) or the capacity (>0 <[Int.MAX_VALUE]) of the channel, defaulting to [Channel.UNLIMITED]
 * @property bufferOverflowStrategy action strategy on exceeding the [bufferCapacity], see [BufferOverflow].
 */
fun <T> Multi<T>.asFlow(
    bufferCapacity: Int = Channel.UNLIMITED,
    bufferOverflowStrategy: BufferOverflow = BufferOverflow.SUSPEND
): Flow<T> = callbackFlow<T> {
    val parentCtx = coroutineContext

    val subscriber = object : MultiSubscriber<T> {
        private val subscription = AtomicReference<Subscription?>()

        override fun onSubscribe(s: Subscription) {
            if (subscription.compareAndSet(null, s)) {
                s.request(Long.MAX_VALUE)
            } else {
                s.cancel()
            }
        }

        override fun onItem(item: T) {
            if (parentCtx.isActive) {
                channel.trySendBlocking(item).getOrThrow()
            } else {
                // Coroutine is completed or was cancelled:
                // regular cancellation may happen by Flow abortion like with Flow.first(), no failure must be thrown
                subscription.get()?.cancel()
            }
        }

        override fun onFailure(failure: Throwable) {
            cancel(CancellationException(failure.message, failure))
        }

        override fun onCompletion() {
            channel.close()
        }
    }
    subscribe().withSubscriber(subscriber)
    awaitClose()
}.buffer(capacity = bufferCapacity, onBufferOverflow = bufferOverflowStrategy)

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
