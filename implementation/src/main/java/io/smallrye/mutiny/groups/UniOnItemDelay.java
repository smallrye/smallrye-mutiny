package io.smallrye.mutiny.groups;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Function;

import io.smallrye.common.annotation.CheckReturnValue;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.uni.UniDelayOnItem;
import io.smallrye.mutiny.operators.uni.UniDelayUntil;

/**
 * Configures the delay applied to the item emission.
 * It allows delaying the item emitted by the previous {@code Uni} to its downstream.
 *
 * @param <T> the type of item
 */
public class UniOnItemDelay<T> {

    private final Uni<T> upstream;
    private ScheduledExecutorService executor;

    /**
     * Creates a new {@code UniOnItemDelay} instance.
     *
     * @param upstream the upstream uni
     * @param executor the executor, can be {@code null}, if {@code null} used the default worker executor.
     */
    public UniOnItemDelay(Uni<T> upstream, ScheduledExecutorService executor) {
        this.upstream = upstream;
        this.executor = executor == null ? Infrastructure.getDefaultWorkerPool() : executor;
    }

    /**
     * Configures the executor which is used to <i>wait</i> for the delay duration.
     *
     * @param executor the executor, must not be {@code null}
     * @return this {@code UniOnItemDelay}.
     */
    @CheckReturnValue
    public UniOnItemDelay<T> onExecutor(ScheduledExecutorService executor) {
        this.executor = nonNull(executor, "executor");
        return this;
    }

    /**
     * Delays the item emission by a specific duration.
     *
     * @param duration the duration of the delay, must not be {@code null}, must be strictly positive.
     * @return the produced {@link Uni}
     */
    @CheckReturnValue
    public Uni<T> by(Duration duration) {
        return Infrastructure.onUniCreation(new UniDelayOnItem<>(upstream, duration, executor));
    }

    /**
     * Delays the item emission until the {@link Uni} produced by the given {@link Function} emits an item
     * (potentially {@code null})
     * <p>
     * When the upstream emits its item, the passed function is called. The returned Uni is subscribed using the
     * configured executor. Once this Uni emits its item, the item emitted by upstream is propagated downstream. If
     * the Uni fails, the failure is propagated instead.
     *
     * @param function the function, must not be {@code null}
     * @return the produced {@link Uni}.
     */
    @CheckReturnValue
    public Uni<T> until(Function<? super T, Uni<?>> function) {
        Function<? super T, Uni<?>> actual = Infrastructure.decorate(nonNull(function, "function"));
        return Infrastructure.onUniCreation(new UniDelayUntil<>(upstream, actual, executor));
    }

}
