package io.smallrye.mutiny.streams.stages;

import java.util.Objects;

import org.eclipse.microprofile.reactive.streams.operators.spi.Graph;
import org.eclipse.microprofile.reactive.streams.operators.spi.Stage;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.streams.Engine;
import io.smallrye.mutiny.streams.operators.PublisherStage;
import io.smallrye.mutiny.streams.operators.PublisherStageFactory;
import io.smallrye.mutiny.streams.utils.CancellablePublisher;
import mutiny.zero.flow.adapters.AdaptersToFlow;

/**
 * Implementation of the {@link Stage.Concat} stage. Because both streams can emits on different thread,
 * this operators takes care to called the user on a Vert.x context if the caller used one, otherwise it
 * uses the current thread.
 *
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class ConcatStageFactory implements PublisherStageFactory<Stage.Concat> {

    @Override
    public <O> PublisherStage<O> create(Engine engine, Stage.Concat stage) {
        Objects.requireNonNull(engine);
        Objects.requireNonNull(stage);
        Graph g1 = stage.getFirst();
        Graph g2 = stage.getSecond();
        return new ConcatStage<>(engine, g1, g2);
    }

    private class ConcatStage<O> implements PublisherStage<O> {
        private final Engine engine;
        private final Graph first;
        private final Graph second;

        ConcatStage(Engine engine, Graph g1, Graph g2) {
            this.engine = Objects.requireNonNull(engine);
            this.first = Objects.requireNonNull(g1);
            this.second = Objects.requireNonNull(g2);
        }

        @Override
        public Multi<O> get() {
            CancellablePublisher<O> cancellable = new CancellablePublisher<>(engine.buildPublisher(second));
            return Multi.createBy().concatenating()
                    .streams(AdaptersToFlow.publisher(engine.buildPublisher(first)), AdaptersToFlow.publisher(cancellable))
                    .onTermination().invoke(cancellable::cancelIfNotSubscribed);
        }
    }
}
