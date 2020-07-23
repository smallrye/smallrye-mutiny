package io.smallrye.mutiny.groups;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.function.Supplier;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.ParameterValidation;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.UniOnCancellation;
import io.smallrye.mutiny.operators.UniOnCancellationInvokeUni;

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
    public Uni<T> invoke(Runnable action) {
        return Infrastructure.onUniCreation(new UniOnCancellation<>(upstream, nonNull(action, "action")));
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
    public Uni<T> invokeUni(Supplier<Uni<?>> supplier) {
        return Infrastructure.onUniCreation(new UniOnCancellationInvokeUni<>(upstream, nonNull(supplier, "supplier")));
    }
}
