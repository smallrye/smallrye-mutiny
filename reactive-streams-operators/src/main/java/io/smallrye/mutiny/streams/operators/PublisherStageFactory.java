package io.smallrye.mutiny.streams.operators;

import org.eclipse.microprofile.reactive.streams.operators.spi.Stage;

import io.smallrye.mutiny.streams.Engine;

/**
 * Factory to create {@link PublisherStage} instances.
 *
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
@FunctionalInterface
public interface PublisherStageFactory<T extends Stage> {

    /**
     * Creates the instance.
     *
     * @param engine the reactive engine
     * @param stage the stage
     * @param <O> output data
     * @return the created processing stage, should never be {@code null}
     */
    <O> PublisherStage<O> create(Engine engine, T stage);

}
