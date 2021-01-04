package io.smallrye.mutiny.streams.stages;

import java.util.Objects;

import org.eclipse.microprofile.reactive.streams.operators.spi.Stage;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.streams.Engine;
import io.smallrye.mutiny.streams.operators.ProcessingStage;
import io.smallrye.mutiny.streams.operators.ProcessingStageFactory;

/**
 * Implementation of the {@link Stage.Distinct} stage.
 *
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class DistinctStageFactory implements ProcessingStageFactory<Stage.Distinct> {

    @SuppressWarnings("unchecked")
    @Override
    public <I, O> ProcessingStage<I, O> create(Engine engine, Stage.Distinct stage) {
        Objects.requireNonNull(stage);
        return source -> (Multi<O>) source.select().distinct();
    }
}
