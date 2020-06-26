package io.smallrye.mutiny.streams.stages;

import java.util.Objects;

import org.eclipse.microprofile.reactive.streams.operators.spi.Stage;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.streams.Engine;
import io.smallrye.mutiny.streams.operators.ProcessingStage;
import io.smallrye.mutiny.streams.operators.ProcessingStageFactory;

/**
 * Implementation of the {@link Stage.OnTerminate} stage.
 *
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class OnTerminateStageFactory implements ProcessingStageFactory<Stage.OnTerminate> {

    @SuppressWarnings("unchecked")
    @Override
    public <I, O> ProcessingStage<I, O> create(Engine engine, Stage.OnTerminate stage) {
        Runnable runnable = Objects.requireNonNull(stage).getAction();
        Objects.requireNonNull(runnable);
        return source -> (Multi<O>) source.onTermination().invoke(runnable);
    }
}
