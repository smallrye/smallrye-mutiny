package io.smallrye.mutiny.groups;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.uni.UniAndCombination;

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

    @SuppressWarnings({ "unchecked", "rawtypes" })
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
        Function<List<?>, O> actual = Infrastructure.decorate(nonNull(function, "function"));
        return Infrastructure
                .onUniCreation(new UniAndCombination<>(source, unis, actual, collectFailures));
    }

    /**
     * Discards the items emitted by the combined {@link Uni unis}, and just emits {@code null} when all the
     * {@link Uni unis} have completed successfully. In the case of failure, the failure is propagated.
     *
     * @return the {@code Uni Uni<Void>} emitting {@code null} when all the {@link Uni unis} have completed, or propagating
     *         the failure.
     */
    public Uni<Void> discardItems() {
        return Infrastructure
                .onUniCreation(new UniAndCombination<>(source, unis, x -> null, collectFailures));
    }

}
