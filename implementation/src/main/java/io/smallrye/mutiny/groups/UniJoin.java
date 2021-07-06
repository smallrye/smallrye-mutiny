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

    public static final UniJoin SHARED_INSTANCE = new UniJoin();

    private UniJoin() {
        // Do nothing
    }

    @SafeVarargs
    public final <T> UniJoinAllStrategy<T> all(Uni<? extends T>... unis) {
        return all(asList(nonNull(unis, "unis")));
    }

    public final <T> UniJoinAllStrategy<T> all(List<Uni<? extends T>> unis) {
        checkNoneNull(unis);
        return new UniJoinAllStrategy<>(unis);
    }

    public static class UniJoinAllStrategy<T> {

        private final List<Uni<? extends T>> unis;

        private UniJoinAllStrategy(List<Uni<? extends T>> unis) {
            this.unis = unis;
        }

        public Uni<List<T>> andCollectFailures() {
            return Infrastructure.onUniCreation(new UniJoinAll<>(unis, UniJoinAll.Mode.COLLECT_FAILURES));
        }

        public Uni<List<T>> andFailFast() {
            return Infrastructure.onUniCreation(new UniJoinAll<>(unis, UniJoinAll.Mode.FAIL_FAST));
        }
    }

    @SafeVarargs
    public final <T> UniJoinFirstStrategy<T> first(Uni<? extends T>... unis) {
        return first(asList(nonNull(unis, "unis")));
    }

    public final <T> UniJoinFirstStrategy<T> first(List<Uni<? extends T>> unis) {
        checkNoneNull(unis);
        return new UniJoinFirstStrategy<>(unis);
    }

    public static class UniJoinFirstStrategy<T> {

        private final List<Uni<? extends T>> unis;

        private UniJoinFirstStrategy(List<Uni<? extends T>> unis) {
            this.unis = unis;
        }

        public Uni<T> toEmit() {
            return Infrastructure.onUniCreation(new UniJoinFirst<>(unis, UniJoinFirst.Mode.FIRST_TO_EMIT));
        }

        public Uni<T> withItem() {
            return Infrastructure.onUniCreation(new UniJoinFirst<>(unis, UniJoinFirst.Mode.FIRST_WITH_ITEM));
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

        public UniJoinAllStrategy<T> joinAll() {
            return UniJoin.this.all(unis);
        }

        public UniJoinFirstStrategy<T> joinFirst() {
            return UniJoin.this.first(unis);
        }
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
}
