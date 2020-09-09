package io.smallrye.mutiny.groups;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.function.Supplier;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.multi.MultiOnCancellationCall;
import io.smallrye.mutiny.operators.multi.MultiOnCancellationInvoke;

public class MultiOnCancel<T> {

    private final Multi<T> upstream;

    public MultiOnCancel(Multi<T> upstream) {
        this.upstream = nonNull(upstream, "upstream");
    }

    /**
     * Attaches an action executed when the subscription is cancelled.
     * The upstream is not cancelled yet, but will be cancelled when the callback completes.
     * Note that if the callback throws an exception then it will be discarded.
     *
     * @param action the action, must not be {@code null}
     * @return a new {@link Multi}
     */
    public Multi<T> invoke(Runnable action) {
        return Infrastructure.onMultiCreation(new MultiOnCancellationInvoke<>(upstream, nonNull(action, "action")));
    }

    /**
     * Attaches an action executed when the subscription is cancelled.
     * The upstream is not cancelled yet, but will be cancelled when the returned {@link Uni} completes.
     * The supplier must not return {@code null}.
     * Note that the result or the failure of the {@link Uni} will be discarded.
     *
     * @param supplier the {@link Uni} supplier, must not return {@code null}.
     * @return a new {@link Multi}
     */
    public Multi<T> call(Supplier<Uni<?>> supplier) {
        return Infrastructure.onMultiCreation(new MultiOnCancellationCall<>(upstream, nonNull(supplier, "supplier")));
    }

    /**
     * Attaches an action executed when the subscription is cancelled.
     * The upstream is not cancelled yet, but will be cancelled when the returned {@link Uni} completes.
     * The supplier must not return {@code null}.
     * Note that the result or the failure of the {@link Uni} will be discarded.
     * 
     * @param supplier the {@link Uni} supplier, must not return {@code null}.
     * @return a new {@link Multi}
     * @deprecated Use {@link #call(Supplier)}
     */
    @Deprecated
    public Multi<T> invokeUni(Supplier<Uni<?>> supplier) {
        return call(supplier);
    }
}
