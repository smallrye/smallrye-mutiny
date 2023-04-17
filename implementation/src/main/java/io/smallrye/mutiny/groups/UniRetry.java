package io.smallrye.mutiny.groups;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;
import static io.smallrye.mutiny.helpers.ParameterValidation.validate;

import java.time.Duration;
import java.util.concurrent.Flow;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Function;
import java.util.function.Predicate;

import io.smallrye.common.annotation.CheckReturnValue;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.ExponentialBackoff;
import io.smallrye.mutiny.helpers.ParameterValidation;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.uni.UniRetryAtMost;

// TODO This method should be renamed to UniOnFailureRetry, however it's a breaking change.
public class UniRetry<T> {

    private final Uni<T> upstream;
    private final Predicate<? super Throwable> onFailurePredicate;

    private Duration initialBackOffDuration = Duration.ofSeconds(1);
    private Duration maxBackoffDuration = ExponentialBackoff.MAX_BACKOFF;
    private double jitter = ExponentialBackoff.DEFAULT_JITTER;

    private boolean backOffConfigured = false;

    private ScheduledExecutorService executor = null;

    public UniRetry(Uni<T> upstream, Predicate<? super Throwable> onFailurePredicate) {
        this.upstream = upstream;
        this.onFailurePredicate = onFailurePredicate;
    }

    /**
     * Define a scheduled executor other than {@link Infrastructure#getDefaultWorkerPool()} for the time-aware retry
     * policies (e.g., {{@link #withBackOff(Duration)}}.
     *
     * @param executor the scheduled executor, must not be {@code null}
     * @return this instance
     */
    @CheckReturnValue
    public UniRetry<T> withExecutor(ScheduledExecutorService executor) {
        this.executor = nonNull(executor, "executor");
        return this;
    }

    /**
     * Produces a {@link Uni} resubscribing to the current {@link Uni} until it gets an item (potentially {@code null})
     * On every failure, it re-subscribes, indefinitely.
     *
     * @return the {@link Uni}
     */
    @CheckReturnValue
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
    @CheckReturnValue
    public Uni<T> atMost(long numberOfAttempts) {
        if (!backOffConfigured) {
            return Infrastructure.onUniCreation(new UniRetryAtMost<>(upstream, onFailurePredicate, numberOfAttempts));
        } else {
            ScheduledExecutorService pool = (this.executor == null) ? Infrastructure.getDefaultWorkerPool() : this.executor;
            Function<Multi<Throwable>, Flow.Publisher<Long>> factory = ExponentialBackoff
                    .randomExponentialBackoffFunction(numberOfAttempts,
                            initialBackOffDuration, maxBackoffDuration, jitter, pool);
            return upstream.toMulti().onFailure(onFailurePredicate).retry().when(factory).toUni();
        }
    }

    /**
     * Produces a {@link Uni} resubscribing to the current {@link Uni} until {@code expireAt} time or until it
     * gets an item (potentially {@code null}). On every failure, it re-subscribes.
     * <p>
     * If expiration time is passed, the last failure is propagated.
     * Backoff must be configured.
     *
     * @param expireAt absolute time in millis that specifies when to give up
     * @return a new {@link Uni} retrying to subscribe to the current {@link Uni} until it gets an item or until
     *         expiration {@code expireAt}. When the expiration is reached, the last failure is propagated.
     * @throws IllegalArgumentException if back off not configured,
     */
    @CheckReturnValue
    public Uni<T> expireAt(long expireAt) {
        if (!backOffConfigured) {
            throw new IllegalArgumentException(
                    "Invalid retry configuration, `expiresAt/expiresIn` must be used with a back-off configuration");
        }
        ScheduledExecutorService pool = (this.executor == null) ? Infrastructure.getDefaultWorkerPool() : this.executor;
        Function<Multi<Throwable>, Flow.Publisher<Long>> factory = ExponentialBackoff
                .randomExponentialBackoffFunctionExpireAt(expireAt,
                        initialBackOffDuration, maxBackoffDuration, jitter, pool);
        return upstream.toMulti().onFailure(onFailurePredicate).retry().when(factory).toUni();
    }

