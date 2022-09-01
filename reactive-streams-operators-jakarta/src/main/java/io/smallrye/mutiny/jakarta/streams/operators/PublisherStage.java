package io.smallrye.mutiny.jakarta.streams.operators;

import java.util.function.Supplier;

import io.smallrye.mutiny.Multi;

/**
 * Specialization of the {@link ProcessingStage} for data sources (publishers).
 *
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public interface PublisherStage<O> extends Supplier<Multi<O>> {

    /**
     * @return the publisher.
     */
    Multi<O> get();
}
