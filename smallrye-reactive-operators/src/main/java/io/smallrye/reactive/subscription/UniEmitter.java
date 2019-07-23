package io.smallrye.reactive.subscription;


import io.smallrye.reactive.Uni;

/**
 * An object allowing to send signals to the downstream {@link Uni}.
 * {@link Uni} propagates a single signal, once the first is propagated, the others signals have no effect.
 *
 * @param <T> the expected type of result.
 */
public interface UniEmitter<T> {

    /**
     * Emits the {@code result} event downstream with the given (potentially {@code null}) result.
     * <p>
     * Calling this method multiple times or after the {@link #failure(Throwable)} method has no effect.
     *
     * @param result the result, may be {@code null}
     */
    void result(T result);

    /**
     * Emits the {@code failure} event downstream with the given exception.
     * <p>
     * Calling this method multiple times or after the {@link #result(Object)} method has no effect.
     *
     * @param failure the exception, must not be {@code null}
     */
    void failure(Throwable failure);

    /**
     * Attaches a @{code cancellation} event handler invoked when the downstream {@link UniSubscription} is cancelled.
     * This method allow propagating the cancellation to the source and potentially cleanup resources.
     *
     * <ul>
     *     <li>Executor: Operate on no particular executor, except if {//TODO@link Uni#cancelOn} has been called</li>
     * </ul>
     *
     * @param onCancel the action to run on cancellation, must not be {@code null}
     * @return this emitter
     */
    UniEmitter<T> onCancellation(Runnable onCancel);

}
