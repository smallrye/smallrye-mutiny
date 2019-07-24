package io.smallrye.reactive.subscription;


import io.smallrye.reactive.Multi;
import org.reactivestreams.Subscription;

/**
 * An object allowing to send signals to the downstream {@link Multi}.
 * {@link Multi} propagates several results event, once a failure or completion event is fired, the other events have
 * no effects.
 * <p>
 * Emitting a {@code null} result is invalid and will cause a failure.
 *
 * @param <T> the expected type of results.
 */
public interface MultiEmitter<T> {

    /**
     * Emits a {@code result} event downstream with the given (potentially {@code null}) result.
     * <p>
     * Calling this method after a failure or a completion events has no effect.
     *
     * @param result the result, must not be {@code null}
     */
    void result(T result);

    /**
     * Emits a {@code failure} event downstream with the given exception.
     * <p>
     * Calling this method multiple times or after the {@link #complete()} method has no effect.
     *
     * @param failure the exception, must not be {@code null}
     */
    void failure(Throwable failure);

    /**
     * Emits a {@code completion} event downstream indicating that no more result will be sent.
     * <p>
     * Calling this method multiple times or after the {@link #failure(Throwable)} method has no effect.
     */
    void complete();

    /**
     * Attaches a @{code cancellation} event handler invoked when the downstream {@link Subscription} is cancelled.
     * This method allow propagating the cancellation to the source and potentially cleanup resources.
     *
     * @param onCancel the action to run on cancellation, must not be {@code null}
     * @return this emitter
     */
    MultiEmitter<T> onCancellation(Runnable onCancel);

}
