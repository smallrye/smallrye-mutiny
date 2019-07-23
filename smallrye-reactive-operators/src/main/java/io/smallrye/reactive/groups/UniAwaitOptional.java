package io.smallrye.reactive.groups;

import io.smallrye.reactive.TimeoutException;
import io.smallrye.reactive.Uni;
import io.smallrye.reactive.operators.UniBlockingAwait;

import java.time.Duration;
import java.util.Optional;

import static io.smallrye.reactive.helpers.ParameterValidation.nonNull;

/**
 * Likes {@link UniAwait} but wrapping the result event into an {@link Optional}. This optional is empty if the
 * {@link Uni} fires {@code null}.
 *
 * @param <T> the type of the result
 * @see Uni#await()
 */
public class UniAwaitOptional<T> {

    private final Uni<T> upstream;

    public UniAwaitOptional(Uni<T> upstream) {
        this.upstream = nonNull(upstream, "upstream");
    }

    /**
     * Subscribes to the {@link Uni} and waits (blocking the caller thread) <strong>indefinitely</strong> until a
     * {@code result} event is fired or a {@code failure} event is fired by the upstream uni.
     * <p>
     * If the {@link Uni} fires a result, it returns that result wrapped into an {@link Optional}. If the result is
     * {@code null} the returned optional is empty.
     * If the {@link Uni} fires a failure, the original exception is thrown (wrapped in
     * a {@link java.util.concurrent.CompletionException} it's a checked exception).
     * <p>
     * Note that each call to this method triggers a new subscription.
     *
     * @return the result from the {@link Uni} wrapped into an {@link Optional}, empty if the {@link Uni} is resolved
     * with {@code null}
     */
    public Optional<T> indefinitely() {
        return atMost(null);
    }

    /**
     * Subscribes to the {@link Uni} and waits (blocking the caller thread) <strong>at most</strong> the given duration
     * until a result or failure is fired by the upstream uni.
     * <p>
     * If the {@link Uni} fires a result, it returns that result wrapped into an {@link Optional}. If the result is
     * {@code null} the returned optional is empty.
     * If the {@link Uni} fires a failure, the original exception is thrown (wrapped in
     * a {@link java.util.concurrent.CompletionException} it's a checked exception).
     * If the timeout is reached before completion, a {@link TimeoutException} is thrown.
     * <p>
     * Note that each call to this method triggers a new subscription.
     *
     * @param duration the duration, must not be {@code null}, must not be negative or zero.
     * @return the result from the {@link Uni}, potentially {@code null}
     */
    public Optional<T> atMost(Duration duration) {
        return Optional.ofNullable(UniBlockingAwait.await(upstream, duration));
    }


}
