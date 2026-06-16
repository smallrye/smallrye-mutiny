package io.smallrye.mutiny.coroutines

import io.smallrye.mutiny.Multi
import io.smallrye.mutiny.MultiEmitterScope
import io.smallrye.mutiny.MultiEmitterScopeAdapter
import io.smallrye.mutiny.subscription.BackPressureStrategy
import io.smallrye.mutiny.subscription.MultiEmitter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * Build a [Multi] from a suspending block using [MultiEmitterScope].
 *
 * The [supplier] block is launched in the provided [context] (defaulting to [GlobalScope]).
 * When the block completes normally, [MultiEmitter.complete] is called automatically.
 * Exceptions thrown from the block are propagated as [MultiEmitter.fail].
 * Cancellation of the Multi subscription cancels the coroutine job.
 *
 * @param context the coroutine scope to launch the supplier in, defaults to [GlobalScope]
 * @param backPressure the back-pressure strategy, defaults to [BackPressureStrategy.BUFFER]
 * @param supplier the suspending block that emits items via [MultiEmitterScope]
 */
@ExperimentalCoroutinesApi
@OptIn(DelicateCoroutinesApi::class)
fun <T> multi(
    context: CoroutineScope = GlobalScope,
    backPressure: BackPressureStrategy = BackPressureStrategy.BUFFER,
    supplier: suspend MultiEmitterScope<T>.() -> Unit
): Multi<T> = Multi.createFrom().emitter({ emitter ->
    val scope = MultiEmitterScopeAdapter(emitter)
    val job = context.launch {
        try {
            scope.supplier()
            if (!emitter.isCancelled) {
                emitter.complete()
            }
        } catch (th: Throwable) {
            if (!emitter.isCancelled) {
                emitter.fail(th)
            }
        }
    }
    emitter.onTermination {
        if (job.isActive) {
            job.cancel()
        }
    }
}, backPressure)
