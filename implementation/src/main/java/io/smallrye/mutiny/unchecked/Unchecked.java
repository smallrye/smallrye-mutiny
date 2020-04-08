package io.smallrye.mutiny.unchecked;

import java.util.function.*;

/**
 * Provides wrapper to handle functions / consumers / suppliers that throw checked exceptions.
 */
public class Unchecked {

    private Unchecked() {
        // avoid direct instantiation.
    }

    /**
     * Transforms the given bi-function into a version that can throw exceptions.
     *
     * @param function the function
     * @param <T> the type of the first argument to the function
     * @param <U> the type of the second argument to the function
     * @param <R> the type of the result of the function
     * @return the new {@link UncheckedBiFunction}
     */
    public static <T, U, R> UncheckedBiFunction<T, U, R> unchecked(BiFunction<T, U, R> function) {
        return UncheckedBiFunction.from(function);
    }

    /**
     * Transforms the given function into a version that can throw exceptions.
     *
     * @param function the function
     * @param <T> the type of the argument to the function
     * @param <R> the type of the result of the function
     * @return the new {@link UncheckedFunction}
     */
    public static <T, R> UncheckedFunction<T, R> unchecked(Function<T, R> function) {
        return UncheckedFunction.from(function);
    }

    /**
     * Transforms the given consumer into a version that can throw exceptions.
     *
     * @param consumer the consumer
     * @param <T> the type of the input to the operation
     * @return the new {@link UncheckedConsumer}
     */
    public static <T> UncheckedConsumer<T> unchecked(Consumer<T> consumer) {
        return UncheckedConsumer.from(consumer);
    }

    /**
     * Transforms the given bi-consumer into a version that can throw exceptions.
     *
     * @param consumer the consumer
     * @param <T> the type of the first argument to the operation
     * @param <U> the type of the second argument to the operation
     * @return the new {@link UncheckedBiConsumer}
     */
    public static <T, U> UncheckedBiConsumer<T, U> unchecked(BiConsumer<T, U> consumer) {
        return UncheckedBiConsumer.from(consumer);
    }

    /**
     * Transforms the given supplier into a version that can throw exceptions.
     *
     * @param supplier the supplier
     * @param <T> the type of items supplied by this supplier
     * @return the new {@link UncheckedSupplier}
     */
    public static <T> UncheckedSupplier<T> unchecked(Supplier<T> supplier) {
        return UncheckedSupplier.from(supplier);
    }

    /**
     * Transforms the given (unchecked) function into a regular function.
     * If the operation throws an exception, this exception is rethrown, wrapped into a {@link RuntimeException} if
     * needed.
     *
     * @param function the function
     * @param <T> the type of the argument to the function
     * @param <R> the type of the result of the function
     * @return a {@link Function} executing the {@link UncheckedFunction}. If the operation throws an exception,
     *         the exception is rethrown, wrapped in a {@link RuntimeException} if needed.
     */
    public static <T, R> Function<T, R> function(UncheckedFunction<T, R> function) {
        return function.toFunction();
    }

    /**
     * Transforms the given (unchecked) bi-function into a regular bi-function.
     * If the operation throws an exception, this exception is rethrown, wrapped into a {@link RuntimeException} if
     * needed.
     *
     * @param function the function
     * @param <T> the type of the first argument to the function
     * @param <U> the type of the second argument to the function
     * @param <R> the type of the result of the function
     * @return a {@link BiFunction} executing this {@link UncheckedBiFunction}. If the operation throws an exception,
     *         the exception is rethrown, wrapped in a {@link RuntimeException} if needed.
     */
    public static <T, U, R> BiFunction<T, U, R> function(UncheckedBiFunction<T, U, R> function) {
        return function.toBiFunction();
    }

    /**
     * Transforms the given (unchecked) bi-consumer into a regular bi-consumer.
     * If the operation throws an exception, this exception is rethrown, wrapped into a {@link RuntimeException} if
     * needed.
     *
     * @param consumer the consumer
     * @param <T> the type of the first argument to the operation
     * @param <U> the type of the second argument to the operation
     * @return a {@link BiConsumer} executing this {@link UncheckedBiConsumer}. If the operation throws an exception,
     *         the exception is rethrown, wrapped in a {@link RuntimeException} if needed.
     */
    public static <T, U> BiConsumer<T, U> consumer(UncheckedBiConsumer<T, U> consumer) {
        return consumer.toBiConsumer();
    }

    /**
     * Transforms the given (unchecked) consumer into a regular consumer.
     * If the operation throws an exception, this exception is rethrown, wrapped into a {@link RuntimeException} if
     * needed.
     *
     * @param consumer the consumer
     * @param <T> the type of the first argument to the operation
     * @return a {@link Consumer} executing this {@link UncheckedConsumer}. If the operation throws an exception,
     *         the exception is rethrown, wrapped in a {@link RuntimeException} if needed.
     */
    public static <T> Consumer<T> consumer(UncheckedConsumer<T> consumer) {
        return consumer.toConsumer();
    }

    /**
     * Transforms the given (unchecked) supplier into a regular supplier.
     * If the operation throws an exception, this exception is rethrown, wrapped into a {@link RuntimeException} if
     * needed.
     *
     * @param supplier the supplier
     * @param <T> the type of items supplied by this supplier
     * @return a {@link Supplier} executing this {@link UncheckedSupplier}. If the operation throws an exception,
     *         the exception is rethrown, wrapped in a {@link RuntimeException} if needed.
     */
    public static <T> Supplier<T> supplier(UncheckedSupplier<T> supplier) {
        return supplier.toSupplier();
    }

}
