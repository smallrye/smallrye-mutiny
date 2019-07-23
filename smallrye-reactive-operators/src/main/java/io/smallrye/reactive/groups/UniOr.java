package io.smallrye.reactive.groups;


import io.smallrye.reactive.Uni;
import io.smallrye.reactive.operators.UniOrCombination;

import java.util.Arrays;
import java.util.List;

import static io.smallrye.reactive.helpers.ParameterValidation.nonNull;

public class UniOr<T> {

    private final Uni<T> upstream;

    public UniOr(Uni<T> upstream) {
        this.upstream = nonNull(upstream, "upstream");
    }

    public Uni<T> uni(Uni<T> other) {
        return unis(upstream, other);
    }

    public Uni<T> unis(Uni<T>... other) {
        List<Uni<T>> list = Arrays.asList(other);
        return new UniOrCombination<T>(list);
    }

}
