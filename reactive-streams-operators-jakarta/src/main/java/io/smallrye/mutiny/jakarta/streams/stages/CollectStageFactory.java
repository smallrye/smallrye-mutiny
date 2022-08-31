package io.smallrye.mutiny.jakarta.streams.stages;

import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collector;

import org.eclipse.microprofile.reactive.streams.operators.spi.Stage;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.jakarta.streams.Engine;
import io.smallrye.mutiny.jakarta.streams.operators.TerminalStage;
import io.smallrye.mutiny.jakarta.streams.operators.TerminalStageFactory;

/**
 * Implement the {@link Stage.Collect} stage. It accumulates the result in a {@link Collector} and
 * redeems the last result.
 *
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class CollectStageFactory implements TerminalStageFactory<Stage.Collect> {

    @SuppressWarnings("unchecked")
    @Override
    public <I, O> TerminalStage<I, O> create(Engine engine, Stage.Collect stage) {
        Collector<I, Object, O> collector = (Collector<I, Object, O>) Objects.requireNonNull(stage).getCollector();
        Objects.requireNonNull(collector);
        return new CollectStage<>(collector);
    }

    private static class CollectStage<I, O> implements TerminalStage<I, O> {

        private final Collector<I, Object, O> collector;

        CollectStage(Collector<I, Object, O> collector) {
            this.collector = collector;
        }

        @Override
        public CompletionStage<O> apply(Multi<I> source) {
            return source.collect().with(collector).subscribeAsCompletionStage();
        }
    }

}
