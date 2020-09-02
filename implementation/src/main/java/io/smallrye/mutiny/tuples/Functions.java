package io.smallrye.mutiny.tuples;

import java.util.List;
import java.util.function.Function;

public final class Functions {

    private Functions() {
        // Avoid direct instantiation.
    }

    @FunctionalInterface
    public interface Function3<T1, T2, T3, R> extends Function<List<Object>, R> {
        R apply(T1 item1, T2 item2, T3 item3);

        @SuppressWarnings("unchecked")
        @Override
        default R apply(List<Object> objects) {
            return apply((T1) objects.get(0), (T2) objects.get(1), (T3) objects.get(2));
        }
    }

    @FunctionalInterface
    public interface Function4<T1, T2, T3, T4, R> extends Function<List<Object>, R> {
        R apply(T1 item1, T2 item2, T3 item3, T4 item4);

        @SuppressWarnings("unchecked")
        @Override
        default R apply(List<Object> objects) {
            return apply((T1) objects.get(0), (T2) objects.get(1), (T3) objects.get(2), (T4) objects.get(3));
        }
    }

    @FunctionalInterface
    public interface Function5<T1, T2, T3, T4, T5, R> extends Function<List<Object>, R> {
        R apply(T1 item1, T2 item2, T3 item3, T4 item4, T5 item5);

        @SuppressWarnings("unchecked")
        @Override
        default R apply(List<Object> objects) {
            return apply((T1) objects.get(0), (T2) objects.get(1),
                    (T3) objects.get(2), (T4) objects.get(3), (T5) objects.get(4));
        }
    }

    @FunctionalInterface
    public interface Function6<T1, T2, T3, T4, T5, T6, R> extends Function<List<Object>, R> {
        R apply(T1 item1, T2 item2, T3 item3, T4 item4, T5 item5, T6 item6);

        @SuppressWarnings("unchecked")
        @Override
        default R apply(List<Object> objects) {
            return apply((T1) objects.get(0), (T2) objects.get(1),
                    (T3) objects.get(2), (T4) objects.get(3), (T5) objects.get(4), (T6) objects.get(5));
        }
    }

    @FunctionalInterface
    public interface Function7<T1, T2, T3, T4, T5, T6, T7, R> extends Function<List<Object>, R> {
        R apply(T1 item1, T2 item2, T3 item3, T4 item4, T5 item5, T6 item6, T7 item7);

        @SuppressWarnings("unchecked")
        @Override
        default R apply(List<Object> objects) {
            return apply((T1) objects.get(0), (T2) objects.get(1),
                    (T3) objects.get(2), (T4) objects.get(3), (T5) objects.get(4), (T6) objects.get(5), (T7) objects.get(6));
        }
    }

    @FunctionalInterface
    public interface Function8<T1, T2, T3, T4, T5, T6, T7, T8, R> extends Function<List<Object>, R> {
        R apply(T1 item1, T2 item2, T3 item3, T4 item4, T5 item5, T6 item6, T7 item7, T8 item8); // NOSONAR

        @SuppressWarnings("unchecked")
        @Override
        default R apply(List<Object> objects) {
            return apply((T1) objects.get(0), (T2) objects.get(1),
                    (T3) objects.get(2), (T4) objects.get(3), (T5) objects.get(4), (T6) objects.get(5),
                    (T7) objects.get(6), (T8) objects.get(7));
        }
    }

    @FunctionalInterface
    public interface Function9<T1, T2, T3, T4, T5, T6, T7, T8, T9, R> extends Function<List<Object>, R> {
        R apply(T1 item1, T2 item2, T3 item3, T4 item4, T5 item5, T6 item6, T7 item7, T8 item8, T9 item9); // NOSONAR

        @SuppressWarnings("unchecked")
        @Override
        default R apply(List<Object> objects) {
            return apply((T1) objects.get(0), (T2) objects.get(1),
                    (T3) objects.get(2), (T4) objects.get(3), (T5) objects.get(4), (T6) objects.get(5),
                    (T7) objects.get(6), (T8) objects.get(7), (T9) objects.get(8));
        }
    }

    /**
     * A consumer taking 3 parameters.
     *
     * @param <A> the type of the first parameter
     * @param <B> the type of the second parameter
     * @param <C> the type of the third parameter
     */
    @FunctionalInterface
    public interface TriConsumer<A, B, C> {
        void accept(A a, B b, C c);
    }

}
