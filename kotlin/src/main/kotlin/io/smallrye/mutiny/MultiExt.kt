package io.smallrye.mutiny

import io.smallrye.mutiny.subscription.BackPressureStrategy
import io.smallrye.mutiny.subscription.MultiEmitter

/**
 * Kotlin-friendly scope for emitting items into a [Multi].
 * Wraps [MultiEmitter] with a more idiomatic Kotlin interface.
 */
interface MultiEmitterScope<T> {
    fun emit(item: T)
    fun complete()
    fun fail(error: Throwable)
    val isCancelled: Boolean
}

internal class MultiEmitterScopeAdapter<T>(private val emitter: MultiEmitter<in T>) : MultiEmitterScope<T> {
    override fun emit(item: T) { emitter.emit(item) }
    override fun complete() { emitter.complete() }
    override fun fail(error: Throwable) { emitter.fail(error) }
    override val isCancelled: Boolean get() = emitter.isCancelled
}

/**
 * Build a [Multi] from a non-suspending block using [MultiEmitterScope].
 *
 * @param backPressure the back-pressure strategy, defaults to [BackPressureStrategy.BUFFER]
 * @param supplier the block that emits items via [MultiEmitterScope]
 */
fun <T> multi(
    backPressure: BackPressureStrategy = BackPressureStrategy.BUFFER,
    supplier: MultiEmitterScope<T>.() -> Unit
): Multi<T> = Multi.createFrom().emitter({ emitter ->
    val scope = MultiEmitterScopeAdapter(emitter)
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
}, backPressure)
