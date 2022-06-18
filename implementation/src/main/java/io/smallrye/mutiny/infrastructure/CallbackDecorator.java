package io.smallrye.mutiny.infrastructure;

import java.util.concurrent.Callable;
import java.util.function.*;

import io.smallrye.mutiny.tuples.Functions;

/**
 * Intercept user callbacks.
 * Decorators are called when the user passes a callback to Mutiny and so decorators can modify the passed callback.
 * <p>
 * The default behavior is to return the user's callback, unchanged.
 * <p>
 * Decorators must not transform a user callback into {@code null}.
 */
public interface CallbackDecorator extends MutinyInterceptor {

    /**
     * Allows decorating a {@link Supplier}.
     *
     * @param supplier the supplier
     * @param <T> the produced type
     * @return the decorated supplier.
     */
    default <T> Supplier<T> decorate(Supplier<T> supplier) {
        return supplier;
    }

    /**
     * Allows decorating a {@link Consumer}.
     *
     * @param consumer the consumer
     * @param <T> the consumed type
     * @return the decorated consumer.
     */
    default <T> Consumer<T> decorate(Consumer<T> consumer) {
        return consumer;
    }

    /**
     * Allows decorating a {@link LongConsumer}.
     *
     * @param consumer the consumer
     * @return the decorated consumer.
     */
    default LongConsumer decorate(LongConsumer consumer) {
        return consumer;
    }

    /**
     * Allows decorating a {@link Runnable}.
     *
     * @param runnable the runnable
     * @return the decorated runnable
     */
    default Runnable decorate(Runnable runnable) {
        return runnable;
    }

    /**
     * Allows decorating a {@link Callable}.
     *
     * @param callable the callable
     * @return the decorated callable
     * @param <V> the callable return type
     */
    default <V> Callable<V> decorate(Callable<V> callable) {
        return callable;
    }

    /**
     * Allows decorating a {@link BiConsumer}.
     *
     * @param consumer the consumer
     * @param <T1> the type of the first parameter
     * @param <T2> the type of the second parameter
     * @return the decorated bi-consumer
     */
    default <T1, T2> BiConsumer<T1, T2> decorate(BiConsumer<T1, T2> consumer) {
        return consumer;
    }

    /**
     * Allows decorating a {@link Function}.
     *
     * @param function the function
     * @param <I> the input type
     * @param <O> the output type
     * @return the decorated function
     */
    default <I, O> Function<I, O> decorate(Function<I, O> function) {
        return function;
    }

    /**
     * Allows decorating a {@link io.smallrye.mutiny.tuples.Functions.Function3}.
     *
     * @param function the function
     * @param <I1> the type of the 1st parameter
     * @param <I2> the type of the 2nd parameter
     * @param <I3> the type of the 3rd parameter
     * @param <O> the output type
     * @return the decorated function
     */
    default <I1, I2, I3, O> Functions.Function3<I1, I2, I3, O> decorate(Functions.Function3<I1, I2, I3, O> function) {
        return function;
    }

    /**
     * Allows decorating a {@link io.smallrye.mutiny.tuples.Functions.Function4}.
     *
     * @param function the function
     * @param <I1> the type of the 1st parameter
     * @param <I2> the type of the 2nd parameter
     * @param <I3> the type of the 3rd parameter
     * @param <I4> the type of the 4th parameter
     * @param <O> the output type
     * @return the decorated function
     */
    default <I1, I2, I3, I4, O> Functions.Function4<I1, I2, I3, I4, O> decorate(
            Functions.Function4<I1, I2, I3, I4, O> function) {
        return function;
    }

    /**
     * Allows decorating a {@link io.smallrye.mutiny.tuples.Functions.Function5}.
     *
     * @param function the function
     * @param <I1> the type of the 1st parameter
     * @param <I2> the type of the 2nd parameter
     * @param <I3> the type of the 3rd parameter
     * @param <I4> the type of the 4th parameter
     * @param <I5> the type of the 5th parameter
     * @param <O> the output type
     * @return the decorated function
     */
    default <I1, I2, I3, I4, I5, O> Functions.Function5<I1, I2, I3, I4, I5, O> decorate(
            Functions.Function5<I1, I2, I3, I4, I5, O> function) {
        return function;
    }

    /**
     * Allows decorating a {@link io.smallrye.mutiny.tuples.Functions.Function6}.
     *
     * @param function the function
     * @param <I1> the type of the 1st parameter
     * @param <I2> the type of the 2nd parameter
     * @param <I3> the type of the 3rd parameter
     * @param <I4> the type of the 4th parameter
     * @param <I5> the type of the 5th parameter
     * @param <I6> the type of the 6th parameter
     * @param <O> the output type
     * @return the decorated function
     */
    default <I1, I2, I3, I4, I5, I6, O> Functions.Function6<I1, I2, I3, I4, I5, I6, O> decorate(
            Functions.Function6<I1, I2, I3, I4, I5, I6, O> function) {
        return function;
    }

