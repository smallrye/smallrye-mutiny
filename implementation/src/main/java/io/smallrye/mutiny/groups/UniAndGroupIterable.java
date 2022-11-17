package io.smallrye.mutiny.groups;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import io.smallrye.common.annotation.CheckReturnValue;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.ParameterValidation;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.uni.UniAndCombination;

public class UniAndGroupIterable<T1> {

    private final Uni<? extends T1> source;
    private final List<? extends Uni<?>> unis;

    private boolean collectFailures;
    private int concurrency = -1;

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

    @CheckReturnValue
    public UniAndGroupIterable<T1> collectFailures() {
        collectFailures = true;
        return this;
    }

    /**
     * Limit the number of concurrent upstream subscriptions.
     * <p>
     * When not specified all upstream {@link Uni} are being subscribed when the combining {@link Uni} is subscribed.
     * <p>
     * Setting a limit is useful when you have a large number of {@link Uni} to combine and their simultaneous
     * subscriptions might overwhelm resources (e.g., database connections, etc).
     *
     * @param level the concurrency level, must be strictly positive
     * @return an object to configure the combination logic
     */
    @CheckReturnValue
    public UniAndGroupIterable<T1> usingConcurrencyOf(int level) {
        this.concurrency = ParameterValidation.positive(level, "level");
        return this;
    }

    /**
     * Combine the items emitted by the {@link Uni unis}, and emit the result when all {@link Uni unis} have
     * successfully completed. In case of failure, the failure is propagated.
     *
     * @param function the combination function
     * @param <O> the combination value type
     * @return the new {@link Uni}
     */
    @CheckReturnValue
    public <O> Uni<O> combinedWith(Function<List<?>, O> function) {
        Function<List<?>, O> actual = Infrastructure.decorate(nonNull(function, "function"));
        return Infrastructure
                .onUniCreation(new UniAndCombination<>(source, unis, actual, collectFailures, concurrency));
    }

    /**
     * Combine the items emitted by the {@link Uni unis}, and emit the result when all {@link Uni unis} have
     * successfully completed. In case of failure, the failure is propagated.
     * <p>
     * This method is a convenience wrapper for {@link #combinedWith(Function)} but with the assumption that all items
     * have {@code I} as a super type, which saves you a cast in the combination function.
     * If the cast fails then the returned {@link Uni} fails with a {@link ClassCastException}.
     *
     * @param superType the super type of all items
     * @param function the combination function
     * @param <O> the combination value type
     * @param <I> the super type of all items
     * @return the new {@link Uni}
     */
    @SuppressWarnings("unchecked")
    @CheckReturnValue
    public <O, I> Uni<O> combinedWith(Class<I> superType, Function<List<I>, O> function) {
        return combinedWith((Function) function);
    }

    /**
     * Discards the items emitted by the combined {@link Uni unis}, and just emits {@code null} when all the
     * {@link Uni unis} have successfully completed. In case of failure, the failure is propagated.
     *
     * @return the {@code Uni Uni<Void>} emitting {@code null} when all the {@link Uni unis} have completed, or propagating
     *         the failure.
     */
    @CheckReturnValue
    public Uni<Void> discardItems() {
        return Infrastructure
                .onUniCreation(new UniAndCombination<>(source, unis, x -> null, collectFailures, concurrency));
    }

}
