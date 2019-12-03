package io.smallrye.mutiny.streams.operators;

import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import io.smallrye.mutiny.Multi;

/**
 * Defines a terminal stage - so a stream subscription and observation.
 *
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
@FunctionalInterface
public interface TerminalStage<I, O> extends Function<Multi<I>, CompletionStage<O>> {

    /**
     * Creates the {@link CompletionStage} called when the embedded logic has completed or failed.
     *
     * @param multi the observed / subscribed stream
     * @return the asynchronous result
     */
    CompletionStage<O> apply(Multi<I> multi);

}
