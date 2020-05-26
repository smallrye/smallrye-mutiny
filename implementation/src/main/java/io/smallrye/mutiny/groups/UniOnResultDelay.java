package io.smallrye.mutiny.groups;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Function;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.UniDelayOnItem;
import io.smallrye.mutiny.operators.UniDelayUntil;

/**
 * Configures the delay applied to the item emission.
 * It allows delaying the item emitted by the previous {@code Uni} to its downstream.
 *
 * @param <T> the type of item
 */
public class UniOnResultDelay<T> {

    private final Uni<T> upstream;
    private ScheduledExecutorService executor;

    /**
     * Creates a new {@code UniOnResultDelay} instance.
     *
     * @param upstream the upstream uni
     * @param executor the executor, can be {@code null}, if {@code null} used the default worker executor.
     */
    public UniOnResultDelay(Uni<T> upstream, ScheduledExecutorService executor) {
        this.upstream = upstream;
        this.executor = executor == null ? Infrastructure.getDefaultWorkerPool() : executor;
    }

    /**
     * Configures the executor which is used to <i>wait</i> for the delay duration.
     *
     * @param executor the executor, must not be {@code null}
     * @return this {@code UniOnResultDelay}.
     */
    public UniOnResultDelay<T> onExecutor(ScheduledExecutorService executor) {
        this.executor = nonNull(executor, "executor");
        return this;
    }

    /**
     * Delays the item emission by a specific duration.
     *
     * @param duration the duration of the delay, must not be {@code null}, must be strictly positive.
     * @return the produced {@link Uni}
     */
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
    public Uni<T> until(Function<? super T, ? extends Uni<?>> function) {
        return Infrastructure.onUniCreation(new UniDelayUntil<>(upstream, function, executor));
    }

}
