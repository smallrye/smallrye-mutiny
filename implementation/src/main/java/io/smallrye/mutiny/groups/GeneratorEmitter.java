package io.smallrye.mutiny.groups;

import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * Interface for {@link io.smallrye.mutiny.Multi} generators, where items are being generated on subscription requests.
 * <p>
 * Each generation round shall call {@link GeneratorEmitter#emit(Object)} once, possibly followed by a terminal call to
 * {@link GeneratorEmitter#complete()}, or call {@link GeneratorEmitter#fail(Throwable)} to terminate the stream with a failure.
 *
 * @param <T> the elements type
 * @see MultiCreate#generator(Supplier, BiFunction)
 */
public interface GeneratorEmitter<T> {

    /**
     * Emit an item.
     *
     * @param item the item
     */
    void emit(T item);

    /**
     * Emit a failure and terminate the stream.
     *
     * @param failure the failure
     */
    void fail(Throwable failure);

    /**
     * Complete the stream.
     */
    void complete();
}
