package io.smallrye.mutiny.unchecked;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Represents a function that accepts two arguments and produces a result.
 * This is the two-arity specialization of {@link UncheckedFunction}.
 * <p>
 * The operation can throw {@link Exception Exceptions}.
 *
 * @param <T> the type of the first argument to the function
 * @param <U> the type of the second argument to the function
 * @param <R> the type of the result of the function
 */
@FunctionalInterface
public interface UncheckedBiFunction<T, U, R> {

    /**
     * Creates a {@link UncheckedBiFunction} from an existing {@link BiFunction}.
     *
     * @param function the function
     * @param <T> the type of the first argument to the function
     * @param <U> the type of the second argument to the function
     * @param <R> the type of the result of the function
     * @return the created {@link UncheckedBiFunction}
     */
    static <T, U, R> UncheckedBiFunction<T, U, R> from(BiFunction<T, U, R> function) {
        return function::apply;
    }

    /**
     * @return a {@link Function} executing this {@code UncheckedFunction}.If the operation throws an exception, the
     *         exception is rethrown, wrapped in a {@link RuntimeException} if needed.
     */
    default BiFunction<T, U, R> toBiFunction() {
        return (t, u) -> {
            try {
                return apply(t, u);
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

    /**
     * Applies this function to the given arguments.
     *
     * @param t the first function argument
     * @param u the second function argument
     * @return the function result
     * @throws Exception if anything wrong happen
     */
    R apply(T t, U u) throws Exception;

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
     */
    default <V> UncheckedBiFunction<T, U, V> andThen(UncheckedFunction<? super R, ? extends V> after) {
        Objects.requireNonNull(after);
        return (T t, U u) -> after.apply(apply(t, u));
    }
}
