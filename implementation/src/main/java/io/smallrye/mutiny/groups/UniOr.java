package io.smallrye.mutiny.groups;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.smallrye.common.annotation.CheckReturnValue;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.uni.UniOrCombination;

public class UniOr<T> {

    private final Uni<T> upstream;

    public UniOr(Uni<T> upstream) {
        this.upstream = nonNull(upstream, "upstream");
    }

    @SafeVarargs
    @CheckReturnValue
    public final Uni<T> unis(Uni<T>... other) {
        List<Uni<T>> list = new ArrayList<>();
        list.add(upstream);
        list.addAll(Arrays.asList(other));
        return Infrastructure.onUniCreation(new UniOrCombination<>(list));
    }

}
