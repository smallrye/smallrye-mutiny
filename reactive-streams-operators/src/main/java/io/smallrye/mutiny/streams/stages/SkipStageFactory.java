package io.smallrye.mutiny.streams.stages;

import org.eclipse.microprofile.reactive.streams.operators.spi.Stage;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.streams.Engine;
import io.smallrye.mutiny.streams.operators.ProcessingStage;
import io.smallrye.mutiny.streams.operators.ProcessingStageFactory;

/**
 * Implementation of the {@link Stage.Skip} stage.
 *
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class SkipStageFactory implements ProcessingStageFactory<Stage.Skip> {

    @SuppressWarnings("unchecked")
    @Override
    public <I, O> ProcessingStage<I, O> create(Engine engine, Stage.Skip stage) {
        long skip = stage.getSkip();
        return source -> (Multi<O>) source.skip().first(skip);
    }
}
