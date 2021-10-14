package io.smallrye.mutiny.groups;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import io.smallrye.common.annotation.CheckReturnValue;
import io.smallrye.mutiny.CompositeException;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.tuples.Tuple2;
import io.smallrye.mutiny.tuples.Tuples;

/**
 * Configures the combination of 2 {@link Uni unis}.
 *
 * @param <T1> the type of item of the first {@link Uni}
 * @param <T2> the type of item of the second {@link Uni}
 */
public class UniAndGroup2<T1, T2> extends UniAndGroupIterable<T1> {

    public UniAndGroup2(Uni<? extends T1> source, Uni<? extends T2> other) {
        super(source, Collections.singletonList(other), false);
    }

    /**
     * Configure the processing to wait until all the {@link Uni unis} to fire an event (item or failure) before
     * firing the failure. If several failures have been collected, a {@link CompositeException} is fired wrapping
     * the different failures.
     *
     * @return the current {@link UniAndGroup2}
     */
    @CheckReturnValue
    public UniAndGroup2<T1, T2> collectFailures() {
        super.collectFailures();
        return this;
    }

    /**
     * @return the resulting {@link Uni}. The items are combined into a {@link Tuple2 Tuple2&lt;T1, T2&gt;}.
     */
    @CheckReturnValue
    public Uni<Tuple2<T1, T2>> asTuple() {
        return combine(Tuple2::of);
    }

    /**
     * Creates the resulting {@link Uni}. The items are combined using the given combinator function.
     *
     * @param combinator the combinator function, must not be {@code null}
     * @param <O> the type of item
     * @return the resulting {@link Uni}. The items are combined into a {@link Tuple2 Tuple2&lt;T1, T2&gt;}.
     */
    @CheckReturnValue
    public <O> Uni<O> combinedWith(BiFunction<T1, T2, O> combinator) {
        BiFunction<T1, T2, O> actual = Infrastructure.decorate(nonNull(combinator, "combinator"));
        return combine(actual);
    }

    @SuppressWarnings("unchecked")
    private <O> Uni<O> combine(BiFunction<T1, T2, O> combinator) {
        Function<List<?>, O> function = list -> {
            Tuples.ensureArity(list, 2);
            T1 item1 = (T1) list.get(0);
            T2 item2 = (T2) list.get(1);
            return combinator.apply(item1, item2);
        };
        return super.combinedWith(function);
    }

}
