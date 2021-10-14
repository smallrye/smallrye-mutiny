package io.smallrye.mutiny.groups;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import io.smallrye.common.annotation.CheckReturnValue;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.tuples.*;

public class UniAndGroup7<T1, T2, T3, T4, T5, T6, T7> extends UniAndGroupIterable<T1> {

    public UniAndGroup7(Uni<? extends T1> source, Uni<? extends T2> o1, Uni<? extends T3> o2,
            Uni<? extends T4> o3, Uni<? extends T5> o4, Uni<? extends T6> o5,
            Uni<? extends T7> o6) {
        super(source, Arrays.asList(o1, o2, o3, o4, o5, o6));
    }

    @Override
    @CheckReturnValue
    public UniAndGroup7<T1, T2, T3, T4, T5, T6, T7> collectFailures() {
        super.collectFailures();
        return this;
    }

    @CheckReturnValue
    public Uni<Tuple7<T1, T2, T3, T4, T5, T6, T7>> asTuple() {
        return combinedWith(Tuple7::of);
    }

    @CheckReturnValue
    public <O> Uni<O> combinedWith(Functions.Function7<T1, T2, T3, T4, T5, T6, T7, O> combinator) {
        Functions.Function7<T1, T2, T3, T4, T5, T6, T7, O> actual = Infrastructure
                .decorate(nonNull(combinator, "combinator"));
        return combine(actual);
    }

    @SuppressWarnings("unchecked")
    private <O> Uni<O> combine(Functions.Function7<T1, T2, T3, T4, T5, T6, T7, O> combinator) {
        Function<List<?>, O> function = list -> {
            Tuples.ensureArity(list, 7);
            T1 item1 = (T1) list.get(0);
            T2 item2 = (T2) list.get(1);
            T3 item3 = (T3) list.get(2);
            T4 item4 = (T4) list.get(3);
            T5 item5 = (T5) list.get(4);
            T6 item6 = (T6) list.get(5);
            T7 item7 = (T7) list.get(6);

            return combinator.apply(item1, item2, item3, item4, item5, item6, item7);
        };
        return super.combinedWith(function);
    }

}
