package io.smallrye.mutiny.groups;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.ParameterValidation;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.UniOnCancellation;
import io.smallrye.mutiny.operators.UniOnCancellationInvokeUni;

import java.util.function.Supplier;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

public class UniOnCancel<T> {

    private final Uni<T> upstream;

    public UniOnCancel(Uni<T> upstream) {
        this.upstream = ParameterValidation.nonNull(upstream, "upstream");
    }

    public Uni<T> invoke(Runnable action) {
        return Infrastructure.onUniCreation(new UniOnCancellation<>(upstream, nonNull(action, "action")));
    }

    public Uni<T> invokeUni(Supplier<Uni<?>> supplier) {
        return Infrastructure.onUniCreation(new UniOnCancellationInvokeUni<>(upstream, nonNull(supplier, "supplier")));
    }
}
