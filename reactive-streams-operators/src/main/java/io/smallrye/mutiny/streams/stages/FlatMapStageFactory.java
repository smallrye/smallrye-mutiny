package io.smallrye.mutiny.streams.stages;

import java.util.Objects;
import java.util.function.Function;

import org.eclipse.microprofile.reactive.streams.operators.spi.Graph;
import org.eclipse.microprofile.reactive.streams.operators.spi.Stage;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.streams.Engine;
import io.smallrye.mutiny.streams.operators.ProcessingStage;
import io.smallrye.mutiny.streams.operators.ProcessingStageFactory;
import io.smallrye.mutiny.streams.utils.Casts;
import mutiny.zero.flow.adapters.AdaptersToFlow;

/**
 * Implementation of the {@link Stage.FlatMap} stage. Be aware it behaves as a RX `concatMap`.
 *
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class FlatMapStageFactory implements ProcessingStageFactory<Stage.FlatMap> {

    @Override
    public <I, O> ProcessingStage<I, O> create(Engine engine, Stage.FlatMap stage) {
        Function<I, Graph> mapper = Casts.cast(stage.getMapper());
        return new FlatMapStage<>(engine, mapper);
    }

    private static class FlatMapStage<I, O> implements ProcessingStage<I, O> {
        private final Engine engine;
        private final Function<I, Graph> mapper;

        private FlatMapStage(Engine engine, Function<I, Graph> mapper) {
            this.mapper = Objects.requireNonNull(mapper);
            this.engine = engine;
        }

        @Override
        public Multi<O> apply(Multi<I> source) {
            return source
                    .onItem().transformToMultiAndConcatenate(item -> {
                        Graph graph = mapper.apply(item);
                        return AdaptersToFlow.publisher(engine.buildPublisher(Objects.requireNonNull(graph)));
                    });
        }
    }
}