    /**
     * Allows decorating a {@link io.smallrye.mutiny.tuples.Functions.Function7}.
     *
     * @param function the function
     * @param <I1> the type of the 1st parameter
     * @param <I2> the type of the 2nd parameter
     * @param <I3> the type of the 3rd parameter
     * @param <I4> the type of the 4th parameter
     * @param <I5> the type of the 5th parameter
     * @param <I6> the type of the 6th parameter
     * @param <I7> the type of the 7th parameter
     * @param <O> the output type
     * @return the decorated function
     */
    default <I1, I2, I3, I4, I5, I6, I7, O> Functions.Function7<I1, I2, I3, I4, I5, I6, I7, O> decorate(
            Functions.Function7<I1, I2, I3, I4, I5, I6, I7, O> function) {
        return function;
    }

    /**
     * Allows decorating a {@link io.smallrye.mutiny.tuples.Functions.Function8}.
     *
     * @param function the function
     * @param <I1> the type of the 1st parameter
     * @param <I2> the type of the 2nd parameter
     * @param <I3> the type of the 3rd parameter
     * @param <I4> the type of the 4th parameter
     * @param <I5> the type of the 5th parameter
     * @param <I6> the type of the 6th parameter
     * @param <I7> the type of the 7th parameter
     * @param <I8> the type of the 8th parameter
     * @param <O> the output type
     * @return the decorated function
     */
    default <I1, I2, I3, I4, I5, I6, I7, I8, O> Functions.Function8<I1, I2, I3, I4, I5, I6, I7, I8, O> decorate(
            Functions.Function8<I1, I2, I3, I4, I5, I6, I7, I8, O> function) {
        return function;
    }

    /**
     * Allows decorating a {@link io.smallrye.mutiny.tuples.Functions.Function9}.
     *
     * @param function the function
     * @param <I1> the type of the 1st parameter
     * @param <I2> the type of the 2nd parameter
     * @param <I3> the type of the 3rd parameter
     * @param <I4> the type of the 4th parameter
     * @param <I5> the type of the 5th parameter
     * @param <I6> the type of the 6th parameter
     * @param <I7> the type of the 7th parameter
     * @param <I8> the type of the 8th parameter
     * @param <I9> the type of the 9th parameter
     * @param <O> the output type
     * @return the decorated function
     */
    default <I1, I2, I3, I4, I5, I6, I7, I8, I9, O> Functions.Function9<I1, I2, I3, I4, I5, I6, I7, I8, I9, O> decorate(
            Functions.Function9<I1, I2, I3, I4, I5, I6, I7, I8, I9, O> function) {
        return function;
    }

    /**
     * Allows decorating a {@link BiFunction}.
     *
     * @param function the function
     * @param <I1> the type of the 1st parameter
     * @param <I2> the type of the 2nd parameter
     * @param <O> the output type
     * @return the decorated function
     */
    default <I1, I2, O> BiFunction<I1, I2, O> decorate(BiFunction<I1, I2, O> function) {
        return function;
    }

    /**
     * Allows decorating a {@link BinaryOperator}.
     *
     * @param operator the operator
     * @param <T> the type of the parameters
     * @return the decorated binary operator
     */
    default <T> BinaryOperator<T> decorate(BinaryOperator<T> operator) {
        return operator;
    }

    /**
     * Allows decorating a {@link io.smallrye.mutiny.tuples.Functions.TriConsumer}.
     *
     * @param consumer the consumer
     * @param <T1> the type of the 1st parameter
     * @param <T2> the type of the 2nd parameter
     * @param <T3> the type of the 3rd parameter
     * @return the decorated consumer
     */
    default <T1, T2, T3> Functions.TriConsumer<T1, T2, T3> decorate(Functions.TriConsumer<T1, T2, T3> consumer) {
        return consumer;
    }

    /**
     * Allows decorating a {@link BooleanSupplier}.
     *
     * @param supplier the supplier
     * @return the decorated boolean supplier
     */
    default BooleanSupplier decorate(BooleanSupplier supplier) {
        return supplier;
    }

    /**
     * Allows decorating a {@link Predicate}.
     *
     * @param predicate the predicate
     * @param <T> the type tested by the predicate
     * @return the decorated predicate
     */
    default <T> Predicate<T> decorate(Predicate<T> predicate) {
        return predicate;
    }
}
