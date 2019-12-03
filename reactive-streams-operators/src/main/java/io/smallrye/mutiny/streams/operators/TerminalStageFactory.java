package io.smallrye.mutiny.streams.operators;

import org.eclipse.microprofile.reactive.streams.operators.spi.Stage;

import io.smallrye.mutiny.streams.Engine;

/**
 * Factory to create {@link TerminalStage} instances.
 *
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
@FunctionalInterface
public interface TerminalStageFactory<T extends Stage> {

    /**
     * Creates the instance.
     *
     * @param engine the reactive engine, must not be {@code null}
     * @param stage the stage, must not be {@code null}
     * @param <I> incoming data
     * @param <O> computed result
     * @return the terminal stage, must not be {@code null}
     */
    <I, O> TerminalStage<I, O> create(Engine engine, T stage);

}