    /**
     * Produces a {@link Uni} resubscribing to the current {@link Uni} until {@code expireIn} time or until it
     * gets an item (potentially {@code null}). On every failure, it re-subscribes.
     * <p>
     * If expiration time is passed, the last failure is propagated.
     * Backoff must be configured.
     *
     * @param expireIn relative time in millis that specifies when to give up
     * @return a new {@link Uni} retrying to subscribe to the current {@link Uni} until it gets an item or until
     *         expiration {@code expireIn}. When the expiration is reached, the last failure is propagated.
     * @throws IllegalArgumentException if back off not configured,
     */
    @CheckReturnValue
    public Uni<T> expireIn(long expireIn) {
        return expireAt(System.currentTimeMillis() + expireIn);
    }

    /**
     * Produces a {@code Uni} resubscribing to the current {@link Uni} until the given predicate returns {@code false}.
     * The predicate is called with the failure emitted by the current {@link Uni}.
     *
     * @param predicate the predicate that determines if a re-subscription may happen in case of a specific failure,
     *        must not be {@code null}. If the predicate returns {@code true} for the given failure, a
     *        re-subscription is attempted.
     * @return the new {@code Uni} instance
     */
    @CheckReturnValue
    public Uni<T> until(Predicate<? super Throwable> predicate) {
        ParameterValidation.nonNull(predicate, "predicate");
        Function<Multi<Throwable>, Flow.Publisher<Long>> whenStreamFactory = stream -> stream.onItem()
                .transformToUni(failure -> Uni.createFrom().<Long> emitter(emitter -> {
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
        return when(whenStreamFactory);
    }

    /**
     * Produces a {@link Uni} resubscribing to the current {@link Uni} when the {@link Flow.Publisher} produced by the
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
    @CheckReturnValue
    public Uni<T> when(Function<Multi<Throwable>, ? extends Flow.Publisher<?>> whenStreamFactory) {
        if (backOffConfigured) {
            throw new IllegalArgumentException(
                    "Invalid retry configuration, `when` cannot be used with a back-off configuration");
        }
        Function<Multi<Throwable>, ? extends Flow.Publisher<?>> actual = Infrastructure
                .decorate(nonNull(whenStreamFactory, "whenStreamFactory"));
        return upstream.toMulti().onFailure(this.onFailurePredicate).retry().when(actual).toUni();
    }

    /**
     * Configures a back-off delay between to attempt to re-subscribe. A random factor (jitter) is applied to increase
     * the delay when several failures happen.
     *
     * @param initialBackOff the initial back-off duration, must not be {@code null}, must not be negative.
     * @return this object to configure the retry policy.
     */
    @CheckReturnValue
    public UniRetry<T> withBackOff(Duration initialBackOff) {
        return withBackOff(initialBackOff, ExponentialBackoff.MAX_BACKOFF);
    }

    /**
     * Configures a back-off delay between to attempt to re-subscribe. A random factor (jitter) is applied to increase
     * the delay when several failures happen. The max delays is {@code maxBackOff}.
     *
     * @param initialBackOff the initial back-off duration, must not be {@code null}, must not be negative.
     * @param maxBackOff the max back-off duration, must not be {@code null}, must not be negative.
     * @return this object to configure the retry policy.
     */
    @CheckReturnValue
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
    @CheckReturnValue
    public UniRetry<T> withJitter(double jitter) {
        if (jitter < 0 || jitter > 1.0) {
            throw new IllegalArgumentException("Invalid `jitter`, the value must be in [0.0, 1.0]");
        }
        this.backOffConfigured = true;
        this.jitter = jitter;
        return this;
    }

}
