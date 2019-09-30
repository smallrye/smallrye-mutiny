package io.smallrye.reactive.unimulti.groups;

import io.smallrye.reactive.unimulti.Uni;
import io.smallrye.reactive.unimulti.infrastructure.Infrastructure;
import io.smallrye.reactive.unimulti.operators.UniAndCombination;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static io.smallrye.reactive.unimulti.helpers.ParameterValidation.nonNull;

public class UniAndGroupIterable<T1> {

    private final Uni<? extends T1> source;
    private final List<? extends Uni<?>> unis;

    private boolean collectFailures;

    public UniAndGroupIterable(Iterable<? extends Uni<?>> iterable) {
        this(null, iterable, false);
    }

    public UniAndGroupIterable(Uni<? extends T1> source, Iterable<? extends Uni<?>> iterable) {
        this(source, iterable, false);
    }

    @SuppressWarnings("unchecked")
    public UniAndGroupIterable(Uni<? extends T1> source, Iterable<? extends Uni<?>> iterable, boolean collectFailures) {
        this.source = source;
        List<? extends Uni<?>> others;
        if (iterable instanceof List) {
            others = (List) iterable;
        } else {
            others = StreamSupport.stream(iterable.spliterator(), false).collect(Collectors.toList());
        }
        this.unis = others;
        this.collectFailures = collectFailures;
    }

    public UniAndGroupIterable<T1> collectFailures() {
        collectFailures = true;
        return this;
    }

    public <O> Uni<O> combinedWith(Function<List<?>, O> function) {
        return Infrastructure
                .onUniCreation(new UniAndCombination<>(source, unis, nonNull(function, "function"), collectFailures)
                );
    }

}
