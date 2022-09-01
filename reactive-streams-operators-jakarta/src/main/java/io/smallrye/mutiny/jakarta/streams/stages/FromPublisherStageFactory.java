package io.smallrye.mutiny.jakarta.streams.stages;

import java.util.Objects;

import org.eclipse.microprofile.reactive.streams.operators.spi.Stage;
import org.reactivestreams.Publisher;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.jakarta.streams.Engine;
import io.smallrye.mutiny.jakarta.streams.operators.PublisherStage;
import io.smallrye.mutiny.jakarta.streams.operators.PublisherStageFactory;
import mutiny.zero.flow.adapters.AdaptersToFlow;

/**
 * Implementation of the {@link Stage.PublisherStage} stage.
 *
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class FromPublisherStageFactory implements PublisherStageFactory<Stage.PublisherStage> {

    @SuppressWarnings("unchecked")
    @Override
    public <O> PublisherStage<O> create(Engine engine, Stage.PublisherStage stage) {
        Publisher<O> publisher = (Publisher<O>) Objects.requireNonNull(Objects.requireNonNull(stage.getRsPublisher()));
        return () -> Multi.createFrom().publisher(AdaptersToFlow.publisher(publisher));
    }
}
