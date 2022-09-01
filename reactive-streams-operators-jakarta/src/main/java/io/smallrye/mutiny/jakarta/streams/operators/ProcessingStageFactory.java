package io.smallrye.mutiny.jakarta.streams.operators;

import org.eclipse.microprofile.reactive.streams.operators.spi.Stage;

import io.smallrye.mutiny.jakarta.streams.Engine;

/**
 * Factory to create {@link ProcessingStage} instances.
 *
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
@FunctionalInterface
public interface ProcessingStageFactory<T extends Stage> {

    /**
     * Creates the instance.
     *
     * @param engine the reactive engine
     * @param stage the stage
     * @param <I> input data
     * @param <O> output data
     * @return the created processing stage, should never be {@code null}
     */
    <I, O> ProcessingStage<I, O> create(Engine engine, T stage);

}
