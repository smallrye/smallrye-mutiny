package io.smallrye.mutiny.unchecked;

import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * Represents an operation that accepts two input arguments and returns no
 * result. This is the two-arity specialization of {@link UncheckedConsumer}.
 * <p>
 * The operation can throw {@link Exception Exceptions}.
 *
 * @param <T> the type of the first argument to the operation
 * @param <U> the type of the second argument to the operation
 */
@FunctionalInterface
public interface UncheckedBiConsumer<T, U> {

    /**
     * Creates a {@link UncheckedBiConsumer} from an existing {@link BiConsumer}
     *
     * @param consumer the consumer
     * @param <T> the type of the first argument to the operation
     * @param <U> the type of the second argument to the operation
     * @return the created {@link UncheckedBiConsumer}
     */
    static <T, U> UncheckedBiConsumer<T, U> from(BiConsumer<T, U> consumer) {
        return consumer::accept;
    }

    /**
     * Performs this operation on the given arguments.
     *
     * @param t the first input argument
     * @param u the second input argument
     * @throws Exception if something <em>bad</em> happen during the execution
     */
    void accept(T t, U u) throws Exception;

    /**
     * Returns a composed {@code UncheckedBiConsumer} that performs, in sequence, this
     * operation followed by the {@code after} operation. If performing either
     * operation throws an exception, it is relayed to the caller of the
     * composed operation. If performing this operation throws an exception,
     * the {@code after} operation will not be performed.
     *
     * @param after the operation to perform after this operation
     * @return a composed {@code UncheckedBiConsumer} that performs in sequence this
     *         operation followed by the {@code after} operation
     * @throws NullPointerException if {@code after} is null
     */
    default UncheckedBiConsumer<T, U> andThen(UncheckedBiConsumer<? super T, ? super U> after) {
        Objects.requireNonNull(after);

        return (l, r) -> {
            accept(l, r);
            after.accept(l, r);
        };
    }

    /**
     * @return the {@link BiConsumer} associated with this {@code UncheckedBiConsumer}. If the operation throws an
     *         exception, the exception is rethrown, wrapped in a {@link RuntimeException} if needed.
     */
    default BiConsumer<T, U> toBiConsumer() {
        return (x, y) -> {
            try {
                accept(x, y);
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }
}
