package io.smallrye.reactive.groups;

import io.smallrye.reactive.Multi;
import io.smallrye.reactive.operators.MultiRetryAtMost;

import java.util.function.Predicate;

import static io.smallrye.reactive.helpers.ParameterValidation.nonNull;

public class MultiRetry<T> {

    private final Multi<T> upstream;
    private final Predicate<? super Throwable> predicate;

    public MultiRetry(Multi<T> upstream, Predicate<? super Throwable> predicate) {
        this.upstream = nonNull(upstream, "upstream");
        this.predicate = nonNull(predicate, "predicate");
    }

    /**
     * Produces a {@link Multi} resubscribing to the current {@link Multi} until it gets a items followed
     * by a completion events.
     * <p>
     * On every failure, it re-subscribes, indefinitely.
     *
     * @return the {@link Multi}
     */
    public Multi<T> indefinitely() {
        return atMost(Long.MAX_VALUE);
    }

    /**
     * Produces a {@link Multi} resubscribing to the current {@link Multi} at most {@code numberOfAttempts} time,
     * until it gets items followed by the completion event. On every failure, it re-subscribes.
     * <p>
     * If the number of attempt is reached, the last failure is propagated.
     *
     * @param numberOfAttempts the number of attempt, must be greater than zero
     * @return a new {@link Multi} retrying at most {@code numberOfAttempts} times to subscribe to the current
     * {@link Multi} until it gets an item. When the number of attempt is reached, the last failure is propagated.
     */
    public Multi<T> atMost(long numberOfAttempts) {
        return new MultiRetryAtMost<>(upstream, predicate, numberOfAttempts);
    }

    public Multi<T> until(Predicate<? super Throwable> predicate) {
        throw new UnsupportedOperationException("not yet implemented");
    }

    // TODO backOff such as withInitialBackOff(Duration first) withMaxBackOff(Duration max) withJitter(double jitter)
}
