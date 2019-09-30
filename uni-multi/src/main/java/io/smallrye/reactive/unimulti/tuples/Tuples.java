package io.smallrye.reactive.unimulti.tuples;

import io.smallrye.reactive.unimulti.helpers.ParameterValidation;

import java.util.List;

/**
 * A set of methods to create {@link Tuple} instances from lists.
 */
@SuppressWarnings({ "rawtypes" })
public abstract class Tuples {

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

    public static void ensureArity(List<?> parameters, int expectedSize) {
        if (parameters.size() != expectedSize) {
            throw new IllegalArgumentException("Cannot call combinator from list. Expected size: " + expectedSize
                    + ", Actual size: " + parameters.size());
        }
    }
}

