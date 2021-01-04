package io.smallrye.mutiny.streams.stages;

import java.util.Objects;
import java.util.function.Predicate;

import org.eclipse.microprofile.reactive.streams.operators.spi.Stage;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.streams.Engine;
import io.smallrye.mutiny.streams.operators.ProcessingStage;
import io.smallrye.mutiny.streams.operators.ProcessingStageFactory;

/**
 * Implementation of the {@link Stage.TakeWhile} stage.
 *
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class TakeWhileStageFactory implements ProcessingStageFactory<Stage.TakeWhile> {

    @SuppressWarnings("unchecked")
    @Override
    public <I, O> ProcessingStage<I, O> create(Engine engine, Stage.TakeWhile stage) {
        Predicate<I> predicate = (Predicate<I>) Objects.requireNonNull(stage.getPredicate());
        return source -> (Multi<O>) source.select().first(predicate);
    }
}
