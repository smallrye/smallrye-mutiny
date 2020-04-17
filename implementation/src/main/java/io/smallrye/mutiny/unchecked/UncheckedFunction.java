package io.smallrye.mutiny.unchecked;

import java.util.Objects;
import java.util.function.Function;

/**
 * Represents a function that accepts one argument and produces a result.
 * <p>
 * The operation can throw {@link Exception Exceptions}.
 *
 * @param <T> the type of the input to the function
 * @param <R> the type of the result of the function
 */
@FunctionalInterface
public interface UncheckedFunction<T, R> {

    /**
     * Creates a {@link UncheckedBiConsumer} from an existing {@link Function}
     *
     * @param function the function
     * @param <T> the type of the input to the function
     * @param <R> the type of the result of the function
     * @return the created {@link UncheckedBiConsumer}
     */
    static <T, R> UncheckedFunction<T, R> from(Function<T, R> function) {
        return function::apply;
    }

    /**
     * Applies this function to the given argument.
     *
     * @param t the function argument
     * @return the function result
     * @throws Exception if anything wrong happen
     */
    R apply(T t) throws Exception;

    /**
     * @return a {@link Function} executing this {@code UncheckedFunction}. If the operation throws an exception,
     *         the exception is rethrown, wrapped in a {@link RuntimeException} if needed.
     */
    default Function<T, R> toFunction() {
        return t -> {
            try {
                return apply(t);
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

    /**
     * Returns a composed function that first applies the {@code before}
     * function to its input, and then applies this function to the result.
     * If evaluation of either function throws an exception, it is relayed to
     * the caller of the composed function.
     *
     * @param <V> the type of input to the {@code before} function, and to the
     *        composed function
     * @param before the function to apply before this function is applied
     * @return a composed function that first applies the {@code before}
     *         function and then applies this function
     * @throws NullPointerException if before is null
     * @see #andThen(UncheckedFunction)
     */
    default <V> UncheckedFunction<V, R> compose(UncheckedFunction<? super V, ? extends T> before) {
        Objects.requireNonNull(before);
        return (V v) -> apply(before.apply(v));
    }

    /**
     * Returns a composed function that first applies this function to
     * its input, and then applies the {@code after} function to the result.
     * If evaluation of either function throws an exception, it is relayed to
     * the caller of the composed function.
     *
     * @param <V> the type of output of the {@code after} function, and of the
     *        composed function
     * @param after the function to apply after this function is applied
     * @return a composed function that first applies this function and then
     *         applies the {@code after} function
     * @throws NullPointerException if after is null
     * @see #compose(UncheckedFunction)
     */
    default <V> UncheckedFunction<T, V> andThen(UncheckedFunction<? super R, ? extends V> after) {
        Objects.requireNonNull(after);
        return (T t) -> after.apply(apply(t));
    }
}
