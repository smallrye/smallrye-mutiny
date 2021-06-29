package io.smallrye.mutiny.groups;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;
import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.List;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.uni.builders.UniJoinAll;
import io.smallrye.mutiny.operators.uni.builders.UniJoinFirst;

public class UniJoin {

    @SafeVarargs
    public final <T> Uni<List<T>> all(Uni<? extends T>... unis) {
        return all(asList(nonNull(unis, "unis")));
    }

    public final <T> Uni<List<T>> all(List<Uni<? extends T>> unis) {
        checkNoneNull(unis);
        return Infrastructure.onUniCreation(new UniJoinAll<>(unis));
    }

    @SafeVarargs
    public final <T> Uni<T> first(Uni<? extends T>... unis) {
        return first(asList(nonNull(unis, "unis")));
    }

    public final <T> Uni<T> first(List<Uni<? extends T>> unis) {
        checkNoneNull(unis);
        return Infrastructure.onUniCreation(new UniJoinFirst<>(unis));
    }

    private <T> void checkNoneNull(List<Uni<? extends T>> unis) {
        nonNull(unis, "unis");
        int index = 0;
        for (Uni<? extends T> uni : unis) {
            if (uni == null) {
                throw new IllegalArgumentException("The uni at index " + index + " is null");
            }
            index++;
        }
    }

    public <T> UniJoinBuilder<T> builder() {
        return new UniJoinBuilder<>();
    }

    public class UniJoinBuilder<T> {

        private final List<Uni<? extends T>> unis = new ArrayList<>();

        public UniJoinBuilder<T> add(Uni<? extends T> uni) {
            unis.add(uni);
            return this;
        }

        public Uni<List<T>> joinAll() {
            return UniJoin.this.all(unis);
        }

        public Uni<T> joinFirst() {
            return UniJoin.this.first(unis);
        }
    }
}
