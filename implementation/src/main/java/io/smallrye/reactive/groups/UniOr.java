package io.smallrye.reactive.groups;

import static io.smallrye.reactive.helpers.ParameterValidation.nonNull;

import java.util.Arrays;
import java.util.List;

import io.smallrye.reactive.Uni;
import io.smallrye.reactive.infrastructure.Infrastructure;
import io.smallrye.reactive.operators.UniOrCombination;

public class UniOr<T> {

    private final Uni<T> upstream;

    public UniOr(Uni<T> upstream) {
        this.upstream = nonNull(upstream, "upstream");
    }

    @SuppressWarnings("unchecked")
    public Uni<T> uni(Uni<T> other) {
        return unis(upstream, other);
    }

    public Uni<T> unis(Uni<T>... other) {
        List<Uni<T>> list = Arrays.asList(other);
        return Infrastructure.onUniCreation(new UniOrCombination<>(list));
    }

}
