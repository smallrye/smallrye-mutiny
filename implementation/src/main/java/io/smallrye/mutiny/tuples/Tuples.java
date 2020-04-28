package io.smallrye.mutiny.tuples;

import java.util.List;

import io.smallrye.mutiny.helpers.ParameterValidation;

/**
 * A set of methods to create {@link Tuple} instances from lists.
 */
public abstract class Tuples {

    private Tuples() {
        // avoid direct instantiation.
    }

    @SuppressWarnings("unchecked")
    public static <L, R> Tuple2<L, R> tuple2(List<?> list) {
        if (ParameterValidation.nonNull(list, "list").size() != 2) {
            throw new IllegalArgumentException(
                    "Cannot create a tuple from the given list, size must be 2 (actual size is " + ParameterValidation
                            .nonNull(list, "list")
                            .size() + ")");
        }
        return Tuple2.of((L) list.get(0), (R) list.get(1));
    }

    @SuppressWarnings("unchecked")
    public static <T1, T2, T3> Tuple3<T1, T2, T3> tuple3(List<?> list) {
        if (ParameterValidation.nonNull(list, "list").size() != 3) {
            throw new IllegalArgumentException(
                    "Cannot create a tuple from the given list, size must be 3 (actual size is " + ParameterValidation
                            .nonNull(list, "list")
                            .size() + ")");
        }
        return Tuple3.of((T1) list.get(0), (T2) list.get(1), (T3) list.get(2));
    }

    @SuppressWarnings("unchecked")
    public static <T1, T2, T3, T4> Tuple4<T1, T2, T3, T4> tuple4(List<?> list) {
        if (ParameterValidation.nonNull(list, "list").size() != 4) {
            throw new IllegalArgumentException(
                    "Cannot create a tuple from the given list, size must be 4 (actual size is " + ParameterValidation
                            .nonNull(list, "list")
                            .size() + ")");
        }
        return Tuple4.of((T1) list.get(0), (T2) list.get(1), (T3) list.get(2), (T4) list.get(3));
    }

    @SuppressWarnings("unchecked")
    public static <T1, T2, T3, T4, T5> Tuple5<T1, T2, T3, T4, T5> tuple5(List<?> list) {
        if (ParameterValidation.nonNull(list, "list").size() != 5) {
            throw new IllegalArgumentException(
                    "Cannot create a tuple from the given list, size must be 5 (actual size is " + ParameterValidation
                            .nonNull(list, "list")
                            .size() + ")");
        }
        return Tuple5.of((T1) list.get(0), (T2) list.get(1), (T3) list.get(2), (T4) list.get(3), (T5) list.get(4));
    }

    @SuppressWarnings("unchecked")
    public static <T1, T2, T3, T4, T5, T6> Tuple6<T1, T2, T3, T4, T5, T6> tuple6(List<?> list) {
        if (ParameterValidation.nonNull(list, "list").size() != 6) {
            throw new IllegalArgumentException(
                    "Cannot create a tuple from the given list, size must be 6 (actual size is " + ParameterValidation
                            .nonNull(list, "list")
                            .size() + ")");
        }
        return Tuple6.of((T1) list.get(0), (T2) list.get(1), (T3) list.get(2), (T4) list.get(3),
                (T5) list.get(4), (T6) list.get(5));
    }

    @SuppressWarnings("unchecked")
    public static <T1, T2, T3, T4, T5, T6, T7> Tuple7<T1, T2, T3, T4, T5, T6, T7> tuple7(List<?> list) {
        if (ParameterValidation.nonNull(list, "list").size() != 7) {
            throw new IllegalArgumentException(
                    "Cannot create a tuple from the given list, size must be 7 (actual size is " + ParameterValidation
                            .nonNull(list, "list")
                            .size() + ")");
        }
        return Tuple7.of((T1) list.get(0), (T2) list.get(1), (T3) list.get(2), (T4) list.get(3),
                (T5) list.get(4), (T6) list.get(5), (T7) list.get(6));
    }

    @SuppressWarnings("unchecked")
    public static <T1, T2, T3, T4, T5, T6, T7, T8> Tuple8<T1, T2, T3, T4, T5, T6, T7, T8> tuple8(List<?> list) {
        if (ParameterValidation.nonNull(list, "list").size() != 8) {
            throw new IllegalArgumentException(
                    "Cannot create a tuple from the given list, size must be 8 (actual size is " + ParameterValidation
                            .nonNull(list, "list")
                            .size() + ")");
        }
        return Tuple8.of((T1) list.get(0), (T2) list.get(1), (T3) list.get(2), (T4) list.get(3),
                (T5) list.get(4), (T6) list.get(5), (T7) list.get(6), (T8) list.get(7));
    }

    @SuppressWarnings("unchecked")
    public static <T1, T2, T3, T4, T5, T6, T7, T8, T9> Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9> tuple9(List<?> list) {
        if (ParameterValidation.nonNull(list, "list").size() != 9) {
            throw new IllegalArgumentException(
                    "Cannot create a tuple from the given list, size must be 9 (actual size is " + ParameterValidation
                            .nonNull(list, "list")
                            .size() + ")");
        }
        return Tuple9.of((T1) list.get(0), (T2) list.get(1), (T3) list.get(2), (T4) list.get(3),
                (T5) list.get(4), (T6) list.get(5), (T7) list.get(6), (T8) list.get(7), (T9) list.get(8));
    }

    public static void ensureArity(List<?> parameters, int expectedSize) {
        if (parameters.size() != expectedSize) {
            throw new IllegalArgumentException("Cannot call combinator from list. Expected size: " + expectedSize
                    + ", Actual size: " + parameters.size());
        }
    }
}
