package io.smallrye.mutiny.jakarta.streams.stages;

import java.util.Objects;
import java.util.function.Function;

import org.eclipse.microprofile.reactive.streams.operators.spi.Stage;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.jakarta.streams.Engine;
import io.smallrye.mutiny.jakarta.streams.operators.ProcessingStage;
import io.smallrye.mutiny.jakarta.streams.operators.ProcessingStageFactory;
import io.smallrye.mutiny.jakarta.streams.utils.Casts;

/**
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class FlatMapIterableStageFactory implements ProcessingStageFactory<Stage.FlatMapIterable> {

    @Override
    public <I, O> ProcessingStage<I, O> create(Engine engine, Stage.FlatMapIterable stage) {
        Function<I, Iterable<O>> mapper = Casts.cast(stage.getMapper());
        return new FlatMapIterable<>(mapper);
    }

    private static class FlatMapIterable<I, O> implements ProcessingStage<I, O> {
        private final Function<I, Iterable<O>> mapper;

        private FlatMapIterable(Function<I, Iterable<O>> mapper) {
            this.mapper = Objects.requireNonNull(mapper);
        }

        @Override
        public Multi<O> apply(Multi<I> source) {
            return source.onItem().transformToIterable(mapper);
        }
    }

}
