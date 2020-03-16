package io.smallrye.mutiny.groups;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;
import static io.smallrye.mutiny.helpers.ParameterValidation.validate;

import java.time.Duration;
import java.util.function.Function;
import java.util.function.Predicate;

import org.reactivestreams.Publisher;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.ExponentialBackoff;
import io.smallrye.mutiny.helpers.ParameterValidation;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.multi.MultiRetryOp;
import io.smallrye.mutiny.operators.multi.MultiRetryWhenOp;

public class MultiRetry<T> {

    private final Multi<T> upstream;
    private Duration initialBackOff = Duration.ofSeconds(1);
    private Duration maxBackoff = ExponentialBackoff.MAX_BACKOFF;
    private double jitter = ExponentialBackoff.DEFAULT_JITTER;
    private boolean backOffConfigured = false;

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
        ParameterValidation.positive(numberOfAttempts, "numberOfAttempts");
        if (backOffConfigured) {
            Function<Multi<Throwable>, Publisher<Long>> whenStreamFactory = ExponentialBackoff
                    .randomExponentialBackoffFunction(numberOfAttempts, initialBackOff, maxBackoff, jitter,
                            Infrastructure.getDefaultWorkerPool());
            return Infrastructure.onMultiCreation(
                    new MultiRetryWhenOp<>(upstream, whenStreamFactory));
        } else {
            return Infrastructure.onMultiCreation(new MultiRetryOp<>(upstream, numberOfAttempts));
        }

    }

    /**
     * Produces a {@code Multi} resubscribing to the current {@link Multi} until the given predicate returns {@code false}.
     * The predicate is called with the failure emitted by the current {@link Multi}.
     *
     * @param predicate the predicate that determines if a re-subscription may happen in case of a specific failure,
     *        must not be {@code null}. If the predicate returns {@code true} for the given failure, a
     *        re-subscription is attempted.
     * @return the new {@code Multi} instance
     */
    public Multi<T> until(Predicate<? super Throwable> predicate) {
        ParameterValidation.nonNull(predicate, "predicate");
        if (backOffConfigured) {
            throw new IllegalArgumentException(
                    "Invalid retry configuration, `until` cannot be used with a back-off configuration");
        }
        Function<Multi<Throwable>, Publisher<Long>> whenStreamFactory = stream -> stream.onItem()
                .produceUni(failure -> Uni.createFrom().<Long> emitter(emitter -> {
                    try {
                        if (predicate.test(failure)) {
                            emitter.complete(1L);
                        } else {
                            emitter.fail(failure);
                        }
                    } catch (Throwable ex) {
                        emitter.fail(ex);
                    }
                }))
                .concatenate();
        return Infrastructure.onMultiCreation(new MultiRetryWhenOp<>(upstream, whenStreamFactory));
    }

    /**
     * Produces a {@link Multi} resubscribing to the current {@link Multi} when the {@link Publisher} produced by the
     * given method emits an item.
     * As {@link #atMost(long)}, on every failure, it re-subscribes. However, a <em>delay</em> is introduced before
     * re-subscribing. The re-subscription happens when the produced streams emits an item. If this stream fails,
     * the downstream gets a failure. It the streams completes, the downstream completes.
     *
     * @param whenStreamFactory the function used to produce the stream triggering the re-subscription, must not be
     *        {@code null}, must not produce {@code null}
     * @return a new {@link Multi} retrying re-subscribing to the current {@link Multi} when the companion stream,
     *         produced by {@code whenStreamFactory} emits an item.
     */
    public Multi<T> when(Function<Multi<Throwable>, ? extends Publisher<?>> whenStreamFactory) {
        if (backOffConfigured) {
            throw new IllegalArgumentException(
                    "Invalid retry configuration, `when` cannot be used with a back-off configuration");
        }
        return Infrastructure.onMultiCreation(new MultiRetryWhenOp<>(upstream, whenStreamFactory));
    }

    /**
     * Configures a back-off delay between to attempt to re-subscribe. A random factor (jitter) is applied to increase
     * the delay when several failures happen.
     *
     * @param initialBackOff the initial back-off duration, must not be {@code null}, must not be negative.
     * @return this object to configure the retry policy.
     */
    public MultiRetry<T> withBackOff(Duration initialBackOff) {
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
    public MultiRetry<T> withBackOff(Duration initialBackOff, Duration maxBackOff) {
        this.backOffConfigured = true;
        this.initialBackOff = validate(initialBackOff, "initialBackOff");
        this.maxBackoff = validate(maxBackOff, "maxBackOff");
        return this;
    }

    /**
     * Configures the random factor when using back-off. By default, it's set to 0.5.
     *
     * @param jitter the jitter. Must be in [0.0, 1.0]
     * @return this object to configure the retry policy.
     */
    public MultiRetry<T> withJitter(double jitter) {
        if (jitter < 0 || jitter > 1.0) {
            throw new IllegalArgumentException("Invalid `jitter`, the value must be in [0.0, 1.0]");
        }
        this.backOffConfigured = true;
        this.jitter = jitter;
        return this;
    }
}
