package io.smallrye.reactive.subscription;


import io.smallrye.reactive.Uni;

/**
 * An object allowing to send signals to the downstream {@link Uni}.
 * {@link Uni} propagates a single result event, once the first is propagated, the others events have no effect.
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
     * Attaches a @{code termination} event handler invoked when the downstream {@link UniSubscription} is cancelled,
     * or when the emitter has emitted either a {@code result} or {@code failure} event.
     * <p>
     * This method allows cleanup resources once the emitter can be disposed (has reached a terminal state).
     *
     * @param onTermination the action to run on termination, must not be {@code null}
     * @return this emitter
     */
    UniEmitter<T> onTermination(Runnable onTermination);

}
