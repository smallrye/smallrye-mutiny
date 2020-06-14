package io.smallrye.mutiny.groups;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.UniOrCombination;

public class UniOr<T> {

    private final Uni<T> upstream;

    public UniOr(Uni<T> upstream) {
        this.upstream = nonNull(upstream, "upstream");
    }

    public Uni<T> uni(Uni<T> other) {
        return unis(upstream, other);
    }

    @SafeVarargs
    public final Uni<T> unis(Uni<T>... other) {
        List<Uni<T>> list = new ArrayList<>();
        list.add(upstream);
        list.addAll(Arrays.asList(other));
        return Infrastructure.onUniCreation(new UniOrCombination<>(list));
    }

}
