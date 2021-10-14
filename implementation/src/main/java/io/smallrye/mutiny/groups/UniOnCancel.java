package io.smallrye.mutiny.groups;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.function.Supplier;

import io.smallrye.common.annotation.CheckReturnValue;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.ParameterValidation;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.uni.UniOnCancellation;
import io.smallrye.mutiny.operators.uni.UniOnCancellationCall;

public class UniOnCancel<T> {

    private final Uni<T> upstream;

    public UniOnCancel(Uni<T> upstream) {
        this.upstream = ParameterValidation.nonNull(upstream, "upstream");
    }

    /**
     * Attaches an action executed when the subscription is cancelled.
     * The upstream is not cancelled yet, but will be cancelled when the callback completes.
     * Note that if the callback throws an exception then it will be discarded.
     *
     * @param action the action, must not be {@code null}
     * @return a new {@link Uni}
     */
    @CheckReturnValue
    public Uni<T> invoke(Runnable action) {
        Runnable actual = Infrastructure.decorate(nonNull(action, "action"));
        return Infrastructure.onUniCreation(new UniOnCancellation<>(upstream, actual));
    }

    /**
     * Attaches an action executed when the subscription is cancelled.
     * The upstream is not cancelled yet, but will be cancelled when the returned {@link Uni} completes.
     * The supplier must not return {@code null}.
     * Note that the result or the failure of the {@link Uni} will be discarded.
     *
     * @param supplier the {@link Uni} supplier, must not return {@code null}.
     * @return a new {@link Uni}
     */
    @CheckReturnValue
    public Uni<T> call(Supplier<Uni<?>> supplier) {
        Supplier<Uni<?>> actual = Infrastructure.decorate(nonNull(supplier, "supplier"));
        return Infrastructure.onUniCreation(new UniOnCancellationCall<>(upstream, actual));
    }
}
