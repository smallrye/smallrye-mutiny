package io.smallrye.mutiny.streams.stages;

import org.eclipse.microprofile.reactive.streams.operators.spi.Stage;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.streams.Engine;
import io.smallrye.mutiny.streams.operators.ProcessingStage;
import io.smallrye.mutiny.streams.operators.ProcessingStageFactory;

/**
 * Implementation of the {@link Stage.Limit} stage.
 *
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class LimitStageFactory implements ProcessingStageFactory<Stage.Limit> {

    @SuppressWarnings("unchecked")
    @Override
    public <I, O> ProcessingStage<I, O> create(Engine engine, Stage.Limit stage) {
        long limit = stage.getLimit();
        return source -> (Multi<O>) source.select().first(limit);
    }
}
