package io.smallrye.mutiny.unchecked;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Represents an operation that accepts a single input argument and returns no
 * result.
 * <p>
 * The operation can throw {@link Exception Exceptions}.
 *
 * @param <T> the type of the input to the operation
 */
@FunctionalInterface
public interface UncheckedConsumer<T> {

    /**
     * Creates a new {@link UncheckedConsumer} from an existing {@link Consumer}
     *
     * @param consumer the consumer
     * @param <T> the type of the input to the operation
     * @return the created {@code UncheckedConsumer}
     */
    static <T> UncheckedConsumer<T> from(Consumer<T> consumer) {
        return consumer::accept;
    }

    /**
     * Performs this operation on the given argument.
     *
     * @param t the input argument
     * @throws Exception if anything wrong happen
     */
    void accept(T t) throws Exception;

    /**
     * Returns a composed {@code UncheckedConsumer} that performs, in sequence, this
     * operation followed by the {@code after} operation. If performing either
     * operation throws an exception, it is relayed to the caller of the
     * composed operation. If performing this operation throws an exception,
     * the {@code after} operation will not be performed.
     *
     * @param after the operation to perform after this operation
     * @return a composed {@code UncheckedConsumer} that performs in sequence this
     *         operation followed by the {@code after} operation
     * @throws NullPointerException if {@code after} is null
     */
    default UncheckedConsumer<T> andThen(UncheckedConsumer<? super T> after) {
        Objects.requireNonNull(after);
        return (T t) -> {
            accept(t);
            after.accept(t);
        };
    }

    /**
     * @return the {@link Consumer} executing the operation associated to this {@link UncheckedConsumer}. If the
     *         operation throws an exception, the exception is rethrown, wrapped in a {@link RuntimeException} if needed.
     */
    default Consumer<T> toConsumer() {
        return x -> {
            try {
                accept(x);
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }
}
