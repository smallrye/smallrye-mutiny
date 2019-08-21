package io.smallrye.reactive.tuples;

import java.util.List;
import java.util.function.Function;

public class Functions {


    @FunctionalInterface
    public interface Function3<T1, T2, T3, R> extends Function<List<Object>, R> {
        R apply(T1 item1, T2 item2, T3 item3);

        @SuppressWarnings("unchecked")
        @Override
        default R apply(List<Object> objects) {
            return apply((T1) objects.get(0), (T2) objects.get(1), (T3) objects.get(2));
        }
    }

    public interface Function4<T1, T2, T3, T4, R> extends Function<List<Object>, R> {
        R apply(T1 item1, T2 item2, T3 item3, T4 item4);

        @SuppressWarnings("unchecked")
        @Override
        default R apply(List<Object> objects) {
            return apply((T1) objects.get(0), (T2) objects.get(1), (T3) objects.get(2), (T4) objects.get(3));
        }
    }

    public interface Function5<T1, T2, T3, T4, T5, R> extends Function<List<Object>, R> {
        R apply(T1 item1, T2 item2, T3 item3, T4 item4, T5 item5);

        @SuppressWarnings("unchecked")
        @Override
        default R apply(List<Object> objects) {
            return apply((T1) objects.get(0), (T2) objects.get(1),
                    (T3) objects.get(2), (T4) objects.get(3), (T5) objects.get(4));
        }
    }

    /**
     * A consumer taking 3 parameters.
     *
     * @param <A> the type of the first parameter
     * @param <B> the type of the second parameter
     * @param <C> the type of the third parameter
     */
    public interface TriConsumer<A, B, C> {
        void accept(A a, B b, C c);
    }

}
