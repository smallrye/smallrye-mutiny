package io.smallrye.reactive.groups;

import io.smallrye.reactive.CompositeException;
import io.smallrye.reactive.Uni;
import io.smallrye.reactive.tuples.Pair;
import io.smallrye.reactive.tuples.Tuples;

import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Configuresthe combination of 2 {@link Uni unis}.
 *
 * @param <T1> the type of result of the first {@link Uni}
 * @param <T2> the type of result of the second {@link Uni}
 */
public class UniAndGroup2<T1, T2> extends UniAndGroupIterable<T1> {


    public UniAndGroup2(Uni<? extends T1> source, Uni<? extends T2> other) {
        super(source, Collections.singletonList(other), false);
    }

    /**
     * Configure the processing to wait until all the {@link Uni unis} to fires an event (result or failure) before
     * firing the failure. If several failures have been collected, a {@link CompositeException} is fired wrapping
     * the different failures.
     *
     * @return the current {@link UniAndGroup2}
     */
    public UniAndGroup2<T1, T2> awaitCompletion() {
        super.awaitCompletion();
        return this;
    }

    /**
     * @return the resulting {@link Uni}. The results are combined into a {@link Pair Pair&lt;T1, T2&gt;}.
     */
    public Uni<Pair<T1, T2>> asPair() {
        return combinedWith(Pair::of);
    }

    /**
     * Creates the resulting {@link Uni}. The results are combined using the given combinator function.
     *
     * @param combinator the combinator function, must not be {@code null}
     * @param <O>        the type of result
     * @return the resulting {@link Uni}. The result are combined into a {@link Pair Pair&lt;T1, T2&gt;}.
     */
    @SuppressWarnings("unchecked")
    public <O> Uni<O> combinedWith(BiFunction<T1, T2, O> combinator) {
        Function<List<?>, O> function = list -> {
            Tuples.ensureArity(list, 2);
            T1 item1 = (T1) list.get(0);
            T2 item2 = (T2) list.get(1);
            return combinator.apply(item1, item2);
        };
        return super.combinedWith(function);
    }


}
