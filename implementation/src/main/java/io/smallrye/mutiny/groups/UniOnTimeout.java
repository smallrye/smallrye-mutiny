package io.smallrye.mutiny.groups;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;
import static io.smallrye.mutiny.helpers.ParameterValidation.validate;

import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Supplier;

import io.smallrye.common.annotation.CheckReturnValue;
import io.smallrye.mutiny.TimeoutException;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.uni.UniFailOnTimeout;

public class UniOnTimeout<T> {

    private final Uni<T> failure;
    private final Duration timeout;
    private final ScheduledExecutorService executor;

    public UniOnTimeout(Uni<T> upstream, Duration timeout, ScheduledExecutorService executor) {
        this.failure = nonNull(upstream, "upstream");
        this.timeout = timeout;
        this.executor = executor;
    }

    /**
     * Configures the timeout duration.
     *
     * @param timeout the timeout, must not be {@code null}, must be strictly positive.
     * @return a new {@link UniOnTimeout}
     */
    @CheckReturnValue
    public UniOnTimeout<T> after(Duration timeout) {
        return new UniOnTimeout<>(failure, validate(timeout, "timeout"), executor);
    }

    /**
     * Configures on which executor the timeout is measured.
     * Note that this executor is also going to be used to fire the {@code Timeout} failure event.
     *
     * @param executor the executor to use, must not be {@code null}
     * @return a new {@link UniOnTimeout}
     */
    @CheckReturnValue
    public UniOnTimeout<T> on(ScheduledExecutorService executor) {
        return new UniOnTimeout<>(failure, timeout, nonNull(executor, "executor"));
    }

    @CheckReturnValue
    public Uni<T> fail() {
        return Infrastructure.onUniCreation(failWith(TimeoutException::new));
    }

    @CheckReturnValue
    public Uni<T> failWith(Throwable failure) {
        return failWith(() -> failure);
    }

    @CheckReturnValue
    public Uni<T> failWith(Supplier<? extends Throwable> supplier) {
        validate(timeout, "timeout");
        Supplier<? extends Throwable> actual = Infrastructure.decorate(nonNull(supplier, "supplier"));
        return Infrastructure.onUniCreation(new UniFailOnTimeout<>(failure, timeout, actual, executor));
    }

    /**
     * Produces a new {@link Uni} firing a fallback item when the current {@link Uni} the upstream {@link Uni} do not
     * emit an item before the timeout.
     * <p>
     * The fallback item (potentially {@code null}) is used as item by the produced {@link Uni}.
     *
     * @param fallback the fallback value, may be {@code null}
     * @return the new {@link Uni}
     */
    @CheckReturnValue
    public Uni<T> recoverWithItem(T fallback) {
        return fail().onFailure(TimeoutException.class).recoverWithItem(fallback);
    }

    /**
     * Produces a new {@link Uni} firing a fallback item supplied by the given supplier when the current {@link Uni}
     * times out. The produced item (potentially {@code null}) is fired as item by the produced {@link Uni}.
     * Note that if the supplier throws an exception, the produced {@link Uni} emits a failure.
     *
     * @param supplier the fallback supplier, may be {@code null}
     * @return the new {@link Uni}
     */
    @CheckReturnValue
    public Uni<T> recoverWithItem(Supplier<T> supplier) {
        // Decoration happens in `recoverWithItem`
        return fail().onFailure(TimeoutException.class).recoverWithItem(supplier);
    }

    /**
     * Produces a new {@link Uni} providing a fallback {@link Uni} when the current {@link Uni} times out. The fallback
     * is produced using the given supplier, and it called when the failure is caught. The produced {@link Uni} is used
     * instead of the current {@link Uni}.
     *
     * @param supplier the fallback supplier, must not be {@code null}, must not produce {@code null}
     * @return the new {@link Uni}
     */
    @CheckReturnValue
    public Uni<T> recoverWithUni(Supplier<Uni<? extends T>> supplier) {
        // Decoration happens in `recoverWithUni`
        return fail().onFailure(TimeoutException.class).recoverWithUni(supplier);
    }

    /**
     * Produces a new {@link Uni} providing a fallback {@link Uni} when the current {@link Uni} times out. The fallback
     * {@link Uni} is used instead of the current {@link Uni}.
     *
     * @param fallback the fallback {@link Uni}, must not be {@code null}
     * @return the new {@link Uni}
     */
    @CheckReturnValue
    public Uni<T> recoverWithUni(Uni<? extends T> fallback) {
        return fail().onFailure(TimeoutException.class).recoverWithUni(fallback);
    }

}
