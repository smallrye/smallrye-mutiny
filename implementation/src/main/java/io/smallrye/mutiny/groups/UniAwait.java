package io.smallrye.mutiny.groups;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.time.Duration;
import java.util.Optional;

import io.smallrye.common.annotation.CheckReturnValue;
import io.smallrye.mutiny.Context;
import io.smallrye.mutiny.TimeoutException;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.operators.uni.UniBlockingAwait;

/**
 * Waits and returns the item emitted by the {@link Uni}. If the {@link Uni} receives a failure, the failure is thrown.
 * <p>
 * This class lets you configure how to retrieves the item of a {@link Uni} by blocking the caller thread.
 *
 * @param <T> the type of item
 * @see Uni#await()
 */
public class UniAwait<T> {

    private final Uni<T> upstream;
    private final Context context;

    public UniAwait(Uni<T> upstream, Context context) {
        this.upstream = nonNull(upstream, "upstream");
        this.context = context;
    }

    /**
     * Subscribes to the {@link Uni} and waits (blocking the caller thread) <strong>indefinitely</strong> until a
     * {@code item} event is fired or a {@code failure} event is fired by the upstream uni.
     * <p>
     * If the {@link Uni} fires an item, it returns that item, potentially {@code null} if the operation
     * returns {@code null}.
     * If the {@link Uni} fires a failure, the original exception is thrown (wrapped in
     * a {@link java.util.concurrent.CompletionException} it's a checked exception).
     * <p>
     * Note that each call to this method triggers a new subscription.
     *
     * @return the item from the {@link Uni}, potentially {@code null}
     */
    public T indefinitely() {
        return atMost(null);
    }

    /**
     * Subscribes to the {@link Uni} and waits (blocking the caller thread) <strong>at most</strong> the given duration
     * until an item or failure is fired by the upstream uni.
     * <p>
     * If the {@link Uni} fires an item, it returns that item, potentially {@code null} if the operation
     * returns {@code null}.
     * If the {@link Uni} fires a failure, the original exception is thrown (wrapped in
     * a {@link java.util.concurrent.CompletionException} it's a checked exception).
     * If the timeout is reached before completion, a {@link TimeoutException} is thrown.
     * <p>
     * Note that each call to this method triggers a new subscription.
     *
     * @param duration the duration, must not be {@code null}, must not be negative or zero.
     * @return the item from the {@link Uni}, potentially {@code null}
     */
    public T atMost(Duration duration) {
        return UniBlockingAwait.await(upstream, duration, context);
    }

    /**
     * Indicates that you are awaiting for the item event of the attached {@link Uni} wrapped into an {@link Optional}.
     * So if the {@link Uni} fires {@code null} as item, you receive an empty {@link Optional}.
     *
     * @return the {@link UniAwaitOptional} configured to produce an {@link Optional}.
     */
    @CheckReturnValue
    public UniAwaitOptional<T> asOptional() {
        return new UniAwaitOptional<>(upstream, context);
    }

}
