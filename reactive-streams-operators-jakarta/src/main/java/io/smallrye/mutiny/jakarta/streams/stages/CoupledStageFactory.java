package io.smallrye.mutiny.jakarta.streams.stages;

import java.util.Objects;

import org.eclipse.microprofile.reactive.streams.operators.spi.Graph;
import org.eclipse.microprofile.reactive.streams.operators.spi.Stage;
import org.eclipse.microprofile.reactive.streams.operators.spi.SubscriberWithCompletionStage;
import org.reactivestreams.Publisher;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.jakarta.streams.Engine;
import io.smallrye.mutiny.jakarta.streams.operators.ProcessingStage;
import io.smallrye.mutiny.jakarta.streams.operators.ProcessingStageFactory;
import io.smallrye.mutiny.jakarta.streams.utils.CouplingProcessor;
import mutiny.zero.flow.adapters.AdaptersToFlow;
import mutiny.zero.flow.adapters.AdaptersToReactiveStreams;

/**
 * Implementation of the {@link Stage.Coupled} stage.
 *
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class CoupledStageFactory implements ProcessingStageFactory<Stage.Coupled> {
    @Override
    public <I, O> ProcessingStage<I, O> create(Engine engine, Stage.Coupled stage) {
        Graph source = Objects.requireNonNull(stage.getPublisher());
        Graph sink = Objects.requireNonNull(stage.getSubscriber());

        Publisher<O> publisher = engine.buildPublisher(source);
        SubscriberWithCompletionStage<I, ?> subscriber = engine.buildSubscriber(sink);

        return upstream -> Multi.createFrom().publisher(AdaptersToFlow.publisher(
                new CouplingProcessor<>(AdaptersToReactiveStreams.publisher(upstream), subscriber.getSubscriber(), publisher)));
    }
}
