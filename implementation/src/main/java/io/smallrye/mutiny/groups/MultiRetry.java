package io.smallrye.mutiny.groups;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.function.Predicate;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.multi.MultiRetryOp;

public class MultiRetry<T> {

    private final Multi<T> upstream;

    public MultiRetry(Multi<T> upstream) {
        this.upstream = nonNull(upstream, "upstream");
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
     *         {@link Multi} until it gets an item. When the number of attempt is reached, the last failure is propagated.
     */
    public Multi<T> atMost(long numberOfAttempts) {
        return Infrastructure.onMultiCreation(new MultiRetryOp<>(upstream, numberOfAttempts));
    }

    public Multi<T> until(Predicate<? super Throwable> predicate) {
        throw new UnsupportedOperationException("not yet implemented");
    }

    // TODO backOff such as withInitialBackOff(Duration first) withMaxBackOff(Duration max) withJitter(double jitter)
}
