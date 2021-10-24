package io.smallrye.mutiny.coroutines

import io.smallrye.mutiny.Multi
import io.smallrye.mutiny.subscription.MultiEmitter
import io.smallrye.mutiny.subscription.MultiSubscriber
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import org.reactivestreams.Subscription
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.coroutineContext

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
            try {
                runBlocking(parentCtx) {
                    channel.send(item)
                }
            } catch (e: CancellationException) {
                // Coroutine was cancelled either regularly or by failure
                // in case of a regular cancellation, e.g. by Flow abortion (happens also by Flow.first())
                // in case of a failure, 'invokeOnClose' is called with a reason - setting a terminatinoFailure is not necessary here
                if (terminationLock.isLocked) {
                    subscription.get()?.cancel()
                    terminationLock.unlock()
                }
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
        CoroutineScope(parentCtx).launch {
            try {
                collect { item ->
                    if (em.isCancelled) {
                        throw CancellationException("Multi subscription cancelled")
                    }
                    em.emit(item)
                }
                em.complete()
            } catch (th: Throwable) {
                em.fail(th)
            }
        }
    }
}
