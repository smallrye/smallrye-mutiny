package io.smallrye.mutiny.groups;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import io.smallrye.common.annotation.CheckReturnValue;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.tuples.Functions;
import io.smallrye.mutiny.tuples.Tuple9;
import io.smallrye.mutiny.tuples.Tuples;

public class UniAndGroup9<T1, T2, T3, T4, T5, T6, T7, T8, T9> extends UniAndGroupIterable<T1> {

    public UniAndGroup9(Uni<? extends T1> source, Uni<? extends T2> o1, Uni<? extends T3> o2, // NOSONAR
            Uni<? extends T4> o3, Uni<? extends T5> o4, Uni<? extends T6> o5,
            Uni<? extends T7> o6, Uni<? extends T8> o7, Uni<? extends T9> o8) {
        super(source, Arrays.asList(o1, o2, o3, o4, o5, o6, o7, o8));
    }

    @Override
    @CheckReturnValue
    public UniAndGroup9<T1, T2, T3, T4, T5, T6, T7, T8, T9> collectFailures() {
        super.collectFailures();
        return this;
    }

    @CheckReturnValue
    public Uni<Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9>> asTuple() {
        return combine(Tuple9::of);
    }

    @CheckReturnValue
    public <O> Uni<O> combinedWith(Functions.Function9<T1, T2, T3, T4, T5, T6, T7, T8, T9, O> combinator) {
        Functions.Function9<T1, T2, T3, T4, T5, T6, T7, T8, T9, O> actual = Infrastructure
                .decorate(nonNull(combinator, "combinator"));
        return combine(actual);
    }

    @SuppressWarnings("unchecked")
    private <O> Uni<O> combine(Functions.Function9<T1, T2, T3, T4, T5, T6, T7, T8, T9, O> combinator) {
        Function<List<?>, O> function = list -> {
            Tuples.ensureArity(list, 9);

            return combinator.apply(
                    (T1) list.get(0),
                    (T2) list.get(1),
                    (T3) list.get(2),
                    (T4) list.get(3),
                    (T5) list.get(4),
                    (T6) list.get(5),
                    (T7) list.get(6),
                    (T8) list.get(7),
                    (T9) list.get(8));
        };
        return super.combinedWith(function);
    }

}
