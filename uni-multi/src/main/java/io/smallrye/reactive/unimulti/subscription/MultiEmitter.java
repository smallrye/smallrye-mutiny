package io.smallrye.reactive.unimulti.subscription;

import org.reactivestreams.Subscription;

import io.smallrye.reactive.unimulti.Multi;

/**
 * An object allowing to send signals to the downstream {@link Multi}.
 * {@link Multi} propagates several item event, once a failure or completion event is fired, the other events have
 * no effects.
 * <p>
 * Emitting a {@code null} item is invalid and will cause a failure.
 *
 * @param <T> the expected type of items.
 */
public interface MultiEmitter<T> {

    /**
     * Emits an {@code item} event downstream.
     * <p>
     * Calling this method after a failure or a completion events has no effect.
     *
     * @param item the item, must not be {@code null}
     * @return this emitter, so firing item events can be chained.
     */
    MultiEmitter<T> emit(T item);

    /**
     * Emits a {@code failure} event downstream with the given exception.
     * <p>
     * Calling this method multiple times or after the {@link #complete()} method has no effect.
     *
     * @param failure the exception, must not be {@code null}
     */
    void fail(Throwable failure);

    /**
     * Emits a {@code completion} event downstream indicating that no more item will be sent.
     * <p>
     * Calling this method multiple times or after the {@link #fail(Throwable)} method has no effect.
     */
    void complete();

    /**
     * Attaches a @{code termination} event handler invoked when the downstream {@link Subscription} is cancelled,
     * or when the emitter has emitted either a {@code completion} or {@code failure} event.
     * <p>
     * This method allows cleanup resources once the emitter can be disposed (has reached a terminal state).
     *
     * @param onTermination the action to run on termination, must not be {@code null}
     * @return this emitter
     */
    MultiEmitter<T> onTermination(Runnable onTermination);

}
