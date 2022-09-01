package io.smallrye.mutiny.jakarta.streams.stages;

import java.util.Objects;

import org.eclipse.microprofile.reactive.streams.operators.spi.Stage;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.jakarta.streams.Engine;
import io.smallrye.mutiny.jakarta.streams.operators.PublisherStage;
import io.smallrye.mutiny.jakarta.streams.operators.PublisherStageFactory;

/**
 * Implementation of the {@link Stage.Failed} stage.
 *
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class FailedPublisherStageFactory implements PublisherStageFactory<Stage.Failed> {

    @Override
    public <O> PublisherStage<O> create(Engine engine, Stage.Failed stage) {
        Throwable error = Objects.requireNonNull(Objects.requireNonNull(stage).getError());
        return () -> Multi.createFrom().failure(error);
    }
}
