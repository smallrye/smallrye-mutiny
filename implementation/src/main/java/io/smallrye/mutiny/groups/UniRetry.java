package io.smallrye.mutiny.groups;

import java.util.function.Predicate;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.UniRetryAtMost;

public class UniRetry<T> {

    private final Uni<T> upstream;
    private final Predicate<? super Throwable> predicate;

    public UniRetry(Uni<T> upstream, Predicate<? super Throwable> predicate) {
        this.upstream = upstream;
        this.predicate = predicate;
    }

    /**
     * Produces a {@link Uni} resubscribing to the current {@link Uni} until it gets an item (potentially {@code null})
     * On every failure, it re-subscribes, indefinitely.
     *
     * @return the {@link Uni}
     */
    public Uni<T> indefinitely() {
        return atMost(Long.MAX_VALUE);
    }

    /**
     * Produces a {@link Uni} resubscribing to the current {@link Uni} at most {@code numberOfAttempts} time, until it
     * gets an item (potentially {@code null}). On every failure, it re-subscribes.
     * <p>
     * If the number of attempt is reached, the last failure is propagated.
     *
     * @param numberOfAttempts the number of attempt, must be greater than zero
     * @return a new {@link Uni} retrying at most {@code numberOfAttempts} times to subscribe to the current {@link Uni}
     *         until it gets an item. When the number of attempt is reached, the last failure is propagated.
     */
    public Uni<T> atMost(long numberOfAttempts) {
        return Infrastructure.onUniCreation(new UniRetryAtMost<>(upstream, predicate, numberOfAttempts));
    }

    public Uni<T> until(Predicate<? super Throwable> predicate) {
        throw new UnsupportedOperationException("not yet implemented");
    }

    // TODO backOff such as withInitialBackOff(Duration first) withMaxBackOff(Duration max) withJitter(double jitter)

    // TODO add a variant to until taking a Publisher or an Uni. Completion of these indicates that the retry can happen
}
