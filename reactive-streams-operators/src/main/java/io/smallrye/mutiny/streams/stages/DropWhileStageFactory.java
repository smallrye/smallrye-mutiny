package io.smallrye.mutiny.streams.stages;

import java.util.Objects;
import java.util.function.Predicate;

import org.eclipse.microprofile.reactive.streams.operators.spi.Stage;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.streams.Engine;
import io.smallrye.mutiny.streams.operators.ProcessingStage;
import io.smallrye.mutiny.streams.operators.ProcessingStageFactory;
import io.smallrye.mutiny.streams.utils.Casts;

/**
 * Implementation of the {@link Stage.DropWhile} stage.
 *
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class DropWhileStageFactory implements ProcessingStageFactory<Stage.DropWhile> {

    @Override
    public <I, O> ProcessingStage<I, O> create(Engine engine, Stage.DropWhile stage) {
        Predicate<I> predicate = Casts.cast(stage.getPredicate());
        return Casts.cast(new TakeWhile<>(predicate));
    }

    private static class TakeWhile<I> implements ProcessingStage<I, I> {
        private final Predicate<I> predicate;

        TakeWhile(Predicate<I> predicate) {
            this.predicate = Objects.requireNonNull(predicate);
        }

        @Override
        public Multi<I> apply(Multi<I> source) {
            return source.skip().first(predicate);
        }
    }
}
