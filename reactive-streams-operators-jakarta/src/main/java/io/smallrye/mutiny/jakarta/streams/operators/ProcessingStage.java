package io.smallrye.mutiny.jakarta.streams.operators;

import java.util.function.Function;

import io.smallrye.mutiny.Multi;

/**
 * Defines a processing stage - so a stream transformation.
 *
 * @param <I> type of the received items
 * @param <O> type of the emitted items
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
@FunctionalInterface
public interface ProcessingStage<I, O> extends Function<Multi<I>, Multi<O>> {

    /**
     * Adapts the streams.
     *
     * @param source the input stream, must not be {@code null}
     * @return the adapted stream, must not be {@code null}
     */
    Multi<O> apply(Multi<I> source);

}
