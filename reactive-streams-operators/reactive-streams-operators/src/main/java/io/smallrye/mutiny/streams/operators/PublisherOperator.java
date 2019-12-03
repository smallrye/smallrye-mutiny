package io.smallrye.mutiny.streams.operators;

import org.eclipse.microprofile.reactive.streams.operators.spi.Stage;

import io.smallrye.mutiny.streams.Engine;

public class PublisherOperator<T extends Stage> extends Operator<T> {

    private PublisherStageFactory<T> factory;

    public PublisherOperator(Class<T> clazz, PublisherStageFactory<T> factory) {
        super(clazz);
        this.factory = factory;
    }

    public PublisherStage create(Engine engine, T stage) {
        return factory.create(engine, stage);
    }
}
