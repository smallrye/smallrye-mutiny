package io.smallrye.mutiny.subscription;

import io.smallrye.mutiny.Uni;

/**
 * An object allowing to send signals to the downstream {@link Uni}.
 * {@link Uni} propagates a single item event, once the first is propagated, the others events have no effect.
 *
 * @param <T> the expected type of item.
 */
public interface UniEmitter<T> extends ContextSupport {

    /**
     * Emits the {@code item} event downstream with the given (potentially {@code null}) item.
     * <p>
     * Calling this method multiple times or after the {@link #fail(Throwable)} method has no effect.
     *
     * @param item the item, may be {@code null}
     */
    void complete(T item);

    /**
     * Emits the {@code failure} event downstream with the given exception.
     * <p>
     * Calling this method multiple times or after the {@link #complete(Object)} method has no effect.
     *
     * @param failure the exception, must not be {@code null}
     */
    void fail(Throwable failure);

    /**
     * Attaches a @{code termination} event handler invoked when the downstream {@link UniSubscription} is cancelled,
     * or when the emitter has emitted either an {@code item} or {@code failure} event.
     * <p>
     * This method allows cleanup resources once the emitter can be disposed (has reached a terminal state).
     * <p>
     * If the registration of the {@code onTermination} callback is done after the termination, it invokes the callback
     * immediately.
     *
     * @param onTermination the action to run on termination, must not be {@code null}
     * @return this emitter
     */
    UniEmitter<T> onTermination(Runnable onTermination);

}
