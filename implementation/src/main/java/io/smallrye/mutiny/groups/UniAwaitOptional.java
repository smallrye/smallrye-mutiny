package io.smallrye.mutiny.groups;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.time.Duration;
import java.util.Optional;

import io.smallrye.mutiny.Context;
import io.smallrye.mutiny.TimeoutException;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.operators.uni.UniBlockingAwait;

/**
 * Likes {@link UniAwait} but wrapping the item event into an {@link Optional}. This optional is empty if the
 * {@link Uni} fires {@code null}.
 *
 * @param <T> the type of the item
 * @see Uni#await()
 */
public class UniAwaitOptional<T> {

    private final Uni<T> upstream;
    private final Context context;

    public UniAwaitOptional(Uni<T> upstream, Context context) {
        this.upstream = nonNull(upstream, "upstream");
        this.context = context;
    }

    /**
     * Subscribes to the {@link Uni} and waits (blocking the caller thread) <strong>indefinitely</strong> until a
     * {@code item} event is fired or a {@code failure} event is fired by the upstream uni.
     * <p>
     * If the {@link Uni} fires an item, it returns that item wrapped into an {@link Optional}. If the item is
     * {@code null} the returned optional is empty.
     * If the {@link Uni} fires a failure, the original exception is thrown (wrapped in
     * a {@link java.util.concurrent.CompletionException} it's a checked exception).
     * <p>
     * Note that each call to this method triggers a new subscription.
     *
     * @return the item from the {@link Uni} wrapped into an {@link Optional}, empty if the {@link Uni} is resolved
     *         with {@code null}
     */
    public Optional<T> indefinitely() {
        return atMost(null);
    }

    /**
     * Subscribes to the {@link Uni} and waits (blocking the caller thread) <strong>at most</strong> the given duration
     * until an item or failure is fired by the upstream uni.
     * <p>
     * If the {@link Uni} fires an item, it returns that item wrapped into an {@link Optional}. If the item is
     * {@code null} the returned optional is empty.
     * If the {@link Uni} fires a failure, the original exception is thrown (wrapped in
     * a {@link java.util.concurrent.CompletionException} it's a checked exception).
     * If the timeout is reached before completion, a {@link TimeoutException} is thrown.
     * <p>
     * Note that each call to this method triggers a new subscription.
     *
     * @param duration the duration, must not be {@code null}, must not be negative or zero.
     * @return the item from the {@link Uni}, potentially {@code null}
     */
    public Optional<T> atMost(Duration duration) {
        return Optional.ofNullable(UniBlockingAwait.await(upstream, duration, context));
    }

}
