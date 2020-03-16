package io.smallrye.mutiny.groups;

import static io.smallrye.mutiny.helpers.ParameterValidation.validate;

import java.time.Duration;
import java.util.function.Function;
import java.util.function.Predicate;

import org.reactivestreams.Publisher;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.ExponentialBackoff;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.UniRetryAtMost;

public class UniRetry<T> {

    private final Uni<T> upstream;
    private final Predicate<? super Throwable> predicate;

    private Duration initialBackOffDuration = Duration.ofSeconds(1);
    private Duration maxBackoffDuration = ExponentialBackoff.MAX_BACKOFF;
    private double jitter = ExponentialBackoff.DEFAULT_JITTER;

    private boolean backOffConfigured = false;

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
     *         until it gets an item. When the number of attempt is reached, the last failure is propagated. If the back-off
     *         has been configured, a delay is introduced between the attempts.
     */
    public Uni<T> atMost(long numberOfAttempts) {
        if (!backOffConfigured) {
            return Infrastructure.onUniCreation(new UniRetryAtMost<>(upstream, predicate, numberOfAttempts));
        } else {
            Function<Multi<Throwable>, Publisher<Long>> factory = ExponentialBackoff
                    .randomExponentialBackoffFunction(numberOfAttempts,
                            initialBackOffDuration, maxBackoffDuration, jitter, Infrastructure.getDefaultWorkerPool());
            return upstream.toMulti().onFailure().retry().when(factory).toUni();
        }
    }

    public Uni<T> until(Predicate<? super Throwable> predicate) {
        throw new UnsupportedOperationException("not yet implemented");
    }

    /**
     * Produces a {@link Uni} resubscribing to the current {@link Uni} when the {@link Publisher} produced by the
     * given method emits an item.
     * As {@link #atMost(long)}, on every failure, it re-subscribes. However, a <em>delay</em> is introduced before
     * re-subscribing. The re-subscription happens when the produced streams emits an item. If this stream fails,
     * the produced {@link Uni} propagates a failure. It the streams completes, the produced {@link Uni} propagates
     * {@code null}.
     *
     * @param whenStreamFactory the function used to produce the stream triggering the re-subscription, must not be
     *        {@code null}, must not produce {@code null}
     * @return a new {@link Uni} retrying re-subscribing to the current {@link Multi} when the companion stream,
     *         produced by {@code whenStreamFactory} emits an item.
     */
    public Uni<T> when(Function<Multi<Throwable>, ? extends Publisher<?>> whenStreamFactory) {
        if (backOffConfigured) {
            throw new IllegalArgumentException(
                    "Invalid retry configuration, `when` cannot be used with a back-off configuration");
        }
        return upstream.toMulti().onFailure().retry().when(whenStreamFactory).toUni();
    }

    /**
     * Configures a back-off delay between to attempt to re-subscribe. A random factor (jitter) is applied to increase
     * the delay when several failures happen.
     *
     * @param initialBackOff the initial back-off duration, must not be {@code null}, must not be negative.
     * @return this object to configure the retry policy.
     */
    public UniRetry<T> withBackOff(Duration initialBackOff) {
        return withBackOff(initialBackOff, ExponentialBackoff.MAX_BACKOFF);
    }

    /**
     * Configures a back-off delay between to attempt to re-subscribe. A random factor (jitter) is applied to increase
     * he delay when several failures happen. The max delays is {@code maxBackOff}.
     *
     * @param initialBackOff the initial back-off duration, must not be {@code null}, must not be negative.
     * @param maxBackOff the max back-off duration, must not be {@code null}, must not be negative.
     * @return this object to configure the retry policy.
     */
    public UniRetry<T> withBackOff(Duration initialBackOff, Duration maxBackOff) {
        this.backOffConfigured = true;
        this.initialBackOffDuration = validate(initialBackOff, "initialBackOff");
        this.maxBackoffDuration = validate(maxBackOff, "maxBackOff");
        return this;
    }

    /**
     * Configures the random factor when using back-off. By default, it's set to 0.5.
     *
     * @param jitter the jitter. Must be in [0.0, 1.0]
     * @return this object to configure the retry policy.
     */
    public UniRetry<T> withJitter(double jitter) {
        if (jitter < 0 || jitter > 1.0) {
            throw new IllegalArgumentException("Invalid `jitter`, the value must be in [0.0, 1.0]");
        }
        this.backOffConfigured = true;
        this.jitter = jitter;
        return this;
    }

}
