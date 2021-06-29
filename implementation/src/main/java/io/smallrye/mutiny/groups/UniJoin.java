package io.smallrye.mutiny.groups;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;
import static java.util.Arrays.asList;

import java.util.List;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.uni.builders.UniJoinAll;

public class UniJoin {

    @SafeVarargs
    public final <T> Uni<List<T>> all(Uni<? extends T>... unis) {
        return all(asList(nonNull(unis, "unis")));
    }

    public final <T> Uni<List<T>> all(List<Uni<? extends T>> unis) {
        nonNull(unis, "unis");
        int index = 0;
        for (Uni<? extends T> uni : unis) {
            if (uni == null) {
                throw new IllegalArgumentException("The uni at index " + index + " is null");
            }
            index++;
        }
        return Infrastructure.onUniCreation(new UniJoinAll<>(unis));
    }
}
