package io.smallrye.reactive.tuples;

import java.util.List;
import java.util.function.Function;

public class Functions {


    @FunctionalInterface
    public interface Function3<T1, T2, T3, R> extends Function<List<Object>, R> {
        R apply(T1 result1, T2 result2, T3 result3);

        @SuppressWarnings("unchecked")
        @Override
        default R apply(List<Object> objects) {
            return apply((T1) objects.get(0), (T2) objects.get(1), (T3) objects.get(2));
        }
    }

    public interface Function4<T1, T2, T3, T4, R> extends Function<List<Object>, R> {
        R apply(T1 result1, T2 result2, T3 result3, T4 result4);

        @SuppressWarnings("unchecked")
        @Override
        default R apply(List<Object> objects) {
            return apply((T1) objects.get(0), (T2) objects.get(1), (T3) objects.get(2), (T4) objects.get(3));
        }
    }

    public interface Function5<T1, T2, T3, T4, T5, R> extends Function<List<Object>, R> {
        R apply(T1 result1, T2 result2, T3 result3, T4 result4, T5 result5);

        @SuppressWarnings("unchecked")
        @Override
        default R apply(List<Object> objects) {
            return apply((T1) objects.get(0), (T2) objects.get(1),
                    (T3) objects.get(2), (T4) objects.get(3), (T5) objects.get(4));
        }
    }

}
