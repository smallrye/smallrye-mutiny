package io.smallrye.mutiny.groups;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;
import static io.smallrye.mutiny.helpers.ParameterValidation.validate;

import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Supplier;

import io.smallrye.common.annotation.CheckReturnValue;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.TimeoutException;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.multi.MultiFailOnItemTimeout;

public class MultiOnItemTimeout<T> {

    private final Multi<T> failure;
    private final Duration timeout;
    private final ScheduledExecutorService executor;

    public MultiOnItemTimeout(Multi<T> upstream, Duration timeout, ScheduledExecutorService executor) {
        this.failure = nonNull(upstream, "upstream");
        this.timeout = validate(timeout, "timeout");
        this.executor = executor;
    }

    /**
     * Configures on which executor the timeout is measured.
     * Note that this executor is also going to be used to fire the {@code Timeout} failure event.
     *
     * @param executor the executor to use, must not be {@code null}
     * @return a new {@link MultiOnItemTimeout}
     */
    public MultiOnItemTimeout<T> on(ScheduledExecutorService executor) {
        return new MultiOnItemTimeout<>(failure, timeout, nonNull(executor, "executor"));
    }

    @CheckReturnValue
    public Multi<T> fail() {
        return Infrastructure.onMultiCreation(failWith(TimeoutException::new));
    }

    @CheckReturnValue
    public Multi<T> failWith(Throwable failure) {
        return failWith(() -> failure);
    }

    @CheckReturnValue
    public Multi<T> failWith(Supplier<? extends Throwable> supplier) {
        Supplier<? extends Throwable> actual = Infrastructure.decorate(nonNull(supplier, "supplier"));
        return Infrastructure.onMultiCreation(new MultiFailOnItemTimeout<>(failure, timeout, actual, executor));
    }

    /**
     * Produces a new {@link Multi} firing a completion when the current {@link Multi} does not
     * emit an item before the timeout.
     *
     * @return the new {@link Multi}
     */
    @CheckReturnValue
    public Multi<T> recoverWithCompletion() {
        return fail().onFailure(TimeoutException.class).recoverWithCompletion();
    }

    /**
     * Produces a new {@link Multi} providing a fallback {@link Multi} when the current {@link Multi} times out. The fallback
     * is produced using the given supplier, and is called when the failure is caught. The produced {@link Multi} is used
     * instead of the current {@link Multi}.
     *
     * @param supplier the fallback supplier, must not be {@code null}, must not produce {@code null}
     * @return the new {@link Multi}
     */
    @CheckReturnValue
    public Multi<T> recoverWithMulti(Supplier<Multi<? extends T>> supplier) {
        // Decoration happens in `recoverWithMulti`
        return fail().onFailure(TimeoutException.class).recoverWithMulti(supplier);
    }

    /**
     * Produces a new {@link Multi} providing a fallback {@link Multi} when the current {@link Multi} times out. The fallback
     * {@link Multi} is used instead of the current {@link Multi}.
     *
     * @param fallback the fallback {@link Multi}, must not be {@code null}
     * @return the new {@link Multi}
     */
    @CheckReturnValue
    public Multi<T> recoverWithMulti(Multi<? extends T> fallback) {
        return fail().onFailure(TimeoutException.class).recoverWithMulti(fallback);
    }
}
